# Limitations

Honest constraints for operators and agents. Prefer failing clearly over mutating the wrong editor.

## Burp edition

| Capability | Community | Professional |
|------------|-----------|--------------|
| MCP server, HTTP send, Repeater/Intruder send, proxy history, utilities | Yes | Yes |
| Scanner issue tools | No | Yes |
| Collaborator payload / interactions | No | Yes |

## Repeater / Swing UI

- Tab ids are **index-based** (`repeater-tab-0`, …). Re-list after close/reorder.
- Discovery walks Burp’s Swing tree. Labels, nesting, and shared editors can differ by **Burp version** and look-and-feel.
- Burp often uses a **shared** request/response editor for Repeater. Tab-targeted get/set **selects** the tab, waits for the editor to settle, acts, then **restores** the prior suite/Repeater selection. A brief selection flicker is expected.
- Toolbar **Target** must be set before Send. **Target: Not specified** blocks `send_repeater_tab*` even if the raw request has a `Host:` header — otherwise Burp opens Configure target details. See [`repeater.md`](repeater.md).
- Suite tab **Repeater** must be usable (extension loaded, UI available). If Repeater is not the active suite context, some “active tab” queries return empty / no-active messages.
- Changing host/path/method is done by editing the **raw request** text unless a dedicated target tool is added later.
- Do not assume pixel-perfect or headless Burp — this path needs the real desktop UI.

## Agent behavior

- Always **re-discover** tabs; do not cache tab ids across long sessions without re-listing.
- Prefer tool errors that name the failure (ambiguous UI, missing editor) over inventing content.
- Config write tools require the MCP tab toggle for config editing — leave it off unless intentional.
- HTTP send and history access may require user approval depending on extension settings.

## What this project will not do

- Patch or reverse-engineer Burp Suite’s proprietary JAR for private APIs.
- Download UI “skin packs” or discovery rules from the internet at runtime.
- Maintain a shadow Repeater registry as source of truth.

Planned next (MCP tab catalog, agent guides, more Montoya coverage) is tracked under [`maintainer/phases/`](../maintainer/phases/) — not promised by this page until shipped and listed in [`maintainer/shipped/`](../maintainer/shipped/).
