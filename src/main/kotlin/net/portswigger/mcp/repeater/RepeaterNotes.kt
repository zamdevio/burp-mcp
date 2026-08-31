package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import java.awt.Component
import java.awt.Container
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JTabbedPane
import javax.swing.text.JTextComponent

/**
 * Repeater **Notes** sidebar / panel discovery (Swing). Per-tab notes are not on Montoya.
 * Burp 2026.x: use east **Notes** inspector ([RepeaterNotesSidebar]).
 */
internal object RepeaterNotes {

    data class NotesLookup(
        val editor: NotesEditorHandle?,
        val ambiguous: Boolean,
    )

    interface NotesEditorHandle {
        val component: Component
        fun readText(): String
        fun writeText(text: String)
        fun ensureWritable(): Boolean
        fun isVisibleEnough(): Boolean
    }

    fun readWhileSelected(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        api: MontoyaApi? = null,
    ): RepeaterUiDiscovery.Outcome<String> {
        val lookup = locateNotes(discovered, index, requireVisible = true, api = api)
        return when {
            lookup.ambiguous -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.AmbiguousNotesEditor(),
            )
            lookup.editor == null -> {
                val clip = api?.let { RepeaterNotesClipboard.readPlainText(it, discovered) }
                if (!clip.isNullOrBlank()) {
                    RepeaterUiDiscovery.Outcome.Ok(clip)
                } else {
                    RepeaterUiDiscovery.Outcome.Err(
                        RepeaterUiDiscovery.DiscoveryError.NotesNotFound(notesDiagnostic(discovered)),
                    )
                }
            }
            else -> {
                val swingText = lookup.editor.readText()
                val clipRaw = api?.let { RepeaterNotesClipboard.readPlainText(it, discovered) }
                val clip = clipRaw?.trim()?.takeIf { isPlausibleNotesPlainText(it) }
                val text = when {
                    !clip.isNullOrBlank() && (swingText.isBlank() || looksLikeWrongNotesSource(swingText)) -> clip
                    swingText.isNotBlank() && isPlausibleNotesPlainText(swingText) -> swingText
                    !clip.isNullOrBlank() -> clip
                    else -> swingText
                }
                RepeaterUiDiscovery.Outcome.Ok(text)
            }
        }
    }

    fun writeWhileSelected(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        tabId: String,
        notes: String,
        api: MontoyaApi? = null,
    ): RepeaterUiDiscovery.Outcome<String> {
        val lookup = locateNotes(discovered, index, requireVisible = true, api = api)
        val target = lookup.editor
        return when {
            lookup.ambiguous -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.AmbiguousNotesEditor(),
            )
            target == null -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.NotesNotFound(notesDiagnostic(discovered)),
            )
            !target.ensureWritable() -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.NotesNotEditable(),
            )
            else -> {
                target.writeText(notes)
                RepeaterTabSession.settleEditors(discovered, index)
                var verified = verifyNotesWritten(discovered, index, notes, api)
                if (!verified && api != null) {
                    verified = RepeaterNotesClipboard.writePlainText(api, discovered, notes) &&
                        verifyNotesWritten(discovered, index, notes, api)
                }
                if (!verified) {
                    RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.NotesDidNotUpdate())
                } else {
                    RepeaterUiDiscovery.Outcome.Ok("Repeater notes have been set for $tabId")
                }
            }
        }
    }

    private fun notesDiagnostic(discovered: RepeaterUiDiscovery.DiscoveredRepeater): String? {
        val a = RepeaterNotesSidebar.diagnoseCandidates(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        )
        val b = RepeaterNotesReflective.describeScrollViews(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        )
        return listOf(a, b).filter { it.isNotBlank() }.joinToString(" | ").ifBlank { null }
    }

    private fun verifyNotesWritten(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        expected: String,
        api: MontoyaApi?,
    ): Boolean {
        if (RepeaterNotesScanner.sidebarContainsText(discovered, expected)) {
            return true
        }
        val after = locateNotes(discovered, index, requireVisible = true, api = api)
        val text = after.editor?.readText()
        if (text == expected) return true
        if (api != null) {
            val clip = RepeaterNotesClipboard.readPlainText(api, discovered)?.trim()
            if (clip == expected.trim() && isPlausibleNotesPlainText(clip)) return true
        }
        return false
    }

    /** Package-visible for unit tests. */
    fun findNotesInRoot(
        root: Component?,
        messageEditors: RepeaterUiDiscovery.TabEditors,
        tabStrip: JTabbedPane? = null,
    ): NotesLookup = findNotesInRootInternal(root, messageEditors, tabStrip, requireVisible = false)

    internal fun pickNotesFromCandidates(handles: List<NotesEditorHandle>): NotesLookup =
        pickNotesHandles(handles, requireVisible = true)

    internal fun editorPaneHandle(pane: JEditorPane): NotesEditorHandle =
        object : NotesEditorHandle {
            override val component: Component = pane
            override fun readText(): String = readDocumentText(pane)
            override fun writeText(text: String) {
                writeDocumentText(pane, text)
            }
            override fun ensureWritable(): Boolean {
                pane.isEditable = true
                return pane.isEditable
            }
            override fun isVisibleEnough(): Boolean = pane.isDisplayable && pane.isShowing
        }

    internal fun textComponentHandle(text: JTextComponent): NotesEditorHandle =
        object : NotesEditorHandle {
            override val component: Component = text
            override fun readText(): String = text.text.orEmpty()
            override fun writeText(value: String) {
                text.text = value
            }
            override fun ensureWritable(): Boolean {
                if (text.isEditable) return true
                text.isEditable = true
                return text.isEditable
            }
            override fun isVisibleEnough(): Boolean = text.isDisplayable && text.isShowing
        }

    internal fun isUnderComponent(component: Component, ancestor: Component): Boolean {
        var cur: Component? = component
        while (cur != null) {
            if (cur === ancestor) return true
            cur = cur.parent
        }
        return false
    }

    internal fun looksLikeHttpRequest(text: String): Boolean {
        val line = text.lineSequence().firstOrNull()?.trim()?.uppercase() ?: return false
        if (line.startsWith("HTTP/")) return false
        return HTTP_METHODS.any { line.startsWith("$it ") }
    }

    internal fun looksLikeHttpResponse(text: String): Boolean =
        text.trimStart().uppercase().startsWith("HTTP/")

    private fun looksLikeWrongNotesSource(text: String): Boolean {
        if (text.length > 4000) return true
        if (looksLikeHttpRequest(text) || looksLikeHttpResponse(text)) return true
        val lower = text.lowercase()
        if (lower.contains("portswigger ltd") || lower.contains("started mcp server")) return true
        if (lower.contains("message inspector") && lower.contains("analyse")) return true
        if (lower.contains("visible notes test")) return true
        return false
    }

    private fun isPlausibleNotesPlainText(text: String): Boolean {
        if (text.isBlank()) return false
        if (looksLikeHttpRequest(text) || looksLikeHttpResponse(text)) return false
        if (text.length > 32_000) return false
        return true
    }

    private fun locateNotes(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        requireVisible: Boolean,
        api: MontoyaApi? = null,
    ): NotesLookup {
        val messageEditors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, index)
        if (requireVisible) {
            RepeaterNotesSidebar.ensureNotesPanelOpen(
                discovered.repeaterRoot,
                discovered.tabStrip,
                discovered.suiteFrame,
            )
            RepeaterNotesScanner.locateInSidebar(discovered, messageEditors).let { sidebar ->
                if (sidebar.editor != null || sidebar.ambiguous) return sidebar
            }
            return RepeaterNotesSidebar.locateShowingNotesEditor(discovered, messageEditors, api)
        }
        val tabContent = tabContentAt(discovered.tabStrip, index)
        findSidebarNotesLegacy(discovered.repeaterRoot, discovered.tabStrip, messageEditors, requireVisible)
            ?.let { return it }
        val inTab = findNotesInRootInternal(tabContent, messageEditors, discovered.tabStrip, requireVisible)
        if (inTab.editor != null || inTab.ambiguous) return inTab
        return findNotesInRootInternal(discovered.repeaterRoot, messageEditors, discovered.tabStrip, requireVisible)
    }

    private fun findSidebarNotesLegacy(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        messageEditors: RepeaterUiDiscovery.TabEditors,
        requireVisible: Boolean,
    ): NotesLookup? {
        val candidates = collectNotesHandles(repeaterRoot)
            .filter { !isUnderComponent(it.component, tabStrip) }
            .filter { it.component !== messageEditors.request && it.component !== messageEditors.response }
            .filter { !looksLikeHttpRequest(it.readText()) && !looksLikeHttpResponse(it.readText()) }
        val picked = pickNotesHandles(candidates, requireVisible)
        return if (picked.editor != null || picked.ambiguous) picked else null
    }

    private fun findNotesInRootInternal(
        root: Component?,
        messageEditors: RepeaterUiDiscovery.TabEditors,
        tabStrip: JTabbedPane?,
        requireVisible: Boolean,
    ): NotesLookup {
        if (root == null) return NotesLookup(null, ambiguous = false)
        findByNotesSubtab(root, tabStrip)?.let { handle ->
            if (!requireVisible || handle.isVisibleEnough()) {
                return NotesLookup(handle, ambiguous = false)
            }
        }
        findByNotesLabel(root)?.let { handle ->
            if (!requireVisible || handle.isVisibleEnough()) {
                return NotesLookup(handle, ambiguous = false)
            }
        }
        findByAccessibleNotesName(root)?.let { handle ->
            if (!requireVisible || handle.isVisibleEnough()) {
                return NotesLookup(handle, ambiguous = false)
            }
        }
        val handles = collectNotesHandles(root)
            .filter { h ->
                val c = h.component
                c !== messageEditors.request && c !== messageEditors.response &&
                    !looksLikeHttpRequest(h.readText()) && !looksLikeHttpResponse(h.readText())
            }
        return pickNotesHandles(handles, requireVisible)
    }

    private fun pickNotesHandles(
        handles: List<NotesEditorHandle>,
        requireVisible: Boolean,
    ): NotesLookup {
        val pool = if (requireVisible) {
            val visible = handles.filter { it.isVisibleEnough() }
            if (visible.isEmpty()) return NotesLookup(null, ambiguous = false)
            visible
        } else {
            handles
        }
        when (pool.size) {
            0 -> return NotesLookup(null, ambiguous = false)
            1 -> return NotesLookup(pool.single(), ambiguous = false)
            else -> {
                val panes = pool.filter { it.component is JEditorPane }
                if (panes.size == 1) return NotesLookup(panes.single(), ambiguous = false)
                val best = pool.maxByOrNull { it.component.width * it.component.height }
                val tied = pool.count {
                    it.component.width * it.component.height ==
                        best!!.component.width * best.component.height
                }
                return if (tied == 1) NotesLookup(best, ambiguous = false) else NotesLookup(null, ambiguous = true)
            }
        }
    }

    private fun findByNotesSubtab(root: Component, tabStrip: JTabbedPane?): NotesEditorHandle? {
        for (pane in collectTabbedPanes(root)) {
            if (tabStrip != null && pane === tabStrip) continue
            for (i in 0 until pane.tabCount) {
                val title = pane.getTitleAt(i)?.trim().orEmpty()
                if (!title.equals("Notes", ignoreCase = true)) continue
                val content = tabContentAt(pane, i) ?: continue
                val handles = collectNotesHandles(content)
                if (handles.size == 1) return handles.single()
            }
        }
        return null
    }

    private fun findByNotesLabel(root: Component): NotesEditorHandle? {
        var found: NotesEditorHandle? = null
        walk(root) { component ->
            if (found != null) return@walk
            val labelText = (component as? JLabel)?.text?.trim().orEmpty()
            if (labelText.isEmpty() || !labelText.contains("note", ignoreCase = true)) return@walk
            val parent = component.parent ?: return@walk
            val handles = collectNotesHandles(parent)
                .filter { !looksLikeHttpRequest(it.readText()) && !looksLikeHttpResponse(it.readText()) }
            if (handles.size == 1) found = handles.single()
        }
        return found
    }

    private fun findByAccessibleNotesName(root: Component): NotesEditorHandle? {
        val matches = collectNotesHandles(root).filter {
            val name = it.component.accessibleContext?.accessibleName?.trim().orEmpty()
            name.contains("note", ignoreCase = true)
        }
        return when (matches.size) {
            1 -> matches.single()
            else -> null
        }
    }

    private fun collectNotesHandles(root: Component): List<NotesEditorHandle> {
        val out = mutableListOf<NotesEditorHandle>()
        walk(root) { component ->
            when (component) {
                is JEditorPane -> out += editorPaneHandle(component)
                is JTextComponent -> out += textComponentHandle(component)
            }
        }
        return out
    }

    private fun readDocumentText(pane: JEditorPane): String {
        val doc = pane.document
        return try {
            doc.getText(0, doc.length).trim()
        } catch (_: Exception) {
            pane.text.orEmpty().trim()
        }
    }

    private fun writeDocumentText(pane: JEditorPane, text: String) {
        pane.isEditable = true
        val doc = pane.document
        try {
            doc.remove(0, doc.length)
            doc.insertString(0, text, null)
        } catch (_: Exception) {
            pane.text = text
        }
    }

    private val HTTP_METHODS = listOf(
        "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE", "CONNECT",
    )

    private fun tabContentAt(pane: JTabbedPane, index: Int): Component? = pane.getComponentAt(index)

    private fun collectTabbedPanes(root: Component): List<JTabbedPane> {
        val out = mutableListOf<JTabbedPane>()
        walk(root) { if (it is JTabbedPane) out += it }
        return out
    }

    private fun walk(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is Container) {
            if (root is JTabbedPane) {
                for (i in 0 until root.tabCount) {
                    tabContentAt(root, i)?.let { walk(it, visit) }
                }
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
