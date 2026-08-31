package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import javax.swing.JTabbedPane

/**
 * Close Repeater message tabs via the discovered tab strip ([JTabbedPane]).
 * Does not use a tab registry; re-resolves index by id before each close.
 */
internal object RepeaterTabClose {

    fun closeTab(api: MontoyaApi, tabId: String): RepeaterUiDiscovery.Outcome<String> =
        runOnEdt {
            when (val resolved = resolveTab(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Err -> resolved
                is RepeaterUiDiscovery.Outcome.Ok -> {
                    val (discovered, index) = resolved.value
                    closeAtIndex(discovered.tabStrip, index, tabId)
                }
            }
        }

    fun closeOtherTabs(api: MontoyaApi, keepTabId: String): RepeaterUiDiscovery.Outcome<String> =
        runOnEdt {
            when (val resolved = resolveTab(api, keepTabId)) {
                is RepeaterUiDiscovery.Outcome.Err -> resolved
                is RepeaterUiDiscovery.Outcome.Ok -> {
                    val (discovered, keepIndex) = resolved.value
                    val strip = discovered.tabStrip
                    val toRemove = (strip.tabCount - 1 downTo 0)
                        .filter { it != keepIndex && !RepeaterUiDiscovery.isChromeTabIndex(strip, it) }
                    if (toRemove.isEmpty()) {
                        return@runOnEdt RepeaterUiDiscovery.Outcome.Ok(
                            "No other Repeater tabs to close (keeping $keepTabId)"
                        )
                    }
                    var closed = 0
                    for (idx in toRemove) {
                        when (val result = closeAtIndex(strip, idx, null)) {
                            is RepeaterUiDiscovery.Outcome.Err -> return@runOnEdt result
                            is RepeaterUiDiscovery.Outcome.Ok -> closed++
                        }
                    }
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Closed $closed Repeater tab(s); kept $keepTabId. " +
                            "Remaining message tabs: ${countMessageTabs(strip)} " +
                            "(call list_repeater_tabs for current ids and names)."
                    )
                }
            }
        }

    private fun closeAtIndex(
        strip: JTabbedPane,
        index: Int,
        tabIdForMessage: String?,
    ): RepeaterUiDiscovery.Outcome<String> {
        if (index < 0 || index >= strip.tabCount) {
            return RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.TabNotFound(tabIdForMessage ?: "index-$index")
            )
        }
        if (RepeaterUiDiscovery.isChromeTabIndex(strip, index)) {
            return RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.InvalidTabId("chrome tab at index $index")
            )
        }
        val messageTabs = countMessageTabs(strip)
        if (messageTabs <= 1) {
            return RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.CannotCloseLastTab()
            )
        }
        return try {
            strip.removeTabAt(index)
            val label = tabIdForMessage ?: "index $index"
            val remaining = countMessageTabs(strip)
            RepeaterUiDiscovery.Outcome.Ok(
                "Closed Repeater tab $label. Remaining message tabs: $remaining " +
                    "(ids repeater-tab-0..repeater-tab-${(remaining - 1).coerceAtLeast(0)}; " +
                    "call list_repeater_tabs for names after strip changes)."
            )
        } catch (e: Exception) {
            RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.CloseFailed(e.message ?: e.javaClass.simpleName)
            )
        }
    }

    private fun countMessageTabs(strip: JTabbedPane): Int =
        (0 until strip.tabCount).count { !RepeaterUiDiscovery.isChromeTabIndex(strip, it) }

    private fun resolveTab(
        api: MontoyaApi,
        tabId: String,
    ): RepeaterUiDiscovery.Outcome<Pair<RepeaterUiDiscovery.DiscoveredRepeater, Int>> {
        val index = RepeaterTabId.parseIndex(tabId)
            ?: return RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.InvalidTabId(tabId)
            )
        return when (val discovered = RepeaterUiDiscovery.prepareRepeater(api)) {
            is RepeaterUiDiscovery.Outcome.Err -> discovered
            is RepeaterUiDiscovery.Outcome.Ok -> {
                val strip = discovered.value.tabStrip
                if (index >= strip.tabCount || RepeaterUiDiscovery.isChromeTabIndex(strip, index)) {
                    RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.TabNotFound(tabId))
                } else {
                    RepeaterUiDiscovery.Outcome.Ok(discovered.value to index)
                }
            }
        }
    }
}
