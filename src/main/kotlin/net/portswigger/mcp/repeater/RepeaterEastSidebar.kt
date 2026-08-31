package net.portswigger.mcp.repeater

import java.awt.Component
import java.awt.Container
import javax.swing.AbstractButton
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import kotlinx.serialization.Serializable

/**
 * Repeater east inspector rail (Notes / Explanations / …) — observe and mutate with restore.
 */
internal object RepeaterEastSidebar {

    private const val SETTLE_MS = 150L

    @Serializable
    data class EastSidebarState(
        val visible: Boolean,
        val selectedRailTab: String?,
        val railTabs: List<String>,
    )

    data class Snapshot(
        val selectedIndex: Int,
        val expanded: Boolean,
    )

    fun readState(discovered: RepeaterUiDiscovery.DiscoveredRepeater): RepeaterUiDiscovery.Outcome<EastSidebarState> {
        val pane = findRail(discovered)
            ?: return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.EastSidebarNotFound())
        return RepeaterUiDiscovery.Outcome.Ok(buildState(pane))
    }

    fun captureSnapshot(discovered: RepeaterUiDiscovery.DiscoveredRepeater): Snapshot? {
        val pane = findRail(discovered) ?: return null
        return Snapshot(
            selectedIndex = pane.selectedIndex.coerceAtLeast(0),
            expanded = isRailExpanded(pane),
        )
    }

    fun restoreSnapshot(discovered: RepeaterUiDiscovery.DiscoveredRepeater, snapshot: Snapshot) {
        val pane = findRail(discovered) ?: return
        setRailExpanded(discovered, snapshot.expanded)
        if (snapshot.selectedIndex in 0 until pane.tabCount) {
            pane.selectedIndex = snapshot.selectedIndex
        }
        Thread.sleep(SETTLE_MS)
    }

    fun setVisible(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        visible: Boolean,
    ): RepeaterUiDiscovery.Outcome<Unit> {
        val pane = findRail(discovered)
            ?: return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.EastSidebarNotFound())
        val before = isRailExpanded(pane)
        if (before == visible) {
            return RepeaterUiDiscovery.Outcome.Ok(Unit)
        }
        if (!toggleRailExpanded(discovered, targetExpanded = visible)) {
            return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.EastSidebarToggleNotFound())
        }
        Thread.sleep(SETTLE_MS)
        val after = isRailExpanded(findRail(discovered) ?: pane)
        return if (after == visible) {
            RepeaterUiDiscovery.Outcome.Ok(Unit)
        } else {
            RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.EastSidebarDidNotUpdate())
        }
    }

    fun selectRailTab(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        tabTitle: String,
    ): RepeaterUiDiscovery.Outcome<String> {
        val pane = findRail(discovered)
            ?: return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.EastSidebarNotFound())
        val trimmed = tabTitle.trim()
        if (trimmed.isEmpty()) {
            return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.InvalidEastRailTab(trimmed))
        }
        val index = (0 until pane.tabCount).firstOrNull { i ->
            pane.getTitleAt(i)?.trim().equals(trimmed, ignoreCase = true)
        } ?: return RepeaterUiDiscovery.Outcome.Err(
            RepeaterUiDiscovery.DiscoveryError.EastRailTabNotFound(trimmed, railTabTitles(pane)),
        )
        pane.selectedIndex = index
        Thread.sleep(SETTLE_MS)
        val selected = pane.getTitleAt(pane.selectedIndex)?.trim().orEmpty()
        return if (selected.equals(trimmed, ignoreCase = true)) {
            RepeaterUiDiscovery.Outcome.Ok(selected)
        } else {
            RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.EastSidebarDidNotUpdate())
        }
    }

    fun <T> withRestore(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        block: () -> T,
    ): T {
        val snapshot = captureSnapshot(discovered)
        return try {
            block()
        } finally {
            snapshot?.let { restoreSnapshot(discovered, it) }
        }
    }

    private fun findRail(discovered: RepeaterUiDiscovery.DiscoveredRepeater): JTabbedPane? =
        RepeaterNotesSidebar.findEastRailTabbedPane(
            discovered.repeaterRoot,
            discovered.tabStrip,
            discovered.suiteFrame,
        )

    private fun buildState(pane: JTabbedPane): EastSidebarState {
        val tabs = railTabTitles(pane)
        val selected = pane.selectedIndex.takeIf { it in tabs.indices }?.let { tabs[it] }
        return EastSidebarState(
            visible = isRailExpanded(pane),
            selectedRailTab = selected,
            railTabs = tabs,
        )
    }

    private fun railTabTitles(pane: JTabbedPane): List<String> =
        (0 until pane.tabCount).map { pane.getTitleAt(it)?.trim().orEmpty() }

    internal fun isRailExpanded(pane: JTabbedPane): Boolean {
        if (!pane.isVisible) return false
        var cur: Component? = pane
        while (cur != null) {
            if (!cur.isVisible) return false
            cur = cur.parent
        }
        if (pane.width < 32 || pane.height < 32) return false
        val split = findAncestorSplitPane(pane)
        if (split != null && split.width > 0) {
            val loc = split.dividerLocation
            val max = split.maximumDividerLocation
            if (loc >= max - 4) return false
        }
        return pane.isShowing
    }

    private fun setRailExpanded(discovered: RepeaterUiDiscovery.DiscoveredRepeater, expanded: Boolean) {
        val pane = findRail(discovered) ?: return
        if (isRailExpanded(pane) == expanded) return
        toggleRailExpanded(discovered, targetExpanded = expanded)
    }

    private fun toggleRailExpanded(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        targetExpanded: Boolean,
    ): Boolean {
        val pane = findRail(discovered) ?: return false
        findCollapseExpandButton(discovered, pane)?.let { button ->
            if (button.isEnabled) {
                button.doClick()
                return true
            }
        }
        val split = findAncestorSplitPane(pane)
        if (split != null) {
            val max = split.maximumDividerLocation
            split.dividerLocation = if (targetExpanded) (max * 0.72).toInt() else max
            return true
        }
        return false
    }

    private fun findAncestorSplitPane(component: Component): JSplitPane? {
        var cur: Component? = component.parent
        while (cur != null) {
            if (cur is JSplitPane) return cur
            cur = cur.parent
        }
        return null
    }

    private fun findCollapseExpandButton(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        railPane: JTabbedPane,
    ): AbstractButton? {
        val anchor = discovered.repeaterRoot
        val tabStrip = discovered.tabStrip
        val hints = listOf(
            "hide inspector",
            "show inspector",
            "collapse",
            "expand",
            "close panel",
            "open panel",
            "sidebar",
        )
        val buttons = mutableListOf<AbstractButton>()
        for (root in RepeaterNotesSidebar.searchRoots(discovered.repeaterRoot, discovered.suiteFrame)) {
            walkComponents(root) { component ->
                val button = component as? AbstractButton ?: return@walkComponents
                if (RepeaterSwingTree.isUnderComponent(button, tabStrip)) return@walkComponents
                val text = listOf(
                    button.toolTipText,
                    button.text,
                    button.accessibleContext?.accessibleName,
                ).joinToString(" ").lowercase()
                if (hints.any { text.contains(it) }) {
                    buttons += button
                }
            }
        }
        if (buttons.isEmpty()) return null
        val railPoint = runCatching { railPane.locationOnScreen }.getOrNull()
        return buttons.maxByOrNull { button ->
            var score = if (button.isShowing) 100 else 0
            val p = runCatching { button.locationOnScreen }.getOrNull()
            if (railPoint != null && p != null && p.x >= railPoint.x - 80) score += 200
            score
        }
    }

    private fun walkComponents(root: Component, visit: (Component) -> Unit) {
        visit(root)
        if (root is Container) {
            for (child in root.components) {
                walkComponents(child, visit)
            }
        }
    }
}
