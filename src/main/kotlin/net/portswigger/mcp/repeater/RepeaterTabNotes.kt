package net.portswigger.mcp.repeater

/**
 * Repeater tab **Notes** read/write via Montoya [burp.api.montoya.core.Annotations]
 * on the tab's [burp.api.montoya.http.message.HttpRequestResponse] (2026.x `burp.Zemd` bridge).
 */
internal object RepeaterTabNotes {

    fun readWhileSelected(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
    ): RepeaterUiDiscovery.Outcome<String> {
        val bundle = RepeaterTabAnnotations.findForTab(discovered, index)
            ?: return RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.NotesNotFound(
                    "Tab annotations bundle not found; ensure Repeater tab is open.",
                ),
            )
        return RepeaterUiDiscovery.Outcome.Ok(RepeaterTabAnnotations.readNotes(bundle).orEmpty())
    }

    fun writeWhileSelected(
        discovered: RepeaterUiDiscovery.DiscoveredRepeater,
        index: Int,
        tabId: String,
        notes: String,
    ): RepeaterUiDiscovery.Outcome<String> {
        val bundle = RepeaterTabAnnotations.findForTab(discovered, index)
            ?: return RepeaterUiDiscovery.Outcome.Err(
                RepeaterUiDiscovery.DiscoveryError.NotesNotFound(
                    "Tab annotations bundle not found; ensure Repeater tab is open.",
                ),
            )
        if (!RepeaterTabAnnotations.writeNotes(bundle, notes)) {
            return RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.NotesDidNotUpdate())
        }
        RepeaterTabSession.settleEditors(discovered, index)
        val readBack = RepeaterTabAnnotations.readNotes(bundle)?.trim()
        return if (readBack == notes.trim()) {
            RepeaterUiDiscovery.Outcome.Ok("Repeater notes have been set for $tabId")
        } else {
            RepeaterUiDiscovery.Outcome.Err(RepeaterUiDiscovery.DiscoveryError.NotesDidNotUpdate())
        }
    }
}
