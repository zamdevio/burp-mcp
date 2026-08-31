package net.portswigger.mcp.repeater

import java.awt.Component
import java.awt.Container
import javax.swing.JLabel
import javax.swing.JTabbedPane
import javax.swing.text.JTextComponent

/**
 * Repeater **Notes** panel discovery (Swing). Per-tab notes are not exposed on Montoya.
 * Uses layered heuristics; fails safe when ambiguous.
 */
internal object RepeaterNotes {

    data class NotesLookup(
        val editor: JTextComponent?,
        val ambiguous: Boolean,
    )

    fun readWhileSelected(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
    ): RepeaterUiDiscovery.Outcome<String> {
        val lookup = locateNotes(discovered, index)
        return when {
            lookup.ambiguous -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.AmbiguousNotesEditor(),
            )
            lookup.editor == null -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.NotesNotFound(),
            )
            else -> RepeaterUiDiscovery.Outcome.Ok(lookup.editor.text.orEmpty())
        }
    }

    fun writeWhileSelected(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        tabId: String,
        notes: String,
    ): RepeaterUiDiscovery.Outcome<String> {
        val lookup = locateNotes(discovered, index)
        val target = lookup.editor
        return when {
            lookup.ambiguous -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.AmbiguousNotesEditor(),
            )
            target == null -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.NotesNotFound(),
            )
            !ensureWritable(target) -> RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.NotesNotEditable(),
            )
            else -> {
                target.text = notes
                RepeaterTabSession.settleEditors(discovered, index)
                val after = locateNotes(discovered, index).editor?.text
                if (after != notes) {
                    RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.NotesDidNotUpdate())
                } else {
                    RepeaterUiDiscovery.Outcome.Ok("Repeater notes have been set for $tabId")
                }
            }
        }
    }

    /** Package-visible for unit tests. */
    fun findNotesInRoot(
        root: Component?,
        messageEditors: RepeaterUiDiscovery.TabEditors,
    ): NotesLookup {
        if (root == null) return NotesLookup(null, ambiguous = false)
        findByNotesSubtab(root)?.let { return NotesLookup(it, ambiguous = false) }
        findByNotesLabel(root)?.let { return NotesLookup(it, ambiguous = false) }
        findByAccessibleNotesName(root)?.let { return NotesLookup(it, ambiguous = false) }
        return findResidualNotesEditor(root, messageEditors)
    }

    private fun locateNotes(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
    ): NotesLookup {
        val messageEditors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, index)
        val tabContent = tabContentAt(discovered.tabStrip, index)
        val inTab = findNotesInRoot(tabContent, messageEditors)
        if (inTab.editor != null || inTab.ambiguous) return inTab
        return findNotesInRoot(discovered.repeaterRoot, messageEditors)
    }

    private fun findByNotesSubtab(root: Component): JTextComponent? {
        for (pane in collectTabbedPanes(root)) {
            for (i in 0 until pane.tabCount) {
                val title = pane.getTitleAt(i)?.trim().orEmpty()
                if (!title.equals("Notes", ignoreCase = true)) continue
                val content = tabContentAt(pane, i) ?: continue
                val texts = collectMessageTextComponents(content)
                if (texts.size == 1) return texts.single()
            }
        }
        return null
    }

    private fun findByNotesLabel(root: Component): JTextComponent? {
        var found: JTextComponent? = null
        walk(root) { component ->
            if (found != null) return@walk
            val labelText = when (component) {
                is JLabel -> component.text
                else -> null
            }?.trim().orEmpty()
            if (labelText.isEmpty() || !labelText.contains("note", ignoreCase = true)) return@walk
            val parent = component.parent ?: return@walk
            val texts = collectMessageTextComponents(parent)
                .filter { !looksLikeHttpRequest(it.text) && !looksLikeHttpResponse(it.text) }
            if (texts.size == 1) {
                found = texts.single()
            }
        }
        return found
    }

    private fun findByAccessibleNotesName(root: Component): JTextComponent? {
        val matches = mutableListOf<JTextComponent>()
        walk(root) { component ->
            if (component !is JTextComponent) return@walk
            val name = component.accessibleContext?.accessibleName?.trim().orEmpty()
            if (name.contains("note", ignoreCase = true)) {
                matches += component
            }
        }
        return when (matches.size) {
            1 -> matches.single()
            else -> null
        }
    }

    private fun findResidualNotesEditor(
        root: Component,
        messageEditors: RepeaterUiDiscovery.TabEditors,
    ): NotesLookup {
        val request = messageEditors.request
        val response = messageEditors.response
        val extras = collectMessageTextComponents(root)
            .filter { it !== request && it !== response }
            .filter { !looksLikeHttpRequest(it.text) }
            .filter { !looksLikeHttpResponse(it.text) }

        val editable = extras.filter { it.isEditable }
        when {
            editable.size == 1 -> return NotesLookup(editable.single(), ambiguous = false)
            editable.size > 1 -> {
                val named = editable.filter {
                    it.accessibleContext?.accessibleName?.contains("note", ignoreCase = true) == true
                }
                return when (named.size) {
                    1 -> NotesLookup(named.single(), ambiguous = false)
                    else -> NotesLookup(null, ambiguous = true)
                }
            }
            extras.size == 1 -> return NotesLookup(extras.single(), ambiguous = false)
            extras.size > 1 -> return NotesLookup(null, ambiguous = true)
            else -> return NotesLookup(null, ambiguous = false)
        }
    }

    private fun looksLikeHttpRequest(text: String): Boolean {
        val line = text.lineSequence().firstOrNull()?.trim()?.uppercase() ?: return false
        if (line.startsWith("HTTP/")) return false
        return HTTP_METHODS.any { line.startsWith("$it ") }
    }

    private fun looksLikeHttpResponse(text: String): Boolean =
        text.trimStart().uppercase().startsWith("HTTP/")

    /** Burp often shows Notes read-only until focused; flip editable when the component allows it. */
    private fun ensureWritable(target: JTextComponent): Boolean {
        if (target.isEditable) return true
        target.isEditable = true
        target.requestFocusInWindow()
        return target.isEditable
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

    private fun collectMessageTextComponents(root: Component): List<JTextComponent> {
        val out = mutableListOf<JTextComponent>()
        walk(root) { component ->
            if (component is JTextComponent) out += component
        }
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
