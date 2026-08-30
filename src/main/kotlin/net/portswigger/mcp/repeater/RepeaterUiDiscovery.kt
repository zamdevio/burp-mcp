package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import java.awt.Component
import java.awt.Container
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.text.JTextComponent

/**
 * Stateless Repeater UI discovery. Always derives state from the live Swing tree.
 * Does not cache or register tabs.
 *
 * Burp's public Montoya [burp.api.montoya.repeater.Repeater] API cannot enumerate
 * existing tabs; this walks [MontoyaApi.userInterface] → suite frame instead.
 *
 * Heuristics are version-sensitive. When discovery is ambiguous, operations fail
 * with an explicit message rather than mutating the wrong editor.
 */
internal object RepeaterUiDiscovery {

    private val suiteToolTitles = setOf(
        "dashboard",
        "target",
        "proxy",
        "intruder",
        "repeater",
        "sequencer",
        "decoder",
        "comparer",
        "extender",
        "extensions",
        "organizer",
        "logger",
        "inspector",
        "collaborator",
        "ai",
        "mcp",
    )

    sealed class DiscoveryError(val message: String) {
        class RepeaterNotFound : DiscoveryError("<Repeater UI not found>")
        class AmbiguousRepeater : DiscoveryError("<Unable to uniquely identify Repeater panel>")
        class AmbiguousTabPane : DiscoveryError("<Unable to uniquely identify Repeater tab strip>")
        class TabNotFound(tabId: String) : DiscoveryError("<Repeater tab not found: $tabId>")
        class InvalidTabId(tabId: String) : DiscoveryError("<Invalid Repeater tab id: $tabId>")
        class AmbiguousRequestEditor : DiscoveryError("<Unable to uniquely identify Repeater request editor>")
        class AmbiguousResponseEditor : DiscoveryError("<Unable to uniquely identify Repeater response editor>")
        class RequestEditorMissing : DiscoveryError("<Repeater request editor not found>")
        class ResponseEditorMissing : DiscoveryError("<No response available>")
        class NotEditable : DiscoveryError("<Current editor is not editable>")
        class NotInRepeater : DiscoveryError("<No active Repeater tab>")
    }

    sealed class Outcome<out T> {
        data class Ok<T>(val value: T) : Outcome<T>()
        data class Err(val error: DiscoveryError) : Outcome<Nothing>()
    }

    data class TabEditors(
        val request: JTextComponent?,
        val response: JTextComponent?,
        val requestAmbiguous: Boolean,
        val responseAmbiguous: Boolean,
    )

    data class DiscoveredRepeater(
        val suiteTabbedPane: JTabbedPane,
        val suiteRepeaterIndex: Int,
        val repeaterRoot: Component,
        val tabStrip: JTabbedPane,
    )

    fun listTabs(api: MontoyaApi): Outcome<List<RepeaterTabInfo>> = runOnEdt {
        when (val discovered = discoverRepeater(api)) {
            is Outcome.Err -> discovered
            is Outcome.Ok -> Outcome.Ok(buildTabInfos(discovered.value))
        }
    }

    fun getActiveTab(api: MontoyaApi): Outcome<RepeaterTabInfo> = runOnEdt {
        when (val discovered = discoverRepeater(api)) {
            is Outcome.Err -> when (discovered.error) {
                is DiscoveryError.RepeaterNotFound -> Outcome.Err(DiscoveryError.NotInRepeater())
                else -> discovered
            }
            is Outcome.Ok -> {
                val strip = discovered.value.tabStrip
                if (!isRepeaterSuiteTabSelected(discovered.value) || strip.tabCount == 0) {
                    Outcome.Err(DiscoveryError.NotInRepeater())
                } else {
                    val index = strip.selectedIndex
                    if (index < 0) {
                        Outcome.Err(DiscoveryError.NotInRepeater())
                    } else {
                        Outcome.Ok(buildTabInfo(discovered.value, index))
                    }
                }
            }
        }
    }

    fun getTab(api: MontoyaApi, tabId: String): Outcome<RepeaterTabDetail> = runOnEdt {
        when (val resolved = resolveTab(api, tabId)) {
            is Outcome.Err -> resolved
            is Outcome.Ok -> {
                val (discovered, index) = resolved.value
                val strip = discovered.tabStrip
                val editors = findEditorsForTab(discovered, index)
                if (editors.requestAmbiguous) {
                    return@runOnEdt Outcome.Err(DiscoveryError.AmbiguousRequestEditor())
                }
                if (editors.responseAmbiguous) {
                    return@runOnEdt Outcome.Err(DiscoveryError.AmbiguousResponseEditor())
                }
                val requestText = editors.request?.text
                val responseText = editors.response?.text
                val responseAvailable = isResponseAvailable(responseText)
                Outcome.Ok(
                    RepeaterTabDetail(
                        id = RepeaterTabId.fromIndex(index),
                        name = tabTitle(strip, index),
                        index = index,
                        selected = strip.selectedIndex == index,
                        request = requestText,
                        response = if (responseAvailable) responseText else null,
                        requestAvailable = editors.request != null,
                        responseAvailable = responseAvailable,
                    )
                )
            }
        }
    }

    fun getTabRequest(api: MontoyaApi, tabId: String): Outcome<String> = runOnEdt {
        when (val editors = resolveEditors(api, tabId)) {
            is Outcome.Err -> editors
            is Outcome.Ok -> {
                val e = editors.value
                when {
                    e.requestAmbiguous -> Outcome.Err(DiscoveryError.AmbiguousRequestEditor())
                    e.request == null -> Outcome.Err(DiscoveryError.RequestEditorMissing())
                    else -> Outcome.Ok(e.request.text)
                }
            }
        }
    }

    fun getTabResponse(api: MontoyaApi, tabId: String): Outcome<String> = runOnEdt {
        when (val editors = resolveEditors(api, tabId)) {
            is Outcome.Err -> editors
            is Outcome.Ok -> {
                val e = editors.value
                when {
                    e.responseAmbiguous -> Outcome.Err(DiscoveryError.AmbiguousResponseEditor())
                    !isResponseAvailable(e.response?.text) -> Outcome.Err(DiscoveryError.ResponseEditorMissing())
                    else -> Outcome.Ok(e.response!!.text)
                }
            }
        }
    }

    fun setTabRequest(api: MontoyaApi, tabId: String, request: String): Outcome<String> = runOnEdt {
        when (val editors = resolveEditors(api, tabId)) {
            is Outcome.Err -> editors
            is Outcome.Ok -> {
                val e = editors.value
                when {
                    e.requestAmbiguous -> Outcome.Err(DiscoveryError.AmbiguousRequestEditor())
                    e.request == null -> Outcome.Err(DiscoveryError.RequestEditorMissing())
                    !e.request.isEditable -> Outcome.Err(DiscoveryError.NotEditable())
                    else -> {
                        e.request.text = request
                        Outcome.Ok("Repeater request has been set for $tabId")
                    }
                }
            }
        }
    }

    fun selectTab(api: MontoyaApi, tabId: String): Outcome<String> = runOnEdt {
        when (val resolved = resolveTab(api, tabId)) {
            is Outcome.Err -> resolved
            is Outcome.Ok -> {
                val (discovered, index) = resolved.value
                discovered.suiteTabbedPane.selectedIndex = discovered.suiteRepeaterIndex
                discovered.tabStrip.selectedIndex = index
                Outcome.Ok("Selected Repeater tab $tabId")
            }
        }
    }

    fun setTabTitle(api: MontoyaApi, tabId: String, title: String): Outcome<String> = runOnEdt {
        when (val resolved = resolveTab(api, tabId)) {
            is Outcome.Err -> resolved
            is Outcome.Ok -> {
                val (discovered, index) = resolved.value
                val trimmed = title.trim()
                if (trimmed.isEmpty()) {
                    return@runOnEdt Outcome.Err(DiscoveryError.InvalidTabId("empty title"))
                }
                discovered.tabStrip.setTitleAt(index, trimmed)
                Outcome.Ok("Repeater tab title set to \"$trimmed\"")
            }
        }
    }

    // --- discovery primitives (package-visible for tests) ---

    fun discoverRepeater(api: MontoyaApi): Outcome<DiscoveredRepeater> {
        val frame = api.userInterface().swingUtils().suiteFrame()
        return discoverRepeaterFromRoot(frame)
    }

    fun discoverRepeaterFromRoot(root: Component): Outcome<DiscoveredRepeater> {
        val suitePanes = findSuiteRepeaterCandidates(root)
        when {
            suitePanes.isEmpty() -> return Outcome.Err(DiscoveryError.RepeaterNotFound())
            suitePanes.size > 1 -> return Outcome.Err(DiscoveryError.AmbiguousRepeater())
        }
        val (suitePane, repeaterIndex) = suitePanes.single()
        val repeaterRoot = tabContentAt(suitePane, repeaterIndex)
            ?: return Outcome.Err(DiscoveryError.RepeaterNotFound())

        val tabStrips = findRepeaterTabStrips(repeaterRoot)
        when {
            tabStrips.isEmpty() -> return Outcome.Err(DiscoveryError.AmbiguousTabPane())
            tabStrips.size > 1 -> return Outcome.Err(DiscoveryError.AmbiguousTabPane())
        }

        return Outcome.Ok(
            DiscoveredRepeater(
                suiteTabbedPane = suitePane,
                suiteRepeaterIndex = repeaterIndex,
                repeaterRoot = repeaterRoot,
                tabStrip = tabStrips.single(),
            )
        )
    }

    fun findEditors(tabContent: Component?): TabEditors = findEditorsInRoot(tabContent)

    /**
     * Burp often uses one shared request/response editor pair for all Repeater tabs.
     * Select the tab first, then search tab content and the wider Repeater panel.
     */
    fun findEditorsForTab(discovered: DiscoveredRepeater, index: Int): TabEditors =
        withTabSelected(discovered, index) {
            val tabContent = tabContentAt(discovered.tabStrip, index)
            val inTab = findEditorsInRoot(tabContent)
            if (inTab.request != null || inTab.response != null) {
                inTab
            } else {
                findEditorsInRoot(discovered.repeaterRoot)
            }
        }

    fun findEditorsInRoot(root: Component?): TabEditors {
        if (root == null) {
            return TabEditors(null, null, requestAmbiguous = false, responseAmbiguous = false)
        }
        val components = collectMessageTextComponents(root)
        val editable = components.filter { it.isEditable }
        val readOnly = components.filter { !it.isEditable }

        val (request, requestAmbiguous) = pickRequestEditor(editable)
        val (response, responseAmbiguous) = pickResponseEditor(readOnly)

        return TabEditors(
            request = request,
            response = response,
            requestAmbiguous = requestAmbiguous,
            responseAmbiguous = responseAmbiguous,
        )
    }

    private fun pickRequestEditor(editable: List<JTextComponent>): Pair<JTextComponent?, Boolean> {
        if (editable.isEmpty()) return null to false
        if (editable.size == 1) return editable.single() to false

        val httpLike = editable.filter { looksLikeHttpRequest(it.text) }
        if (httpLike.size == 1) return httpLike.single() to false

        val byLength = editable.maxByOrNull { it.text.length } ?: return null to true
        val maxLen = byLength.text.length
        val tiedAtMax = editable.count { it.text.length == maxLen }
        if (httpLike.isEmpty() && tiedAtMax > 1) return null to true
        return byLength to (editable.size > 1 && httpLike.size != 1)
    }

    private fun pickResponseEditor(readOnly: List<JTextComponent>): Pair<JTextComponent?, Boolean> {
        if (readOnly.isEmpty()) return null to false
        if (readOnly.size == 1) return readOnly.single() to false

        val httpLike = readOnly.filter { looksLikeHttpResponse(it.text) }
        if (httpLike.size == 1) return httpLike.single() to false

        val byLength = readOnly.maxByOrNull { it.text.length } ?: return null to true
        val maxLen = byLength.text.length
        val tiedAtMax = readOnly.count { it.text.length == maxLen }
        if (httpLike.isEmpty() && tiedAtMax > 1) return null to true
        return byLength to (readOnly.size > 1 && httpLike.size != 1)
    }

    private fun looksLikeHttpRequest(text: String): Boolean {
        val line = text.lineSequence().firstOrNull()?.trim()?.uppercase() ?: return false
        if (line.startsWith("HTTP/")) return false
        return HTTP_METHODS.any { line.startsWith("$it ") }
    }

    private fun looksLikeHttpResponse(text: String): Boolean =
        text.trimStart().uppercase().startsWith("HTTP/")

    private val HTTP_METHODS = listOf(
        "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT",
    )

    fun isResponseAvailable(text: String?): Boolean {
        if (text == null) return false
        if (text.isBlank()) return false
        // Distinguish "never sent" placeholders from an empty-but-real HTTP response.
        val trimmed = text.trimStart()
        return trimmed.startsWith("HTTP/", ignoreCase = true)
    }

    fun buildTabInfos(discovered: DiscoveredRepeater): List<RepeaterTabInfo> {
        val strip = discovered.tabStrip
        return (0 until strip.tabCount)
            .filterNot { isChromeTab(strip, it) }
            .map { buildTabInfo(discovered, it) }
    }

    fun buildTabInfo(discovered: DiscoveredRepeater, index: Int): RepeaterTabInfo {
        val strip = discovered.tabStrip
        val editors = findEditorsForTab(discovered, index)
        val responseText = editors.response?.text
        return RepeaterTabInfo(
            id = RepeaterTabId.fromIndex(index),
            name = tabTitle(strip, index),
            index = index,
            selected = strip.selectedIndex == index,
            hasRequest = editors.request != null && !editors.requestAmbiguous,
            hasResponse = !editors.responseAmbiguous && isResponseAvailable(responseText),
        )
    }

    private fun isChromeTab(strip: JTabbedPane, index: Int): Boolean {
        val title = strip.getTitleAt(index)?.trim() ?: return true
        if (title == "+" || title.equals("new tab", ignoreCase = true)) return true
        return tabContentAt(strip, index) == null && title.isEmpty()
    }

    private fun <T> withTabSelected(discovered: DiscoveredRepeater, index: Int, block: () -> T): T {
        val strip = discovered.tabStrip
        val suite = discovered.suiteTabbedPane
        val prevStrip = strip.selectedIndex
        val prevSuite = suite.selectedIndex
        suite.selectedIndex = discovered.suiteRepeaterIndex
        strip.selectedIndex = index
        return try {
            block()
        } finally {
            strip.selectedIndex = prevStrip
            if (prevSuite >= 0) {
                suite.selectedIndex = prevSuite
            }
        }
    }

    /** Burp tab strips may return null for chrome slots (e.g. the "+" tab). */
    private fun tabContentAt(pane: JTabbedPane, index: Int): Component? =
        pane.getComponentAt(index)

    private fun resolveTab(api: MontoyaApi, tabId: String): Outcome<Pair<DiscoveredRepeater, Int>> {
        val index = RepeaterTabId.parseIndex(tabId)
            ?: return Outcome.Err(DiscoveryError.InvalidTabId(tabId))
        return when (val discovered = discoverRepeater(api)) {
            is Outcome.Err -> discovered
            is Outcome.Ok -> {
                val strip = discovered.value.tabStrip
                if (index >= strip.tabCount) {
                    Outcome.Err(DiscoveryError.TabNotFound(tabId))
                } else {
                    Outcome.Ok(discovered.value to index)
                }
            }
        }
    }

    private fun resolveEditors(api: MontoyaApi, tabId: String): Outcome<TabEditors> =
        when (val resolved = resolveTab(api, tabId)) {
            is Outcome.Err -> resolved
            is Outcome.Ok -> {
                val (discovered, index) = resolved.value
                Outcome.Ok(findEditorsForTab(discovered, index))
            }
        }

    private fun isRepeaterSuiteTabSelected(discovered: DiscoveredRepeater): Boolean =
        discovered.suiteTabbedPane.selectedIndex == discovered.suiteRepeaterIndex

    private fun tabTitle(tabStrip: JTabbedPane, index: Int): String? {
        val title = tabStrip.getTitleAt(index)
        return title?.takeIf { it.isNotBlank() }
    }

    private fun findSuiteRepeaterCandidates(root: Component): List<Pair<JTabbedPane, Int>> {
        val results = mutableListOf<Pair<JTabbedPane, Int>>()
        for (pane in collectTabbedPanes(root)) {
            for (i in 0 until pane.tabCount) {
                val title = pane.getTitleAt(i)?.trim() ?: continue
                if (title.equals("Repeater", ignoreCase = true)) {
                    results += pane to i
                }
            }
        }
        return results
    }

    /**
     * Candidate request-tab strips live under the Repeater suite panel and are not
     * themselves the suite tool strip (titles like Proxy/Intruder/…). Prefer panes
     * that look like message tabs; require a unique match.
     */
    private fun findRepeaterTabStrips(repeaterRoot: Component): List<JTabbedPane> {
        val panes = collectTabbedPanes(repeaterRoot)
        val candidates = panes.filter { pane ->
            if (pane.tabCount == 0) return@filter false
            val titles = (0 until pane.tabCount).mapNotNull { pane.getTitleAt(it)?.trim()?.lowercase() }
            // Suite-like strips (many tool names) are not the request tab strip.
            val suiteLike = titles.count { it in suiteToolTitles } >= 2
            if (suiteLike) return@filter false
            // Prefer panes that contain at least one text editor (message tabs).
            (0 until pane.tabCount).any { idx ->
                tabContentAt(pane, idx)?.let { root ->
                    collectMessageTextComponents(root).isNotEmpty()
                } == true
            }
        }
        if (candidates.size == 1) return candidates

        // Fallback: single non-suite-like pane even without editors yet (empty new tab).
        val nonSuite = panes.filter { pane ->
            if (pane.tabCount == 0) return@filter true
            val titles = (0 until pane.tabCount).mapNotNull { pane.getTitleAt(it)?.trim()?.lowercase() }
            titles.count { it in suiteToolTitles } < 2
        }
        return if (nonSuite.size == 1) nonSuite else candidates
    }

    private fun collectTabbedPanes(root: Component): List<JTabbedPane> {
        val out = mutableListOf<JTabbedPane>()
        walk(root) { if (it is JTabbedPane) out += it }
        return out
    }

    private fun collectMessageTextComponents(root: Component): List<JTextComponent> {
        val out = mutableListOf<JTextComponent>()
        walk(root) { component ->
            when (component) {
                is JTextField -> Unit
                is JTextComponent -> out += component
            }
        }
        return out
    }

    private fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is Container) {
            // JTabbedPane: walk all tab contents, not only the selected one.
            if (root is JTabbedPane) {
                for (i in 0 until root.tabCount) {
                    tabContentAt(root, i)?.let { walk(it, visit) }
                }
                // Also walk tab components / chrome children.
                for (child in root.components) {
                    if ((0 until root.tabCount).none { tabContentAt(root, it) === child }) {
                        walk(child, visit)
                    }
                }
            } else {
                for (child in root.components) {
                    walk(child, visit)
                }
            }
        }
    }
}
