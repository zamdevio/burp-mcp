# Repeater UI discovery

**Code:** `repeater/RepeaterUiDiscovery.kt`, `repeater/RepeaterTabSession.kt` (select/settle/restore), models in `repeater/RepeaterTabModels.kt`, EDT helper `SwingEdt.kt`.

**Plan:** [`phases/repeater-ui.md`](../phases/repeater-ui.md)

## Flow

1. `suiteFrame()` → find suite tab titled **Repeater** → find inner tab strip.
2. Tab ids: `repeater-tab-{index}`.
3. Tab-targeted get/set: **select → settle → act → restore** via `RepeaterTabSession`.
4. Editors are **`JTextComponent`** (editable ≈ request, read-only ≈ response) in tab content or shared **repeater root**.
5. Fail safe if ambiguous; `<Repeater editor did not update; …>` if a write does not stick.

## Enforce

All tab-targeted read/write operations use select/settle/act/restore (restore default on). `select_repeater_tab` intentionally leaves selection.

## Not used

- Montoya `HttpEditor` / `EditorPane` (unreachable / write-only API).
- Persistent tab maps.
