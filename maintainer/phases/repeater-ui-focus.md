# Phase — Repeater UI focus & caret churn (background-friendly ops)

**Status:** **Queued** — design + spikes; **no implementation yet.**  
**Not end-user docs.** User-facing behavior belongs in [`docs/repeater.md`](../../docs/repeater.md) / [`docs/limitations.md`](../../docs/limitations.md) once options ship.

**Triggers:** User reports while working in Cursor (or any IDE): Burp window jumps to foreground; message tab strip changes; **caret (I-beam) flickers** rapidly during MCP Repeater tools — especially notes (session select) and **request body** edits.

**Related:** [`repeater-ui.md`](./repeater-ui.md) · [`../systems/suite-ui-session.md`](../systems/suite-ui-session.md) · [`../systems/state-plane.md`](../systems/state-plane.md) · ledger [`tools/repeater.md`](./tools/repeater.md)

---

## Problem (what the user feels)

| Symptom | Likely cause |
|---------|----------------|
| Focus leaves IDE → Burp | Suite/message **tab selection** on Swing EDT; Windows activates the Burp process |
| Tab strip jumps | `RepeaterTabSession.withTab`: select → act → **restore** (intentional for shared editor) |
| I-beam ↔ arrow flicker | **Settle** polls request/response editors; focus moves between text components |
| Worse on request writes | Must mutate **shared** `JTextComponent` — hard to avoid editor participation |
| Notes after Annotations | Data path is off-widget; **tab select + settle** for session/envelope still runs |

This is **not** the old clipboard/Robot path (removed). Remaining churn is **session + shared-editor** architecture on a **live GUI** Burp.

---

## Can we prevent it fully?

**Short answer: not for all Repeater Swing tools on a normal Burp Pro desktop session.** We can **reduce a lot** for some tools; others need **Montoya or internal APIs** that do not exist publicly today.

| Goal | Realistic? | Why |
|------|------------|-----|
| **Zero focus steal ever** (all tools, Windows, user typing in IDE) | **No** (with current public APIs + shared editor) | Tab-targeted ops require Burp to select tabs; AWT/Swing often raises the app; Send/clicks need real UI targets |
| **Near-zero for notes read/write** | **Maybe** | Already use **Annotations** (`RepeaterTabAnnotations`); spike skipping strip select when bundle is reachable without selection; skip or shorten **settle** when not touching editors |
| **Near-zero for read-only context/list** | **Partial** | `get_repeater_context` / `list_repeater_tabs` still call `ensureRepeater` today → suite tab focus |
| **Low churn for `set_repeater_tab_request`** | **Hard** | Needs editor mutation or undocumented model write; Montoya has no “set this tab’s request bytes” without UI |
| **User workflow workaround** | **Yes** | Burp on another virtual desktop; don’t run agent while actively typing (operational, not product) |

**Product stance to document:** MCP Repeater tools are **UI-coupled** by design until Burp exposes **headless tab/request/notes** APIs. Phase goal is **minimize** foreground/caret disruption, not promise invisibility.

---

## Mechanism (code map today)

```text
ensureRepeaterSuiteSelected()     → suite JTabbedPane → Repeater
RepeaterTabSession.withTab()      → strip selectedIndex = N
RepeaterTabSession.settleEditors()  → poll request/response fingerprints (EDT sleep)
… mutate (editor, annotations, rail, Send) …
finally restore(strip + suite snapshot)
```

| Layer | Files |
|-------|--------|
| Suite tab | `ui/SuiteUiSession.kt`, `RepeaterUiDiscovery.prepareRepeater` |
| Message tab session | `repeater/RepeaterTabSession.kt` |
| Notes (data) | `repeater/RepeaterTabNotes.kt`, `RepeaterTabAnnotations.kt` |
| Request write | `RepeaterUiDiscovery` write path + shared editor |
| East rail | `repeater/RepeaterEastSidebar.kt` (EDT tab clicks) |

---

## Mitigation directions (implementation backlog)

Priority after east-sidebar **collapse** fix (or parallel if user reprioritizes).

| # | Idea | Affects | Risk / tradeoff |
|---|------|---------|------------------|
| 1 | **Config: `repeaterBackgroundMode`** (Burp MCP tab) | All Repeater Swing tools | Skip `ensureRepeater` when already on Repeater; avoid `requestFocus` calls if any; **shared editor wrong-tab writes** if user changes tab mid-flight |
| 2 | **Notes: no strip select** when `Zemd` found without selecting | get/set notes | Must verify tab index binding without selection; fail-closed if ambiguous |
| 3 | **Shorter / no settle** when tool does not read editor text | notes, context-only | Race on shared editor tools if misclassified |
| 4 | **Peek-only reads** where possible | list tabs, context | May still need one suite select unless reflection on tab list exists |
| 5 | **Montoya upstream** | request, notes, send | Document in [`montoya-gaps.md`](./montoya-gaps.md): tab CRUD + body + annotations without focus |
| 6 | **Send / click** | send_repeater_* | Unlikely to be silent without API; document as “foreground tool” |

---

## Ship order (when phase starts)

1. **Measure** — log timestamps: suite select, strip select, settle ms, restore (Burp MCP output or debug flag).
2. **Spike** — notes get/set with strip select skipped on 2026.8; live-smoke + **no wrong-tab data**.
3. **Config + docs** — background mode default **off**; limitations doc explains IDE focus.
4. **Request path** — only after Montoya/reflection spike; do not pretend full fix.

---

## Done criteria (phase)

- [ ] Documented **honest** guarantee tier per tool group (read / notes / write / send).
- [ ] Optional background mode **reduces** focus steals in live-smoke with user on IDE (subjective + log).
- [ ] No regression: wrong-tab mutations still **fail closed** when ambiguous.
- [ ] `montoya-gaps.md` updated with focus-sparing API asks.

---

## Explicit non-goals

- Patching Burp JAR or injecting focus hacks into PortSwigger code.
- Disabling **restore** by default (agents rely on user tab state).
- Promising zero UI motion on **Send** or **request editor** writes without new APIs.
