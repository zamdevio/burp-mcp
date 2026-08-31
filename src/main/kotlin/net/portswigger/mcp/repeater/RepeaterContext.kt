package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import net.portswigger.mcp.ui.SuiteUiSession

internal object RepeaterContext {

    private val defaultWarnings = listOf(
        "Tab ids are strip indices (repeater-tab-N); re-list after close, reorder, or manual UI edits.",
    )

    fun capture(api: MontoyaApi): RepeaterUiDiscovery.Outcome<RepeaterContextSnapshot> =
        runOnEdt {
            when (val discovered = RepeaterUiDiscovery.discoverRepeater(api)) {
                is RepeaterUiDiscovery.Outcome.Err -> discovered
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(snapshotFrom(discovered.value))
            }
        }

    fun captureEnsuringRepeater(api: MontoyaApi): RepeaterUiDiscovery.Outcome<RepeaterContextSnapshot> =
        runOnEdt {
            when (val discovered = RepeaterUiDiscovery.discoverRepeater(api)) {
                is RepeaterUiDiscovery.Outcome.Err -> discovered
                is RepeaterUiDiscovery.Outcome.Ok -> {
                    RepeaterUiDiscovery.ensureRepeaterSuiteSelected(discovered.value)
                    RepeaterUiDiscovery.Outcome.Ok(snapshotFrom(discovered.value))
                }
            }
        }

    fun snapshotFrom(discovered: RepeaterUiDiscovery.DiscoveredRepeater): RepeaterContextSnapshot {
        val suite = discovered.suiteTabbedPane
        val strip = discovered.tabStrip
        val focused = SuiteUiSession.isSuiteTabSelected(suite, discovered.suiteRepeaterIndex)
        val tabs = RepeaterUiDiscovery.buildTabInfos(discovered)
        val selected = selectedTabInfo(discovered, tabs, strip.selectedIndex)
        val warnings = buildList {
            addAll(defaultWarnings)
            if (!focused) {
                add("Suite tab is not Repeater; tab-targeted tools still select Repeater briefly during withTab.")
            }
        }
        return RepeaterContextSnapshot(
            observedAtEpochMs = System.currentTimeMillis(),
            suiteTool = SuiteUiSession.selectedSuiteToolTitle(suite),
            repeaterSuiteFocused = focused,
            selectedTab = selected,
            tabs = tabs,
            warnings = warnings,
        )
    }

    private fun selectedTabInfo(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        tabs: List<RepeaterTabInfo>,
        stripIndex: Int,
    ): RepeaterTabInfo? {
        if (stripIndex < 0) return null
        if (RepeaterUiDiscovery.isChromeTabIndex(discovered.tabStrip, stripIndex)) return null
        return tabs.find { it.index == stripIndex }
            ?: RepeaterTabInfo(
                id = RepeaterTabId.fromIndex(stripIndex),
                name = discovered.tabStrip.getTitleAt(stripIndex),
                index = stripIndex,
                selected = true,
                hasRequest = false,
                hasResponse = false,
            )
    }
}
