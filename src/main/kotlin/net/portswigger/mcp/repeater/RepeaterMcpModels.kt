package net.portswigger.mcp.repeater

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class RepeaterContextSnapshot(
    val observedAtEpochMs: Long,
    val suiteTool: String?,
    val repeaterSuiteFocused: Boolean,
    val selectedTab: RepeaterTabInfo?,
    val tabs: List<RepeaterTabInfo>,
    val warnings: List<String> = emptyList(),
)

/**
 * JSON body returned by Repeater Swing MCP tools (live context, not server cache).
 */
@Serializable
data class RepeaterToolEnvelope(
    val ok: Boolean,
    val message: String? = null,
    val error: String? = null,
    val context: RepeaterContextSnapshot,
    val data: JsonElement? = null,
)
