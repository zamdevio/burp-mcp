package net.portswigger.mcp.repeater

import java.awt.Component
import java.awt.Container
import java.awt.Point
import javax.swing.AbstractButton
import javax.swing.JTabbedPane

/**
 * Repeater **east inspector rail** (Inspector / Notes / Explanations / …) — not message tab strip.
 * Used by east-sidebar MCP tools and [RepeaterNotesScanner] debug scans.
 */
internal object RepeaterNotesSidebar {

    private const val SETTLE_POLL_MS = 25L

    fun findEastRailTabbedPane(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
    ): JTabbedPane? {
        for (root in searchRoots(repeaterRoot, suiteFrame)) {
            for (pane in RepeaterSwingTree.collectTabbedPanes(root)) {
                if (pane === tabStrip) continue
                val titles = (0 until pane.tabCount).map { pane.getTitleAt(it)?.trim().orEmpty() }
                if (titles.none { it.equals("Inspector", ignoreCase = true) }) continue
                if (titles.none { it.equals("Notes", ignoreCase = true) }) continue
                if (titles.any { it.equals("Request", ignoreCase = true) }) continue
                return pane
            }
        }
        return null
    }

    /** Select east rail **Notes** tab; returns that tab's content root (does not mutate message tabs). */
    fun selectEastSidebarNotesTab(
        repeaterRoot: Component,
        tabStrip: JTabbedPane,
        suiteFrame: Component,
    ): Component? {
        val pane = findEastRailTabbedPane(repeaterRoot, tabStrip, suiteFrame) ?: return null
        val notesIndex = (0 until pane.tabCount).indexOfFirst {
            pane.getTitleAt(it)?.trim().equals("Notes", ignoreCase = true)
        }
        if (notesIndex < 0) return null
        pane.selectedIndex = notesIndex
        Thread.sleep(SETTLE_POLL_MS)
        return RepeaterSwingTree.tabContentAt(pane, notesIndex) ?: pane
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

    internal fun searchRoots(repeaterRoot: Component, suiteFrame: Component): List<Component> =
        if (repeaterRoot === suiteFrame) {
            listOf(repeaterRoot)
        } else {
            listOf(repeaterRoot, suiteFrame)
        }

    private fun findNotesRailButton(searchRoot: Component, tabStrip: JTabbedPane): AbstractButton? {
        val buttons = mutableListOf<AbstractButton>()
        RepeaterSwingTree.walk(searchRoot) { component ->
            val button = component as? AbstractButton ?: return@walk
            if (RepeaterSwingTree.isUnderComponent(button, tabStrip)) return@walk
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
            var s = 0
            val p = safeLocationOnScreen(button)
            if (p != null && p.x > rootPoint.x + searchRoot.width / 2) s += 1000
            if (button.isShowing) s += 500
            s
        }
    }

    private fun safeLocationOnScreen(c: Component): Point? =
        if (c.isShowing) {
            runCatching { c.locationOnScreen }.getOrNull()
        } else {
            null
        }
}
