# Repeater UI discovery

**Code:** `repeater/RepeaterUiDiscovery.kt`, `repeater/RepeaterTabSession.kt` (select/settle/restore), `repeater/RepeaterSend.kt` (Send button + wait), models in `repeater/RepeaterTabModels.kt`, EDT helper `SwingEdt.kt`.

**Plan:** [`phases/repeater-ui.md`](../phases/repeater-ui.md)

## Flow

1. `suiteFrame()` → find suite tab titled **Repeater** → find inner tab strip.
2. Tab ids: `repeater-tab-{index}`.
3. Tab-targeted get/set/send: **select → settle → act → restore** via `RepeaterTabSession` / `RepeaterSend`.
4. Editors are **`JTextComponent`** (editable ≈ request, read-only ≈ response) in tab content or shared **repeater root**.
5. Send: unique `AbstractButton` text/tooltip **Send** only (not “Send to Intruder”).
6. **Target gate:** toolbar **Target: Not specified** blocks Send (Host header alone is insufficient); dismiss Configure-target dialog if present.
7. Fail safe if ambiguous; editor-did-not-update / Send missing / target missing / response timeout errors for agents.

## Enforce

All tab-targeted read/write/send operations use select/settle/act/restore (restore default on). `select_repeater_tab` intentionally leaves selection.

## Not used

- Montoya `HttpEditor` / `EditorPane` (unreachable / write-only API).
- Persistent tab maps.
- Keyboard shortcuts into arbitrary focus as a Send substitute.
