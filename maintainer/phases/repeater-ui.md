# Phase: Repeater tab inspection (Swing)

**Approach:** Montoya where it exists + **stateless** Swing discovery from `suiteFrame()`. **No** Repeater manager, registry, cache, or shadow state.

**Status:** Tab list + read/write + **enforce** + **Send** tools in code (see [`focus.md`](focus.md)). Live smoke after deploy.

---

## Goal

Agents list and interact with **individual Repeater tabs**, not only the focused `JTextArea` (`get/set_active_editor_contents`).

Burp remains the **only** source of truth — re-discover on every tool call.

---

## Montoya facts

| Fact | Detail |
|------|--------|
| Declared Montoya dep | `2025.10` (`gradle/libs.versions.toml`) |
| Burp 2026.x bundled API | Often newer (e.g. `2026.7.x` in shipped JAR) |
| Public `Repeater` | Only `sendToRepeater(...)` |
| `HttpEditor` / `EditorPane` | Unreachable; `EditorPane` is write-only — do not depend on them |
| Tab list/read | **Swing only** — do not patch Burp JAR |

---

## Architecture constraints

1. **No registry** — no `Map<TabId, …>` or persisted tab state.
2. **Stateless discovery** — `RepeaterUiDiscovery` (→ future `ui/` + strategies) inspects live UI each call.
3. Prefer Montoya when available; Swing only where needed.
4. **Fail safe** on ambiguity — never mutate the wrong editor.
5. Keep **`get/set_active_editor_contents`** (keyboard focus semantics).
6. Swing on **EDT**; do not block EDT with MCP/network work.
7. Narrow scope — no arbitrary FS/shell/telemetry.
8. Follow existing security patterns for HTTP/history; Repeater read/write follows active-editor pattern today.

---

## Shipped MCP tools

| Tool | Purpose |
|------|---------|
| `list_repeater_tabs` | Tab metadata (`repeater-tab-N`, name, selected, hasRequest/hasResponse) |
| `get_repeater_tab` | Structured tab + request/response when identifiable |
| `get_repeater_tab_request` / `_response` | Raw message text |
| `set_repeater_tab_request` | Update request editor |
| `get_active_repeater_tab` | Selected Repeater tab (not keyboard focus) |
| `select_repeater_tab` | `JTabbedPane` selection |
| `set_repeater_tab_title` | Tab strip rename |
| `get_repeater_tab_notes` / `set_repeater_tab_notes` | Notes panel read/write |
| `send_repeater_tab` | Toolbar Send (`RepeaterSend`); blocks if Target not specified |
| `send_repeater_tab_and_get_response` | Send + poll response editor |
| `close_repeater_tab` / `close_other_repeater_tabs` | Close tab(s) via strip |

**IDs:** `repeater-tab-{index}` — reorder/close invalidates; re-list before mutate.

User guide: [`docs/repeater.md`](../../docs/repeater.md).

---

## Live Burp behavior (known)

- **Shared message editor** for all Repeater tabs — must **select tab** before read/write; content must **settle** before read.
- Editors are **`JTextComponent`**, not only `JTextArea`; skip single-line target `JTextField`s.
- Tab strip may return **null** `getComponentAt` for chrome (e.g. `+` tab) — handle null safely.
- **`list_repeater_tabs`:** prefer metadata; full body probes only with select+restore when needed.

---

## Enforce

| Step | Behavior |
|------|----------|
| Select | Suite **Repeater** + message tab index |
| Settle | Minimal EDT wait / content-stable heuristic (fast, bounded) — `RepeaterTabSession` |
| Act | Read or write request/response **while selected** |
| Restore | Prior Repeater tab + prior suite tool tab (**default on**) |
| Errors | Stable messages for agents: not found, editor ambiguous, editor did not update, retry hint |

**UX:** Visible tab may flicker briefly; user ends on **restored** selection.

**Code:** `repeater/RepeaterTabSession.kt` + tab-targeted methods in `RepeaterUiDiscovery`.

---

## Optional later

| Tool | Notes |
|------|--------|
| `set_repeater_tab_target` | If target field is uniquely discoverable without dialogs |
| Groups, close, per-tab settings | Full inventory: [`tools/repeater.md`](tools/repeater.md) |

---

## Field updates

| Field | How |
|-------|-----|
| Raw request (method, Host, body) | `set_repeater_tab_request` |
| Tab title | `set_repeater_tab_title` |
| Separate target box | Raw request or future Swing field |
| Response | Read only unless probe proves safe edit path |

---

## Success criteria

- Agent reads/writes a chosen tab without manual focus hacks.
- No persistent tab state in extension.
- `./gradlew test` passes; limitations + workflows in **`docs/repeater.md`** / **`docs/limitations.md`**.
- Ambiguous discovery → explicit error, not wrong mutation.

---

## Out of scope

- Patching/redistributing Burp JAR, decompiling proprietary UI into repo
- Repeater registry
- Runtime discovery rule downloads from GitHub
