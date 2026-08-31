# Suite UI session (Swing tools)

Cross-cutting behavior for MCP tools that drive **Burp suite tabs** via Swing (Repeater today; Proxy/Intruder/Logger later).

**Not:** a tab registry or server-side memo. Burp UI remains the only source of truth; every response carries a **fresh observation**.

---

## Layer

```text
SuiteUiSession.ensureSuiteTool("Repeater")   // suite JTabbedPane selection
  → area discovery (RepeaterUiDiscovery, …)
  → TabSession.withTab / peek / restore
  → RepeaterMcpResponse envelope (context + payload)
```

Code: `ui/SuiteUiSession.kt`, Repeater wiring in `repeater/RepeaterContext.kt` + `repeater/RepeaterMcpResponse.kt`.

---

## Rules

1. **No persistence** — do not cache tab ids across MCP requests for mutations.
2. **`get_repeater_context`** — live read only; same snapshot shape as envelope `context`.
3. **Rich responses** — Repeater Swing tools return JSON **`RepeaterToolEnvelope`**: `ok`, `message`/`error`, `context`, optional `data`.
4. **`ensureRepeater`** — all Repeater discovery tools call this before acting (suite tab **Repeater** selected).
5. **Restore** — tab-targeted mutators still **restore strip + suite** after `withTab` (user UX). `get_active_repeater_tab` **peeks** (ensure Repeater → read → does not leave suite on Variables if we add restore later).
6. **`select_repeater_tab`** — optional; agents pass `tab_id` on other tools. Select leaves Repeater focused (no restore).
7. **Future areas** — same `ensureSuiteTool("<Title>")` pattern; area-specific discovery packages.
8. **Post-mutation context** — envelope `context` is captured **after** the tool body runs; agents treat it as authoritative for that response (see [`state-plane.md`](state-plane.md)).

---

## Context fields (Repeater)

| Field | Meaning |
|-------|---------|
| `observedAtEpochMs` | When snapshot was taken (this call) |
| `suiteTool` | Selected suite tab title (e.g. Variables, Repeater) |
| `repeaterSuiteFocused` | Suite selection is Repeater |
| `selectedTab` | Message tab at strip `selectedIndex` (may be chrome) |
| `tabs` | Message tabs (ids `repeater-tab-N`) |
| `warnings` | e.g. ids index-based, manual UI may have changed |

---

## Related

- [`repeater-ui.md`](repeater-ui.md) · [`phases/repeater-ui.md`](../phases/repeater-ui.md)
- Tool ledger: [`phases/tools/repeater.md`](../phases/tools/repeater.md)
