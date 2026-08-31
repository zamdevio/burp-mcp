# Proxy — MCP tool ledger

**Suite tab / UI:** Proxy (intercept, HTTP/WebSocket history).  
**Related:** [`../montoya-gaps.md`](../montoya-gaps.md) · user tools in [`../../../docs/tools.md`](../../../docs/tools.md)

**Path reality:** History + intercept state mostly **Montoya**; future UI tools (intercept queue chrome) → **Swing** + [`../../systems/suite-ui-session.md`](../../systems/suite-ui-session.md).

---

## History & intercept (Montoya)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_proxy_http_history` | Paginated HTTP history | Montoya | done | Approval gate |
| `get_proxy_http_history_regex` | Filtered HTTP history | Montoya | done | |
| `get_proxy_websocket_history` | WebSocket history | Montoya | done | |
| `get_proxy_websocket_history_regex` | Filtered WS history | Montoya | done | |
| `set_proxy_intercept_state` | Enable/disable intercept | Montoya | done | |
| `get_proxy_intercept_state` | Read intercept on/off | Montoya | done | |

---

## UI backlog (Swing)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_proxy_intercept_queue` | List queued intercept items | Swing | todo | Spike |

---

## Suggested implement order

1. Intercept queue read (if agents need it)
2. `ensureSuiteTool("Proxy")` when adding Swing proxy tools
