package net.portswigger.mcp.tools

/**
 * Single catalog of MCP tools for Swing UI and registration bookkeeping.
 * Runtime [record] calls from [mcpTool] upsert entries; [ensureBuiltIn] seeds
 * the list before the server starts so the MCP tab Tools pane is never empty.
 */
enum class ToolTag {
    Pro,
    Approval,
    ConfigEdit,
    Swing,
    Mutating,
}

data class ToolEntry(
    val name: String,
    val description: String,
    val group: String,
    val tags: Set<ToolTag> = emptySet(),
) {
    fun badgeLabels(): List<String> = tags.map {
        when (it) {
            ToolTag.Pro -> "Pro"
            ToolTag.Approval -> "Approval"
            ToolTag.ConfigEdit -> "Config edit"
            ToolTag.Swing -> "Swing UI"
            ToolTag.Mutating -> "Mutating"
        }
    }
}

object ToolCatalog {
    private val lock = Any()
    private val byName = linkedMapOf<String, ToolEntry>()

    fun clear() = synchronized(lock) { byName.clear() }

    fun record(
        name: String,
        description: String,
        tags: Set<ToolTag> = inferTags(name),
        group: String = inferGroup(name),
    ) = synchronized(lock) {
        byName[name] = ToolEntry(name, description, group, tags)
    }

    fun all(): List<ToolEntry> = synchronized(lock) { byName.values.toList() }

    fun visibleForEdition(professional: Boolean): List<ToolEntry> =
        all().filter { professional || ToolTag.Pro !in it.tags }

    fun ensureBuiltIn() {
        synchronized(lock) {
            for (entry in builtInEntries()) {
                byName.putIfAbsent(entry.name, entry)
            }
        }
    }

    fun inferGroup(name: String): String = when {
        name.startsWith("send_http") || name == "create_repeater_tab" ||
            name == "create_repeater_tab_http2" || name == "send_to_intruder" -> "HTTP"
        name.contains("repeater") || name.contains("active_editor") -> "Repeater"
        name.contains("proxy") && name.contains("intercept") -> "Config"
        name.contains("proxy") || name.contains("websocket") -> "Proxy"
        name.contains("organizer") -> "Organizer"
        name.contains("collaborator") || name.contains("scanner") -> "Pro"
        name == "is_in_scope" || name.contains("scope") -> "Scope"
        name == "get_burp_version" || name == "get_project_info" -> "Info"
        name.startsWith("url_") || name.startsWith("base64_") ||
            name == "generate_random_string" -> "Utilities"
        name.contains("options") || name.contains("task_execution") -> "Config"
        else -> "Other"
    }

    fun inferTags(name: String): Set<ToolTag> {
        val tags = mutableSetOf<ToolTag>()
        if (name.contains("scanner") || name.contains("collaborator")) tags += ToolTag.Pro
        if (name.startsWith("send_http") || name.contains("history") || name.contains("organizer")) {
            tags += ToolTag.Approval
        }
        if (name.startsWith("set_project_options") || name.startsWith("set_user_options")) {
            tags += ToolTag.ConfigEdit
        }
        if (name.contains("repeater") || name.contains("active_editor")) tags += ToolTag.Swing
        if (name.startsWith("send_") || name.startsWith("set_") || name.startsWith("create_") ||
            name == "select_repeater_tab"
        ) {
            tags += ToolTag.Mutating
        }
        return tags
    }

    internal fun builtInEntries(): List<ToolEntry> = listOf(
        entry("send_http1_request", "Issues an HTTP/1.1 request and returns the response."),
        entry(
            "send_http2_request",
            "Issues an HTTP/2 request and returns the response. Do NOT pass headers to the body parameter.",
        ),
        entry(
            "create_repeater_tab",
            "Creates an HTTP/1.1 Repeater tab with the specified raw HTTP request and optional tab name.",
        ),
        entry(
            "create_repeater_tab_http2",
            "Creates an HTTP/2 Repeater tab with the specified HTTP/2 request and optional tab name.",
        ),
        entry("send_to_intruder", "Sends an HTTP request to Intruder with the specified HTTP request and optional tab name."),
        entry("url_encode", "URL encodes the input string"),
        entry("url_decode", "URL decodes the input string"),
        entry("base64_encode", "Base64 encodes the input string"),
        entry("base64_decode", "Base64 decodes the input string"),
        entry("generate_random_string", "Generates a random string of specified length and character set"),
        entry("output_project_options", "Outputs current project-level configuration in JSON format. You can use this to determine the schema for available config options."),
        entry("output_user_options", "Outputs current user-level configuration in JSON format. You can use this to determine the schema for available config options."),
        entry("set_project_options", "Sets project-level configuration in JSON format (requires config editing)."),
        entry("set_user_options", "Sets user-level configuration in JSON format (requires config editing)."),
        entry("get_scanner_issues", "Displays information about issues identified by the scanner"),
        entry("generate_collaborator_payload", "Generates a Burp Collaborator payload URL for OOB testing."),
        entry("get_collaborator_interactions", "Polls Burp Collaborator for out-of-band interactions."),
        entry("get_proxy_http_history", "Displays items within the proxy HTTP history"),
        entry("get_proxy_http_history_regex", "Displays items matching a specified regex within the proxy HTTP history"),
        entry("get_organizer_items", "Displays items within the Organizer tab"),
        entry("get_organizer_items_regex", "Displays items matching a specified regex within the Organizer tab"),
        entry("get_proxy_websocket_history", "Displays items within the proxy WebSocket history"),
        entry(
            "get_proxy_websocket_history_regex",
            "Displays items matching a specified regex within the proxy WebSocket history",
        ),
        entry("set_task_execution_engine_state", "Sets the state of Burp's task execution engine (paused or unpaused)"),
        entry("set_proxy_intercept_state", "Enables or disables Burp Proxy Intercept"),
        entry(
            "get_proxy_intercept_state",
            "Returns whether Burp Proxy intercept is currently enabled (pairs with set_proxy_intercept_state).",
        ),
        entry(
            "get_burp_version",
            "Returns Burp Suite product name, edition, build number, and version string for agent context.",
        ),
        entry(
            "is_in_scope",
            "Returns whether the given URL is within Burp's Suite-wide target scope.",
        ),
        entry("include_in_scope", "Includes the given URL in Burp's Suite-wide target scope."),
        entry("exclude_from_scope", "Excludes the given URL from Burp's Suite-wide target scope."),
        entry("get_project_info", "Returns the current Burp project name and id for agent context."),
        entry("get_active_editor_contents", "Outputs the contents of the user's active message editor"),
        entry("set_active_editor_contents", "Sets the content of the user's active message editor"),
        entry("list_repeater_tabs", "Lists currently discoverable Burp Repeater tabs"),
        entry("get_repeater_tab", "Returns structured information for a Repeater tab"),
        entry("get_repeater_tab_request", "Returns the raw HTTP request in a Repeater tab"),
        entry("get_repeater_tab_response", "Returns the HTTP response in a Repeater tab"),
        entry("set_repeater_tab_request", "Sets the raw HTTP request in a Repeater tab"),
        entry("get_active_repeater_tab", "Returns the currently selected Repeater tab"),
        entry("select_repeater_tab", "Selects the given Repeater tab"),
        entry("set_repeater_tab_title", "Renames a Repeater tab"),
        entry("get_repeater_tab_notes", "Returns the Notes text for a Repeater tab"),
        entry("set_repeater_tab_notes", "Replaces the Notes text for a Repeater tab"),
        entry("send_repeater_tab", "Clicks Repeater Send for a tab (requires Target set)"),
        entry("send_repeater_tab_and_get_response", "Sends a Repeater tab and waits for an HTTP response"),
    )

    private fun entry(name: String, description: String) =
        ToolEntry(name, description, inferGroup(name), inferTags(name))
}
