# In progress & up next

**Purpose:** Handoff board for new chats — what landed in code/docs recently vs what is **not** done.  
**Shipped receipts:** [`../shipped/README.md`](../shipped/README.md) · **Sprint pointer:** [`focus.md`](./focus.md)

**Updated:** 2026-08-31

---

## Active phase (only one “now” for Repeater UI)

| Phase | Doc | Gate |
|-------|-----|------|
| **Repeater east sidebar** (Notes data + rail tabs + collapse/restore) | [`repeater-east-sidebar.md`](./repeater-east-sidebar.md) | Do **not** mark Notes `done` until this phase Step 1–3 pass live smoke |

Everything else in Repeater (groups, target toolbar, gear settings) is **queued behind** this slice unless user reprioritizes.

---

## Landed in tree (commit-ready; verify Notes still blocked)

| Area | What | Notes |
|------|------|--------|
| **Repeater context** | `get_repeater_context` | Live snapshot |
| **Envelope** | `RepeaterToolEnvelope` on Swing Repeater tools | Post-mutation `context` |
| **Close tabs** | `close_repeater_tab`, `close_other_repeater_tabs` | Live-smoked |
| **Notes** | `get/set_repeater_tab_notes` via **Annotations** | **done** (live smoke 2026.8) |
| **East rail** | state + select + visible MCP tools | **done** (select restore smoked; collapse optional) |
| **Debug** | `scan_repeater_notes_ui` | Swing tree scan only (no clipboard) |
| **Docs / systems** | `state-plane.md`, ledger east-sidebar rows, this board | Maintainer only |
| **Jar spike** | Findings summarized in phase doc; extracts under `maintainer/temp/jar-spike/` | No decompile in git |

---

## In progress (implementation)

| Task | Owner | Next action |
|------|-------|-------------|
| Notes **Annotations** binding | — | Spike `Zveg`/`Zemd`-style link from selected tab; read `notes()` / `setNotes()` |
| East sidebar **state snapshot** | — | Design `get_repeater_east_sidebar_state` fields from 2026.8 UI |
| Rail **restore** | — | Extend session pattern from `RepeaterTabSession` |

---

## Soon (same phase, after Notes data path)

| Task | Doc row |
|------|---------|
| `get_repeater_east_sidebar_state` | [`repeater-east-sidebar.md`](./repeater-east-sidebar.md) Step 2 |
| `set_repeater_east_sidebar_visible` + restore | Step 3 |
| `select_repeater_east_sidebar_tab` + restore | Step 3 |
| `append_repeater_tab_notes` | [`tools/repeater.md`](./tools/repeater.md) |

---

## Queued (explicitly not now)

| Item | Doc |
|------|-----|
| Repeater groups | [`tools/repeater.md`](./tools/repeater.md) |
| Target scope list | [`tools/target.md`](./tools/target.md) |
| `get/set_repeater_tab_target` | repeater ledger |
| MCP tab v2/v3 | [`mcp-tab-ui.md`](./mcp-tab-ui.md) |

---

## Chances (honest)

| Goal | Likelihood | Why |
|------|------------|-----|
| **Correct get/set notes** for selected tab | **High** if Annotations/reflection spike succeeds | Data model is documented in Montoya; jar points at `setNotes` bridges |
| **Rail tab switch + restore** | **Medium–high** | Same Swing session patterns as tab strip; need spike on vertical rail controls |
| **Collapse/expand + restore** | **Medium** | UI control discovery; version-sensitive |
| **Explanations / Custom actions content** | **Medium/low** | Depends what Burp exposes per rail tab; may be read-only v1 |

---

## New chat starter prompt (copy)

```text
Read maintainer/phases/focus.md, in-progress.md, repeater-east-sidebar.md.
Implement Step 1 (Annotations notes path) for Burp 2026.8; then Step 2–3 east rail state + restore.
No Repeater registry; ./gradlew test; deploy + live smoke before marking Notes done.
```
