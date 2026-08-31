# MCP tools (reference)

Burp exposes tools over MCP (SSE or stdio proxy). Names are snake_case; exact schemas appear in your client’s `list_tools` / tool descriptors.

Use only on systems you are authorized to test.

---

## HTTP

| Tool | Purpose |
|------|---------|
| `send_http1_request` | Send HTTP/1.1 via Montoya; returns response (subject to approval) |
| `send_http2_request` | Send HTTP/2; do not put headers in the body field |

---

## Repeater

**Create (Montoya):**

| Tool | Purpose |
|------|---------|
| `create_repeater_tab` | Open a new Repeater tab with an HTTP/1 request |
| `create_repeater_tab_http2` | Same for HTTP/2 |

**Inspect / control tabs (Swing — Burp UI required):**

| Tool | Purpose |
|------|---------|
| `list_repeater_tabs` | Ids `repeater-tab-N`, names, selection, hasRequest / hasResponse |
| `get_repeater_tab` | Structured tab detail + messages when identifiable |
| `get_repeater_tab_request` | Raw request text for a tab id |
| `get_repeater_tab_response` | Raw response text for a tab id |
| `set_repeater_tab_request` | Replace request editor contents for a tab id |
| `get_active_repeater_tab` | Currently selected Repeater tab (not keyboard focus elsewhere) |
| `select_repeater_tab` | Select a tab in the Repeater strip |
| `set_repeater_tab_title` | Rename a tab |
| `get_repeater_tab_notes` | Read the tab Notes panel |
| `set_repeater_tab_notes` | Replace tab Notes (agent handoff) |
| `close_repeater_tab` | Close one message tab |
| `close_other_repeater_tabs` | Close all except keep_tab_id |
| `send_repeater_tab` | Click Repeater **Send** for a tab (select → settle → restore) |
| `send_repeater_tab_and_get_response` | Send and wait for HTTP response in the editor (timeout optional) |

**Focused editor (any message editor with focus):**

| Tool | Purpose |
|------|---------|
| `get_active_editor_contents` | Contents of the focused message editor |
| `set_active_editor_contents` | Set contents if the focused editor is editable |

Repeater deep dive (Target gate, agent loops): [`repeater.md`](repeater.md). Caveats: [`limitations.md`](limitations.md). Design notes: [`maintainer/phases/repeater-ui.md`](../maintainer/phases/repeater-ui.md).

---

## Intruder

| Tool | Purpose |
|------|---------|
| `send_to_intruder` | Send a request into Intruder |

---

## Proxy

| Tool | Purpose |
|------|---------|
| `get_proxy_http_history` | Paginated / filtered HTTP history (approval may apply) |
| `get_proxy_http_history_regex` | HTTP history filtered by regex |
| `get_proxy_websocket_history` | WebSocket history |
| `get_proxy_websocket_history_regex` | WebSocket history filtered by regex |
| `set_proxy_intercept_state` | Enable or disable Proxy intercept |
| `get_proxy_intercept_state` | Whether Proxy intercept is currently enabled |

---

## Scope & info

| Tool | Purpose |
|------|---------|
| `is_in_scope` | Whether a URL is in Suite-wide target scope |
| `include_in_scope` | Add a URL to Suite-wide target scope |
| `exclude_from_scope` | Remove a URL from Suite-wide target scope |
| `get_burp_version` | Product name, edition, build number, version string |
| `get_project_info` | Current project name and id |

---

## Organizer

| Tool | Purpose |
|------|---------|
| `get_organizer_items` | Organizer items |
| `get_organizer_items_regex` | Organizer items filtered by regex |

---

## Scanner & Collaborator (Professional)

| Tool | Purpose |
|------|---------|
| `get_scanner_issues` | Scanner issues |
| `generate_collaborator_payload` | New Collaborator payload |
| `get_collaborator_interactions` | Collaborator interactions for payloads |

---

## Config & engine

| Tool | Purpose |
|------|---------|
| `output_project_options` | Export project-level options JSON |
| `output_user_options` | Export user-level options JSON |
| `set_project_options` | Merge project options (requires config-edit enabled in MCP tab) |
| `set_user_options` | Merge user options (same gate) |
| `set_task_execution_engine_state` | Pause / unpause Burp’s task execution engine |

Always export before merge so you know the schema. Follow the tool descriptions for top-level JSON object names.

---

## Utilities

| Tool | Purpose |
|------|---------|
| `url_encode` / `url_decode` | URL coding |
| `base64_encode` / `base64_decode` | Base64 |
| `generate_random_string` | Random string with length / charset |

---

## Implementation

- Handlers: `src/main/kotlin/net/portswigger/mcp/tools/`
- Patterns: [`maintainer/systems/mcp-tools.md`](../maintainer/systems/mcp-tools.md)
- Per-area done/todo ledgers: [`maintainer/phases/tools/`](../maintainer/phases/tools/)
- Montoya index: [`maintainer/phases/montoya-gaps.md`](../maintainer/phases/montoya-gaps.md)
