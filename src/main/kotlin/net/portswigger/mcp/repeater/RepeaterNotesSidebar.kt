package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import java.awt.Component
import java.awt.Container
import java.awt.Point
import javax.swing.AbstractButton
import javax.swing.JEditorPane
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JViewport
import javax.swing.text.JTextComponent

/**
 * Burp 2026.x Repeater **Notes** lives in the east inspector rail (rich-text editor).
 * Searches [repeaterRoot] and full [suiteFrame]; prefers editors beside the rich-text toolbar.
 */
internal object RepeaterNotesSidebar {

    private const val SETTLE_MS = 200L
    private const val SETTLE_POLL_MS = 25L

    private val RICH_TEXT_TOOLBAR_TIPS = listOf("bold", "italic", "underline", "strikethrough", "bullet")

    fun ensureNotesPanelOpen(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
    ) {
        if (collectCandidates(repeaterRoot, tabStrip, suiteFrame, null).isNotEmpty()) {
            return
        }
        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            findNotesRailButton(root, tabStrip)?.takeIf { it.isEnabled }?.doClick()
            val deadline = System.currentTimeMillis() + SETTLE_MS
            while (System.currentTimeMillis() < deadline) {
                if (collectCandidates(repeaterRoot, tabStrip, suiteFrame, null).isNotEmpty()) {
                    return
                }
                Thread.sleep(SETTLE_POLL_MS)
            }
        }
    }

    fun locateShowingNotesEditor(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        messageEditors: RepeaterUiDiscovery.TabEditors,
        api: MontoyaApi? = null,
    ): RepeaterNotes.NotesLookup {
        val repeaterRoot = discovered.repeaterRoot
        val tabStrip = discovered.tabStrip
        val suiteFrame = discovered.suiteFrame

        ensureNotesPanelOpen(repeaterRoot, tabStrip, suiteFrame)
        val sidebarNotesRoot = selectEastSidebarNotesTab(repeaterRoot, tabStrip, suiteFrame)

        sidebarNotesRoot?.let { root ->
            findByRichTextToolbar(root, tabStrip, suiteFrame, messageEditors)?.let { handle ->
                return RepeaterNotes.NotesLookup(handle, ambiguous = false)
            }
            RepeaterNotesReflective.findLargeScrollEditor(
                root,
                tabStrip,
                repeaterRoot,
                messageEditors,
                maxScrollWidth = 900,
            )?.let { return RepeaterNotes.NotesLookup(it, ambiguous = false) }
            val narrow = collectCandidates(root, tabStrip, root, messageEditors)
            val picked = RepeaterNotes.pickNotesFromCandidates(narrow)
            if (picked.editor != null || picked.ambiguous) return picked
        }

        findByRichTextToolbar(repeaterRoot, tabStrip, suiteFrame, messageEditors)?.let { handle ->
            return RepeaterNotes.NotesLookup(handle, ambiguous = false)
        }

        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            RepeaterNotesReflective.findLargeScrollEditor(
                root,
                tabStrip,
                repeaterRoot,
                messageEditors,
                maxScrollWidth = 900,
            )?.let { return RepeaterNotes.NotesLookup(it, ambiguous = false) }
        }

        val candidates = collectCandidates(repeaterRoot, tabStrip, suiteFrame, messageEditors)
        val lookup = RepeaterNotes.pickNotesFromCandidates(candidates)
        if (lookup.editor == null && !lookup.ambiguous && api != null) {
            logDiscoveryHint(api, diagnoseCandidates(repeaterRoot, tabStrip, suiteFrame))
        }
        return lookup
    }

    /** Clicks the vertical east-rail **Notes** control when present (Burp 2026.x). */
    fun ensureEastNotesRailSelected(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
    ) {
        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            findNotesRailButton(root, tabStrip)?.takeIf { it.isEnabled }?.doClick()
        }
        Thread.sleep(SETTLE_POLL_MS)
    }

    /** Select east rail **Notes** (Inspector / Notes / Explanations), not the message tab strip. */
    fun selectEastSidebarNotesTab(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
    ): Component? {
        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            for (pane in collectTabbedPanes(root)) {
                if (pane === tabStrip) continue
                val titles = (0 until pane.tabCount).map { pane.getTitleAt(it)?.trim().orEmpty() }
                if (titles.none { it.equals("Inspector", ignoreCase = true) }) continue
                if (titles.none { it.equals("Notes", ignoreCase = true) }) continue
                if (titles.any { it.equals("Request", ignoreCase = true) }) continue
                val notesIndex = titles.indexOfFirst { it.equals("Notes", ignoreCase = true) }
                if (notesIndex < 0) continue
                pane.selectedIndex = notesIndex
                Thread.sleep(SETTLE_POLL_MS)
                return tabContentAt(pane, notesIndex) ?: pane
            }
        }
        return null
    }

    private fun collectTabbedPanes(root: Component): List<JTabbedPane> {
        val out = mutableListOf<JTabbedPane>()
        walk(root) { if (it is JTabbedPane) out += it }
        return out
    }

    private fun tabContentAt(pane: JTabbedPane, index: Int): Component? = pane.getComponentAt(index)

    /** Agent/Burp-output hint when no editor matched. */
    fun diagnoseCandidates(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
    ): String {
        val lines = mutableListOf<String>()
        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            walk(root) { component ->
                when (component) {
                    is JScrollPane -> {
                        if (RepeaterNotes.isUnderComponent(component, tabStrip)) return@walk
                        val view = component.viewport?.view
                        lines += "Scroll show=${component.isShowing} ${component.width}x${component.height} " +
                            "view=${view?.javaClass?.simpleName} viewShow=${view?.isShowing}"
                    }
                    is JTextComponent -> {
                        val c = component
                        if (RepeaterNotes.isUnderComponent(c, tabStrip)) return@walk
                        lines += "${c.javaClass.simpleName} show=${c.isShowing} ${c.width}x${c.height} " +
                            "name=${c.accessibleContext?.accessibleName.orEmpty().take(30)}"
                    }
                }
            }
        }
        return lines.take(10).joinToString(" | ")
    }

    private fun collectCandidates(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
        messageEditors: RepeaterUiDiscovery.TabEditors?,
    ): List<RepeaterNotes.NotesEditorHandle> {
        val seen = mutableSetOf<Component>()
        val handles = mutableListOf<RepeaterNotes.NotesEditorHandle>()
        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            for (handle in findShowingNotesEditors(root, tabStrip, messageEditors, repeaterRoot)) {
                if (seen.add(handle.component)) {
                    handles += handle
                }
            }
        }
        return handles.sortedByDescending { score(it, repeaterRoot, tabStrip) }
    }

    private fun searchRoots(repeaterRoot: Component, suiteFrame: Component): List<Component> =
        if (repeaterRoot === suiteFrame) {
            listOf(repeaterRoot)
        } else {
            listOf(repeaterRoot, suiteFrame)
        }

    private fun findByRichTextToolbar(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
        messageEditors: RepeaterUiDiscovery.TabEditors,
    ): RepeaterNotes.NotesEditorHandle? {
        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            val toolbarHosts = mutableSetOf<Container>()
            walk(root) { component ->
                val button = component as? AbstractButton ?: return@walk
                if (RepeaterNotes.isUnderComponent(button, tabStrip)) return@walk
                val tip = button.toolTipText?.lowercase().orEmpty()
                if (RICH_TEXT_TOOLBAR_TIPS.any { tip.contains(it) }) {
                    var host: Container? = button.parent
                    repeat(3) { host = host?.parent as? Container }
                    host?.let { toolbarHosts += it }
                }
            }
            for (host in toolbarHosts) {
                val pool = findShowingNotesEditors(host, tabStrip, messageEditors, repeaterRoot)
                if (pool.size == 1) return pool.single()
                if (pool.isNotEmpty()) {
                    return pool.maxByOrNull { score(it, repeaterRoot, tabStrip) }
                }
            }
        }
        return null
    }

    private fun findShowingNotesEditors(
        searchRoot: Component,
        tabStrip: JTabbedPane,
        messageEditors: RepeaterUiDiscovery.TabEditors?,
        anchor: Component,
    ): List<RepeaterNotes.NotesEditorHandle> {
        val handles = mutableListOf<RepeaterNotes.NotesEditorHandle>()
        walk(searchRoot) { component ->
            if (component is JScrollPane && !RepeaterNotes.isUnderComponent(component, tabStrip)) {
                val view = component.viewport?.view
                when (view) {
                    is JEditorPane -> {
                        val handle = RepeaterNotes.editorPaneHandle(view)
                        if (isNotesCandidate(handle, tabStrip, messageEditors, anchor)) {
                            handles += handle
                        }
                    }
                    is JTextComponent -> {
                        if (view is JEditorPane) return@walk
                        val handle = RepeaterNotes.textComponentHandle(view)
                        if (isNotesCandidate(handle, tabStrip, messageEditors, anchor)) {
                            handles += handle
                        }
                    }
                }
            }
            when (component) {
                is JEditorPane -> {
                    val handle = RepeaterNotes.editorPaneHandle(component)
                    if (isNotesCandidate(handle, tabStrip, messageEditors, anchor)) {
                        handles += handle
                    }
                }
                is JTextComponent -> {
                    if (component is JEditorPane) return@walk
                    val handle = RepeaterNotes.textComponentHandle(component)
                    if (isNotesCandidate(handle, tabStrip, messageEditors, anchor)) {
                        handles += handle
                    }
                }
            }
        }
        return handles
    }

    private fun isNotesCandidate(
        handle: RepeaterNotes.NotesEditorHandle,
        tabStrip: JTabbedPane,
        messageEditors: RepeaterUiDiscovery.TabEditors?,
        anchor: Component,
    ): Boolean {
        val c = handle.component
        if (!isEffectivelyVisible(c) || c.width < 16 || c.height < 16) return false
        if (RepeaterNotes.isUnderComponent(c, tabStrip)) return false
        if (messageEditors != null) {
            if (c === messageEditors.request || c === messageEditors.response) return false
            val text = handle.readText()
            if (RepeaterNotes.looksLikeHttpRequest(text) || RepeaterNotes.looksLikeHttpResponse(text)) {
                return false
            }
        }
        return true
    }

    private fun isEffectivelyVisible(c: Component): Boolean {
        if (!c.isDisplayable) return false
        var cur: Component? = c
        while (cur != null) {
            if (!cur.isVisible) return false
            cur = cur.parent
        }
        if (c.isShowing) return true
        // JTextComponent in JScrollPane often has isShowing=false while the pane is visible.
        var ancestor: Component? = c.parent
        while (ancestor != null) {
            when (ancestor) {
                is JScrollPane -> if (ancestor.isShowing && ancestor.width > 16 && ancestor.height > 16) {
                    return true
                }
                is JViewport -> {
                    val scroll = ancestor.parent as? JScrollPane
                    if (scroll != null && scroll.isShowing && scroll.width > 16 && scroll.height > 16) {
                        return true
                    }
                }
            }
            ancestor = ancestor.parent
        }
        return false
    }

    private fun score(
        handle: RepeaterNotes.NotesEditorHandle,
        anchor: Component,
        tabStrip: JTabbedPane,
    ): Int {
        val c = handle.component
        var score = c.width * c.height
        if (c is JEditorPane) score += 2_000_000
        val rootPoint = safeLocationOnScreen(anchor)
        val cPoint = safeLocationOnScreen(c)
        if (rootPoint != null && cPoint != null) {
            val midX = rootPoint.x + anchor.width / 2
            if (cPoint.x >= midX) score += 800_000
        }
        if (ancestorHintsNotes(c)) score += 400_000
        if (RepeaterNotes.isUnderComponent(c, tabStrip)) score -= 5_000_000
        return score
    }

    private fun ancestorHintsNotes(component: Component): Boolean {
        var cur: Component? = component.parent
        while (cur != null) {
            val name = cur.javaClass.simpleName + cur.accessibleContext?.accessibleName.orEmpty()
            if (name.contains("note", ignoreCase = true) ||
                name.contains("inspector", ignoreCase = true) ||
                name.contains("sidebar", ignoreCase = true)
            ) {
                return true
            }
            cur = cur.parent
        }
        return false
    }

    private fun findNotesRailButton(searchRoot: Component, tabStrip: JTabbedPane): AbstractButton? {
        val buttons = mutableListOf<AbstractButton>()
        walk(searchRoot) { component ->
            val button = component as? AbstractButton ?: return@walk
            if (RepeaterNotes.isUnderComponent(button, tabStrip)) return@walk
            val hint = listOf(
                button.toolTipText,
                button.text,
                button.accessibleContext?.accessibleName,
                button.accessibleContext?.accessibleDescription,
            ).joinToString(" ").trim()
            if (hint.contains("note", ignoreCase = true)) {
                buttons += button
            }
        }
        if (buttons.isEmpty()) return null
        val rootPoint = safeLocationOnScreen(searchRoot) ?: return buttons.first()
        return buttons.maxByOrNull { button ->
            val p = safeLocationOnScreen(button)
            var s = 0
            if (p != null && p.x > rootPoint.x + searchRoot.width / 2) s += 1000
            if (button.isShowing) s += 500
            s
        }
    }

    private fun logDiscoveryHint(api: MontoyaApi, hint: String) {
        api.logging().logToOutput("MCP Notes discovery: no showing editor. $hint")
    }

    private fun safeLocationOnScreen(c: Component): Point? =
        if (c.isShowing) {
            try {
                c.locationOnScreen
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }

    private fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is Container) {
            if (root is JTabbedPane) {
                for (i in 0 until root.tabCount) {
                    root.getComponentAt(i)?.let { walk(it, visit) }
                }
                for (child in root.components) {
                    if ((0 until root.tabCount).none { root.getComponentAt(it) === child }) {
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
