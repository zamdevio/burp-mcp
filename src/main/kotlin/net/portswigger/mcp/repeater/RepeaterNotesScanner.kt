package net.portswigger.mcp.repeater

import java.awt.Component
import java.awt.Container
import javax.swing.JScrollPane
import javax.swing.text.JTextComponent
import kotlinx.serialization.Serializable

/**
 * Full tree scan for text-bearing components (Notes UI spike / needle calibration).
 */
internal object RepeaterNotesScanner {

    @Serializable
    data class NotesUiScanResult(
        val needle: String?,
        val messageTabId: String?,
        val sidebarNotesPanelFound: Boolean,
        val candidates: List<NotesUiCandidate>,
        val clipboardPreview: String? = null,
        val clipboardMatchesNeedle: Boolean = false,
    )

    @Serializable
    data class NotesUiCandidate(
        val className: String,
        val width: Int,
        val height: Int,
        val showing: Boolean,
        val accessibleName: String,
        val textPreview: String,
        val matchesNeedle: Boolean,
        val inSidebarNotesPanel: Boolean,
        val scrollSize: String?,
    )

    fun scanRepeaterNotesUi(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        tabIndex: Int,
        needle: String?,
    ): NotesUiScanResult {
        val sidebarRoot = RepeaterNotesSidebar.selectEastSidebarNotesTab(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        )
        val normalizedNeedle = needle?.trim()?.takeIf { it.isNotEmpty() }
        val candidates = mutableListOf<NotesUiCandidate>()
        for (root in listOf(discovered.suiteFrame, discovered.repeaterRoot).distinct()) {
            collectCandidates(
                root = root,
                tabStrip = discovered.tabStrip,
                sidebarRoot = sidebarRoot,
                needle = normalizedNeedle,
                out = candidates,
            )
        }
        val deduped = candidates
            .distinctBy { "${it.className}:${it.textPreview.take(40)}:${it.width}x${it.height}" }
            .sortedWith(
                compareByDescending<NotesUiCandidate> { it.matchesNeedle }
                    .thenByDescending { it.inSidebarNotesPanel }
                    .thenByDescending { it.textPreview.length },
            )
            .take(40)
        RepeaterNotesSidebar.ensureEastNotesRailSelected(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        )
        val clipPreview = RepeaterNotesClipboard.readClipboardPreview(discovered)
        return NotesUiScanResult(
            needle = normalizedNeedle,
            messageTabId = RepeaterTabId.fromIndex(tabIndex),
            sidebarNotesPanelFound = sidebarRoot != null,
            candidates = deduped,
            clipboardPreview = clipPreview?.take(200),
            clipboardMatchesNeedle = normalizedNeedle != null &&
                clipPreview?.contains(normalizedNeedle, ignoreCase = true) == true,
        )
    }

    fun sidebarContainsText(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        expected: String,
    ): Boolean {
        val sidebarRoot = RepeaterNotesSidebar.selectEastSidebarNotesTab(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        ) ?: return false
        val hits = mutableListOf<NotesUiCandidate>()
        collectCandidates(
            root = sidebarRoot,
            tabStrip = discovered.tabStrip,
            sidebarRoot = sidebarRoot,
            needle = expected,
            out = hits,
        )
        return hits.any { it.matchesNeedle }
    }

    fun locateInSidebar(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        messageEditors: RepeaterUiDiscovery.TabEditors,
    ): RepeaterNotes.NotesLookup {
        val sidebarRoot = RepeaterNotesSidebar.selectEastSidebarNotesTab(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        ) ?: return RepeaterNotes.NotesLookup(null, false)
        val handles = mutableListOf<RepeaterNotes.NotesEditorHandle>()
        walk(sidebarRoot) { component ->
            if (component !is Component) return@walk
            if (RepeaterNotes.isUnderComponent(component, discovered.tabStrip)) return@walk
            val handle = RepeaterNotesReflective.reflectiveHandle(component)
                ?: (component as? JTextComponent)?.let { RepeaterNotes.textComponentHandle(it) }
                ?: return@walk
            if (messageEditors.request != null && component === messageEditors.request) return@walk
            if (messageEditors.response != null && component === messageEditors.response) return@walk
            val text = runCatching { handle.readText() }.getOrDefault("")
            if (RepeaterNotes.looksLikeHttpRequest(text) || RepeaterNotes.looksLikeHttpResponse(text)) {
                return@walk
            }
            handles += handle
        }
        return RepeaterNotes.pickNotesFromCandidates(handles)
    }

    private fun collectCandidates(
        root: Component,
        tabStrip: javax.swing.JTabbedPane,
        sidebarRoot: Component?,
        needle: String?,
        out: MutableList<NotesUiCandidate>,
    ) {
        walk(root) { component ->
            if (RepeaterNotes.isUnderComponent(component, tabStrip)) return@walk
            val handle = RepeaterNotesReflective.reflectiveHandle(component)
                ?: (component as? JTextComponent)?.let { RepeaterNotes.textComponentHandle(it) }
            val text = handle?.let { runCatching { it.readText() }.getOrNull() }
                ?: probeAccessibleText(component)
            if (text.isNullOrBlank() && handle == null) return@walk
            val preview = text.orEmpty().replace("\r", " ").replace("\n", " ").take(120)
            if (preview.isBlank() && handle == null) return@walk
            val scroll = findAncestorScroll(component)
            out += NotesUiCandidate(
                className = component.javaClass.name,
                width = component.width,
                height = component.height,
                showing = component.isShowing,
                accessibleName = component.accessibleContext?.accessibleName.orEmpty().take(60),
                textPreview = preview,
                matchesNeedle = needle != null && preview.contains(needle, ignoreCase = true),
                inSidebarNotesPanel = sidebarRoot != null && isUnder(component, sidebarRoot),
                scrollSize = scroll?.let { "${it.width}x${it.height}" },
            )
        }
    }

    private fun probeAccessibleText(component: Component): String? {
        val ctx = component.accessibleContext ?: return null
        val at = ctx.accessibleText ?: return null
        val len = at.charCount
        if (len <= 0) return null
        return runCatching { at.getAtIndex(javax.accessibility.AccessibleText.CHARACTER, 0)?.toString() }.getOrNull()
            ?: runCatching {
                buildString {
                    val end = minOf(len, 200)
                    for (i in 0 until end) append(at.getAtIndex(javax.accessibility.AccessibleText.CHARACTER, i))
                }
            }.getOrNull()
    }

    private fun findAncestorScroll(component: Component): JScrollPane? {
        var cur: Component? = component.parent
        while (cur != null) {
            if (cur is JScrollPane) return cur
            cur = cur.parent
        }
        return null
    }

    private fun isUnder(component: Component, ancestor: Component): Boolean {
        var cur: Component? = component
        while (cur != null) {
            if (cur === ancestor) return true
            cur = cur.parent
        }
        return false
    }

    private fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is Container) {
            for (child in root.components) {
                walk(child, visit)
            }
        }
    }
}
