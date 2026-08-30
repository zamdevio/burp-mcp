package net.portswigger.mcp.repeater

/**
 * Deterministic Repeater tab ids derived from the live UI tab index.
 * Format: `repeater-tab-{index}` (0-based). Re-discover on every call;
 * closing/reordering tabs invalidates previously observed ids.
 */
object RepeaterTabId {
    private val pattern = Regex("^repeater-tab-(\\d+)$")

    fun fromIndex(index: Int): String {
        require(index >= 0) { "tab index must be >= 0" }
        return "repeater-tab-$index"
    }

    fun parseIndex(id: String): Int? {
        val match = pattern.matchEntire(id.trim()) ?: return null
        return match.groupValues[1].toIntOrNull()
    }
}
