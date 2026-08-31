package net.portswigger.mcp.repeater

import java.awt.Component
import java.awt.Container
import javax.swing.JScrollPane
import javax.swing.text.JTextComponent
import kotlinx.serialization.Serializable

/**
 * Debug-only Swing scan for Notes UI calibration ([scan_repeater_notes_ui]).
 * Production notes I/O uses [RepeaterTabNotes] / [RepeaterTabAnnotations].
 */
internal object RepeaterNotesScanner {

    @Serializable
    data class NotesUiScanResult(
        val needle: String?,
        val messageTabId: String?,
        val sidebarNotesPanelFound: Boolean,
        val candidates: List<NotesUiCandidate>,
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
        return NotesUiScanResult(
            needle = normalizedNeedle,
            messageTabId = RepeaterTabId.fromIndex(tabIndex),
            sidebarNotesPanelFound = sidebarRoot != null,
            candidates = deduped,
        )
    }

    private fun collectCandidates(
        root: Component,
        tabStrip: javax.swing.JTabbedPane,
        sidebarRoot: Component?,
        needle: String?,
        out: MutableList<NotesUiCandidate>,
    ) {
        RepeaterSwingTree.walk(root) { component ->
            if (RepeaterSwingTree.isUnderComponent(component, tabStrip)) return@walk
            val handle = RepeaterNotesReflective.reflectiveHandle(component)
                ?: (component as? JTextComponent)?.let { RepeaterNotesReflective.textComponentProbe(it) }
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
        return runCatching {
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
}
