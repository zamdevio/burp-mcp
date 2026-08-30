package net.portswigger.mcp.repeater

import javax.swing.text.JTextComponent

/**
 * Stateless select → settle → act → restore for Repeater tab-targeted Swing ops.
 * Does not cache tabs; callers re-discover before each session.
 */
internal object RepeaterTabSession {

    /** Max wall time spent waiting for editor text to stabilize after select (EDT). */
    const val SETTLE_BUDGET_MS: Long = 200L

    /** Poll interval while settling (EDT sleep — intentional, bounded). */
    const val SETTLE_POLL_MS: Long = 10L

    /** Consecutive identical fingerprints required before settle succeeds. */
    const val SETTLE_STABLE_READS: Int = 2

    data class UiSnapshot(
        val suiteIndex: Int,
        val stripIndex: Int,
    )

    fun capture(discovered: RepeaterUiDiscovery.DiscoveredRepeater): UiSnapshot =
        UiSnapshot(
            suiteIndex = discovered.suiteTabbedPane.selectedIndex,
            stripIndex = discovered.tabStrip.selectedIndex,
        )

    fun select(discovered: RepeaterUiDiscovery.DiscoveredRepeater, index: Int) {
        discovered.suiteTabbedPane.selectedIndex = discovered.suiteRepeaterIndex
        discovered.tabStrip.selectedIndex = index
    }

    fun restore(discovered: RepeaterUiDiscovery.DiscoveredRepeater, snapshot: UiSnapshot) {
        val strip = discovered.tabStrip
        val suite = discovered.suiteTabbedPane
        if (snapshot.stripIndex in 0 until strip.tabCount) {
            strip.selectedIndex = snapshot.stripIndex
        }
        if (snapshot.suiteIndex in 0 until suite.tabCount) {
            suite.selectedIndex = snapshot.suiteIndex
        }
    }

    /**
     * Select [index], optionally settle shared editors, run [block], then restore prior UI.
     * Always restores even when [block] throws.
     */
    fun <T> withTab(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        settle: Boolean = true,
        block: () -> T,
    ): T {
        val snapshot = capture(discovered)
        select(discovered, index)
        return try {
            if (settle) {
                settleEditors(discovered, index)
            }
            block()
        } finally {
            restore(discovered, snapshot)
        }
    }

    /**
     * Wait until request/response fingerprints are stable, or [SETTLE_BUDGET_MS] elapses.
     * Same-tab re-entry settles quickly (already stable).
     */
    fun settleEditors(discovered: RepeaterUiDiscovery.DiscoveredRepeater, index: Int) {
        val deadline = System.nanoTime() + SETTLE_BUDGET_MS * 1_000_000L
        var lastFingerprint: String? = null
        var stable = 0
        while (System.nanoTime() < deadline) {
            // Ensure tab selection stuck (Burp may briefly bounce).
            if (discovered.tabStrip.selectedIndex != index) {
                discovered.tabStrip.selectedIndex = index
            }
            val fingerprint = editorFingerprint(discovered, index)
            if (fingerprint == lastFingerprint) {
                stable++
                if (stable >= SETTLE_STABLE_READS) {
                    return
                }
            } else {
                lastFingerprint = fingerprint
                stable = 0
            }
            Thread.sleep(SETTLE_POLL_MS)
        }
    }

    fun editorFingerprint(discovered: RepeaterUiDiscovery.DiscoveredRepeater, index: Int): String {
        val editors = RepeaterUiDiscovery.findEditorsWhileSelected(discovered, index)
        return fingerprintOf(editors.request) + "\u0000" + fingerprintOf(editors.response)
    }

    private fun fingerprintOf(component: JTextComponent?): String {
        if (component == null) return ""
        val text = component.text ?: ""
        // Length + hash keeps settle cheap for large bodies.
        return "${text.length}:${text.hashCode()}"
    }
}
