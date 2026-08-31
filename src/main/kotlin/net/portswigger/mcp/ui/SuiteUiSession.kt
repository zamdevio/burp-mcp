package net.portswigger.mcp.ui

import java.awt.Component
import java.awt.Container
import javax.swing.JTabbedPane

/**
 * Suite-level tab selection (Dashboard, Proxy, Repeater, …).
 * Shared by Swing-backed MCP tools; not a tab registry.
 */
internal object SuiteUiSession {

    fun ensureSuiteTabSelected(suitePane: JTabbedPane, suiteToolIndex: Int) {
        if (suiteToolIndex in 0 until suitePane.tabCount) {
            suitePane.selectedIndex = suiteToolIndex
        }
    }

    fun selectedSuiteToolTitle(suitePane: JTabbedPane): String? =
        suitePane.getTitleAt(suitePane.selectedIndex)?.trim()?.takeIf { it.isNotEmpty() }

    fun isSuiteTabSelected(suitePane: JTabbedPane, suiteToolIndex: Int): Boolean =
        suitePane.selectedIndex == suiteToolIndex

    /**
     * Find index of a suite tool tab by title (case-insensitive exact match).
     */
    fun findSuiteToolIndex(root: Component, toolTitle: String): Int? {
        val want = toolTitle.trim()
        for (pane in collectTabbedPanes(root)) {
            for (i in 0 until pane.tabCount) {
                val title = pane.getTitleAt(i)?.trim() ?: continue
                if (title.equals(want, ignoreCase = true)) {
                    return i
                }
            }
        }
        return null
    }

    private fun collectTabbedPanes(root: Component): List<JTabbedPane> {
        val out = mutableListOf<JTabbedPane>()
        walk(root) { if (it is JTabbedPane) out += it }
        return out
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
