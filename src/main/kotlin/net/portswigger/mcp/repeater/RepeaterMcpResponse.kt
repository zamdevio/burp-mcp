package net.portswigger.mcp.repeater

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

internal object RepeaterMcpResponse {

    private val json = Json { encodeDefaults = true }

    fun encodeOk(
        context: RepeaterContextSnapshot,
        message: String? = null,
        data: JsonElement? = null,
    ): String = json.encodeToString(
        RepeaterToolEnvelope(ok = true, message = message, context = context, data = data)
    )

    fun encodeErr(
        context: RepeaterContextSnapshot,
        error: String,
        data: JsonElement? = null,
    ): String = json.encodeToString(
        RepeaterToolEnvelope(ok = false, error = error, context = context, data = data)
    )

    fun textData(value: String): JsonElement = JsonPrimitive(value)

    fun envelopeFromOutcome(
        contextOutcome: RepeaterUiDiscovery.Outcome<RepeaterContextSnapshot>,
        block: (RepeaterContextSnapshot) -> RepeaterUiDiscovery.Outcome<String>,
    ): String {
        return when (contextOutcome) {
            is RepeaterUiDiscovery.Outcome.Err ->
                minimalErr(contextOutcome.error.message)
            is RepeaterUiDiscovery.Outcome.Ok -> {
                val ctx = contextOutcome.value
                when (val result = block(ctx)) {
                    is RepeaterUiDiscovery.Outcome.Ok -> encodeOk(ctx, message = result.value)
                    is RepeaterUiDiscovery.Outcome.Err -> encodeErr(ctx, error = result.error.message)
                }
            }
        }
    }

    fun envelopeFromOutcomeWithData(
        contextOutcome: RepeaterUiDiscovery.Outcome<RepeaterContextSnapshot>,
        block: (RepeaterContextSnapshot) -> RepeaterUiDiscovery.Outcome<Pair<String, JsonElement?>>,
        contextAfter: () -> RepeaterContextSnapshot,
    ): String {
        return when (contextOutcome) {
            is RepeaterUiDiscovery.Outcome.Err ->
                minimalErr(contextOutcome.error.message)
            is RepeaterUiDiscovery.Outcome.Ok -> {
                when (val result = block(contextOutcome.value)) {
                    is RepeaterUiDiscovery.Outcome.Ok -> {
                        val (msg, data) = result.value
                        encodeOk(contextAfter(), message = msg, data = data)
                    }
                    is RepeaterUiDiscovery.Outcome.Err ->
                        encodeErr(contextAfter(), error = result.error.message)
                }
            }
        }
    }

    private fun minimalErr(error: String): String {
        val ctx = RepeaterContextSnapshot(
            observedAtEpochMs = System.currentTimeMillis(),
            suiteTool = null,
            repeaterSuiteFocused = false,
            selectedTab = null,
            tabs = emptyList(),
            warnings = listOf("Context partial — Repeater UI not available."),
        )
        return encodeErr(ctx, error = error)
    }
}
