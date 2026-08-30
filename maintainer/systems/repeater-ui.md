# Repeater UI discovery

**Code:** `repeater/RepeaterUiDiscovery.kt`, models in `repeater/RepeaterTabModels.kt`, EDT helper `SwingEdt.kt`.

**Plan:** [`phases/repeater-ui.md`](../phases/repeater-ui.md)

## Flow

1. `suiteFrame()` → find suite tab titled **Repeater** → find inner tab strip.
2. Tab ids: `repeater-tab-{index}`.
3. Select tab → find **`JTextComponent`** editors (editable ≈ request, read-only ≈ response) in tab content or shared **repeater root**.
4. Fail safe if ambiguous.

## Enforce (target)

All tab-targeted operations: **select → settle → act → restore** with agent-readable errors.

## Not used

- Montoya `HttpEditor` / `EditorPane` (unreachable / write-only API).
- Persistent tab maps.
