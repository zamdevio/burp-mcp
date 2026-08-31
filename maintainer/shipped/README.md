# Shipped

Receipts for contributors and agents — **check before re-implementing**.

| When | Item |
|------|------|
| 2026-08-30 | Repeater MCP tools: list/get/set/select/title + models (`repeater-tab-N`) |
| 2026-08-30 | Swing discovery: suite Repeater tab strip, null-safe `getComponentAt`, shared editor + `JTextComponent` |
| 2026-08-30 | Repeater **Send**: `send_repeater_tab` + `send_repeater_tab_and_get_response` (`RepeaterSend`); block when Target is Not specified; dismiss Configure-target dialog |
| 2026-08-30 | Repeater **enforce**: select → settle → act → restore (`RepeaterTabSession`); act while selected; editor-did-not-update error |
| 2026-08-30 | Docs: [`docs/repeater.md`](../../docs/repeater.md) user guide |
| 2026-08-30 | Agent guides v1 under [`docs/guides/`](../../docs/guides/) |
| 2026-08-30 | `scripts/mcp/check.sh` + `test.sh` (host auto-detect, SSE handshake) |
| 2026-08-30 | MCP tab **Tools** catalog v1 (`ToolCatalog` + `ToolsPanel`; Server \| Tools tabs) |
| 2026-08-30 | Montoya: `get_proxy_intercept_state`, `get_burp_version`, `is_in_scope` |
| 2026-08-30 | Montoya: `include_in_scope`, `exclude_from_scope`, `get_project_info` |
| 2026-08-31 | Repeater: `get_repeater_tab_notes`, `set_repeater_tab_notes` |
| 2026-08-30 | Live validation: tab list + request/response + Send/target-gate on Burp 2026.x |
| 2026-08-30 | Maintainer scaffold: phases, systems, agents, `.cursor`, `docs/`, `scripts/` |

## Upstream baseline

Fork/extend of [PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server) — original HTTP/proxy/collaborator/config tools.
