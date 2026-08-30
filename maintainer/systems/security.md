# Security model

## HTTP outbound (`send_http1_request`, `send_http2_request`)

- `McpConfig.requireHttpRequestApproval` — dialog unless target auto-approved.
- Auto-approve list validated in `TargetValidation`.

## Data access (history, organizer, WebSocket)

- `DataAccessSecurity` — optional approval per type; “always allow” persisted in config.

## Config mutation

- `set_project_options` / `set_user_options` require **Enable tools that can edit your config** in MCP tab.
- Optional credential filtering on config export (`filterConfigCredentials`).

## Repeater / active editor

- Same trust model as local user session today — no separate Repeater approval gate.
- Revisit if product requires parity with history approval.

## MCP server bind

- Default bind **`127.0.0.1`** — local clients use the same host in SSE URL.
