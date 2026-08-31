package net.portswigger.mcp.repeater

import burp.api.montoya.MontoyaApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Repeater MCP tool handlers returning [RepeaterToolEnvelope] JSON.
 */
internal object RepeaterToolHandlers {

    private val json = Json { encodeDefaults = true }

    fun getContext(api: MontoyaApi): String =
        when (val result = RepeaterUiDiscovery.getRepeaterContext(api)) {
            is RepeaterUiDiscovery.Outcome.Ok ->
                RepeaterMcpResponse.encodeOk(
                    result.value,
                    message = "Repeater context snapshot (live; not cached).",
                )
            is RepeaterUiDiscovery.Outcome.Err ->
                contextThenErr(api, result.error.message)
        }

    fun scanNotesUi(api: MontoyaApi, tabId: String, needle: String?): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.scanNotesUi(api, tabId, needle)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Notes UI scan for $tabId" to json.encodeToJsonElement(
                            RepeaterNotesScanner.NotesUiScanResult.serializer(),
                            result.value,
                        )
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun listTabs(api: MontoyaApi): String =
        envelope(api) { ctx ->
            when (val result = RepeaterUiDiscovery.listTabs(api)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Listed ${result.value.size} Repeater tab(s)." to json.encodeToJsonElement(
                            ListSerializer(RepeaterTabInfo.serializer()),
                            result.value,
                        )
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun getTab(api: MontoyaApi, tabId: String): String =
        envelope(api) { ctx ->
            when (val result = RepeaterUiDiscovery.getTab(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Repeater tab $tabId" to json.encodeToJsonElement(
                            RepeaterTabDetail.serializer(),
                            result.value,
                        )
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun getTabRequest(api: MontoyaApi, tabId: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.getTabRequest(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Request for $tabId" to JsonPrimitive(result.value)
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun getTabResponse(api: MontoyaApi, tabId: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.getTabResponse(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Response for $tabId" to JsonPrimitive(result.value)
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun setTabRequest(api: MontoyaApi, tabId: String, request: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.setTabRequest(api, tabId, request)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(result.value to null)
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun getActiveTab(api: MontoyaApi): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.getActiveTab(api)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Active Repeater message tab" to json.encodeToJsonElement(
                            RepeaterTabInfo.serializer(),
                            result.value,
                        )
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun selectTab(api: MontoyaApi, tabId: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.selectTab(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(result.value to null)
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun setTabTitle(api: MontoyaApi, tabId: String, title: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.setTabTitle(api, tabId, title)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(result.value to null)
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun getTabNotes(api: MontoyaApi, tabId: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.getTabNotes(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Notes for $tabId" to JsonPrimitive(result.value)
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun setTabNotes(api: MontoyaApi, tabId: String, notes: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.setTabNotes(api, tabId, notes)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(result.value to null)
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun closeTab(api: MontoyaApi, tabId: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.closeTab(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(result.value to null)
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun closeOtherTabs(api: MontoyaApi, keepTabId: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterUiDiscovery.closeOtherTabs(api, keepTabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(result.value to null)
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun sendTab(api: MontoyaApi, tabId: String): String =
        envelope(api) { _ ->
            when (val result = RepeaterSend.sendTab(api, tabId)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(result.value to null)
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    fun sendTabAndGetResponse(api: MontoyaApi, tabId: String, timeoutMs: Long): String =
        envelope(api) { _ ->
            when (val result = RepeaterSend.sendTabAndGetResponse(api, tabId, timeoutMs)) {
                is RepeaterUiDiscovery.Outcome.Ok ->
                    RepeaterUiDiscovery.Outcome.Ok(
                        "Response for $tabId" to JsonPrimitive(result.value)
                    )
                is RepeaterUiDiscovery.Outcome.Err -> RepeaterUiDiscovery.Outcome.Err(result.error)
            }
        }

    private fun envelope(
        api: MontoyaApi,
        block: (RepeaterContextSnapshot) -> RepeaterUiDiscovery.Outcome<Pair<String, JsonElement?>>,
    ): String {
        val ctxOutcome = RepeaterContext.captureEnsuringRepeater(api)
        return RepeaterMcpResponse.envelopeFromOutcomeWithData(ctxOutcome, block) {
            refreshContext(api)
        }
    }

    private fun refreshContext(api: MontoyaApi): RepeaterContextSnapshot =
        when (val c = RepeaterContext.captureEnsuringRepeater(api)) {
            is RepeaterUiDiscovery.Outcome.Ok -> c.value
            is RepeaterUiDiscovery.Outcome.Err ->
                RepeaterContextSnapshot(
                    observedAtEpochMs = System.currentTimeMillis(),
                    suiteTool = null,
                    repeaterSuiteFocused = false,
                    selectedTab = null,
                    tabs = emptyList(),
                    warnings = listOf("Context partial after tool run."),
                )
        }

    private fun contextThenErr(api: MontoyaApi, error: String): String {
        val partial = when (val c = RepeaterContext.capture(api)) {
            is RepeaterUiDiscovery.Outcome.Ok -> c.value
            is RepeaterUiDiscovery.Outcome.Err ->
                RepeaterContextSnapshot(
                    observedAtEpochMs = System.currentTimeMillis(),
                    suiteTool = null,
                    repeaterSuiteFocused = false,
                    selectedTab = null,
                    tabs = emptyList(),
                    warnings = listOf("Context partial."),
                )
        }
        return RepeaterMcpResponse.encodeErr(partial, error = error)
    }
}
