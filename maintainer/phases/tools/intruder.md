# Intruder — MCP tool ledger

**Suite tab / UI:** Intruder (positions, payloads, attack types).  
**Related:** [`../montoya-gaps.md`](../montoya-gaps.md)

**Path reality:** **`send_to_intruder`** is Montoya today; list/read/run attacks need **Swing** or future Montoya APIs.

---

## Create / send (Montoya)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `send_to_intruder` | Open request in Intruder | Montoya | done | |

---

## Backlog

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `list_intruder_tabs` | Tab strip enumeration | Swing | todo | Same patterns as Repeater |
| `start_intruder_attack` | Start configured attack | Swing | todo | High risk — approval |

---

## Suggested implement order

1. Tab list spike (reuse suite-ui-session)
2. Read-only attack config before any “start” tool
