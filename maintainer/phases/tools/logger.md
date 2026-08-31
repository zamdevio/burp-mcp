# Logger — MCP tool ledger

**Suite tab / UI:** Logger (HTTP log, filters).  
**Related:** Proxy history overlap — prefer Montoya proxy/history where sufficient.

**Path reality:** Most logging access via **Montoya** history APIs; Logger **UI** filters → Swing if needed.

---

## Backlog

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_logger_entries` | Paginated log read | Montoya / Swing | todo | Check Montoya Logger API first |

---

## Suggested implement order

1. Montoya gap check in [`../montoya-gaps.md`](../montoya-gaps.md)
2. Only add Swing if Montoya cannot filter/paginate
