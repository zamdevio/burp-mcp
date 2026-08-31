# Phase — Repeater toolbar Target URL

**Status:** **Active bug — fix before claiming Send auto-fill or target MCP tools `done`.**  
**Not end-user docs** until verified.

**Ledgers:** [`tools/repeater.md`](./tools/repeater.md) (Target URL rows) · **Send gate:** `repeater/RepeaterSend.kt` · **Jar/decompile:** [`burp-ui-reference.md`](./burp-ui-reference.md) · **Related:** [`repeater-ui-focus.md`](./repeater-ui-focus.md) (left inspector rail layout)

---

## Problem (live smoke 2026.8 — “Health Check” tab)

| Symptom | Evidence |
|---------|----------|
| Toolbar still shows **Target: Not specified** | User screenshot after MCP Send path |
| URL appears in **request pane search** (bottom of editor), not Target | `https://api.cepatedge.com` in search bar, “0 highlights” |
| `send_repeater_tab` correctly **blocks** with `TargetMissing` | Good gate; bad side effect if auto-fill ran |
| `get/set_repeater_tab_request` **fail** with left inspector rail | `<Unable to uniquely identify Repeater request editor>` — Notes pane adds extra editables |

**Root cause (code today):** `RepeaterSend.ensureTargetUrl()` walks **all** `JTextField` under `repeaterRoot`, picks a blank field via `looksLikeTargetField()`, and sets `https://host-from-request`. Burp 2026.x **Target** is not that field — the request **Find** box is, so we pollute search instead of toolbar Target.

There are **no** MCP tools `get_repeater_tab_target` / `set_repeater_tab_target` yet; the bug is in **implicit** auto-fill on Send.

---

## Scope

| Item | Status |
|------|--------|
| Send refuses when Target unset | **done** (fail-closed message) |
| Auto-fill Target from `Host:` header | **blocked** — wrong control (search bar) |
| `get_repeater_tab_target` | **todo** — must read real toolbar Target |
| `set_repeater_tab_target` | **todo** — must write real Target; no search/editor fields |
| Left/right inspector rail | Editor discovery must **exclude** inspector/notes widgets from request editor heuristics |

---

## Fix direction (when implementing)

1. **Jar/UI spike (2026.8):** search [`burp-ui-reference.md`](./burp-ui-reference.md) decompile tree for Target / “Not specified” / Repeater toolbar — not generic `JTextField` scan under `repeaterRoot`.
2. **Replace** `collectTargetFields` + `looksLikeTargetField` heuristics with discovered control(s); verify on **east and west** inspector rail positions.
3. **Optional:** Montoya/upstream ask in [`montoya-gaps.md`](./montoya-gaps.md) for tab Target URL API.
4. **Editor discovery:** when picking request editor, exclude components under inspector rail / notes (`RepeaterSwingTree` + rail bounds or `burp.Zs71` subtree).
5. **Live smoke:** Health Check tab — set Target via MCP → Send succeeds → no search bar text.

---

## Ship order

1. Spike + fix **toolbar Target** read/write (or disable `ensureTargetUrl` until fixed — fail-closed only).
2. Fix **request editor** ambiguity with left rail.
3. Ship **`get/set_repeater_tab_target`** MCP tools with restore if UI mutation needed.
4. Re-smoke full flow: send (no target) → set target → send → notes.

---

## Done criteria

- [ ] No URL written to request **search** field during Send or set-target.
- [ ] Toolbar Target matches MCP set value; Send proceeds without Configure-target modal.
- [ ] Ledger rows **`done`** only after live smoke on 2026.8 (rail left + right).
- [ ] Docs [`docs/repeater.md`](../../docs/repeater.md) updated for users.

---

## Related queued

- [`repeater-ui-focus.md`](./repeater-ui-focus.md) — foreground/caret churn during tab select
- East sidebar collapse — [`repeater-east-sidebar.md`](./repeater-east-sidebar.md)
