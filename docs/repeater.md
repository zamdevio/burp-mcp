# Repeater (MCP)

Agents drive Burp **Repeater** through MCP: create tabs (Montoya), then list / read / write / send via **live Swing discovery** (no tab registry).

Use only on systems you are authorized to test. Full catalog: [`tools.md`](tools.md). Constraints: [`limitations.md`](limitations.md).

---

## Mental model

| Idea | Detail |
|------|--------|
| Source of truth | Burp’s UI — re-discover on every call |
| Tab ids | `repeater-tab-N` (strip index). **Re-list** after close/reorder |
| Tool responses | JSON **`RepeaterToolEnvelope`**: `ok`, `message`/`error`, live **`context`**, optional **`data`**. After mutations, **`context` in that same response is authoritative** (selected tab, tab list). |
| Context only | `get_repeater_context` — same snapshot shape, no side effects |
| Shared editors | Many Burp builds share one request/response editor — tools **select → settle → act → restore** |
| Target URL | Toolbar **Target** (pencil). Distinct from the `Host:` header in the raw request |
| Focused editor tools | `get/set_active_editor_contents` = keyboard focus anywhere — **not** the same as tab-targeted tools |

---

## Create tabs (Montoya)

| Tool | Notes |
|------|--------|
| `create_repeater_tab` | HTTP/1.1 raw request + optional name |
| `create_repeater_tab_http2` | Prefer for modern HTTPS targets |

Creating a tab through Montoya typically **sets Target**. Pasting a request into an empty / misconfigured tab can leave **Target: Not specified**.

---

## Inspect and edit (Swing)

| Tool | Notes |
|------|--------|
| `get_repeater_context` | Live snapshot (tabs, selection, suite focus) — not cached |
| `list_repeater_tabs` | id, name, selected, hasRequest / hasResponse (in envelope `data`) |
| `get_repeater_tab` | Structured detail + request/response when unique |
| `get_repeater_tab_request` / `get_repeater_tab_response` | Raw editor text |
| `set_repeater_tab_request` | Replace request editor (`request` + `tabId`) |
| `get_active_repeater_tab` | Strip selection (suite Repeater selected) |
| `select_repeater_tab` | Optional — select strip tab; other tools auto-select by `tab_id` |
| `set_repeater_tab_title` | Rename |
| `get_repeater_tab_notes` / `set_repeater_tab_notes` | Per-tab notes via Burp’s annotations model (select → settle → restore) |
| `get_repeater_east_sidebar_state` | East rail visibility + selected rail tab |
| `set_repeater_east_sidebar_visible` / `select_repeater_east_sidebar_tab` | Mutate rail; restores prior rail state after |
| `close_repeater_tab` / `close_other_repeater_tabs` | Close one tab or all except one |

Tab-targeted get/set briefly switch selection, then **restore** the prior suite/Repeater tab. A short flicker is expected.

---

## Notes

| Tool | Notes |
|------|--------|
| `get_repeater_tab_notes` | Plain text from the tab’s stored notes (Montoya `Annotations`); empty if none yet |
| `set_repeater_tab_notes` | Replace notes for agent → human handoff |

Notes are **not** read from the rich-text Swing widget (2026.x HTML pane); data comes from the same model Burp uses for Proxy/history annotations. Fail-closed if the tab bundle cannot be bound.

## East inspector rail

| Tool | Notes |
|------|--------|
| `get_repeater_east_sidebar_state` | `visible`, `selectedRailTab`, `railTabs` (Inspector, Notes, Explanations, …) |
| `select_repeater_east_sidebar_tab` | e.g. `Notes` or `Explanations`; restores prior rail tab |
| `set_repeater_east_sidebar_visible` | Expand/collapse rail; restores prior visibility |

---

## Send

| Tool | Notes |
|------|--------|
| `send_repeater_tab` | Click toolbar **Send** (select → settle → target check → click → restore). Does **not** wait for a body |
| `send_repeater_tab_and_get_response` | Same, then poll until the response editor shows HTTP (or `timeoutMs`, default 30s, clamp 1s–120s) |

On Burp 2024/2026.x, **Ctrl+Enter is often a no-op** — the extension clicks the real Send control.

### Target must be set

If the toolbar shows **Target: Not specified**, Send is **blocked** with an agent-readable error. A `Host:` header in the raw request is **not** enough; Burp would otherwise open **Configure target details**, which stalls automation and leaves empty responses.

**Fix:** set Target via the pencil in Repeater, or recreate the tab with `create_repeater_tab` / `create_repeater_tab_http2`.

If a Configure-target dialog is already open (or somehow appears), the extension dismisses it and returns the same target-missing error.

### Typical agent loop

1. `list_repeater_tabs`
2. `get_repeater_tab` / `get_repeater_tab_request` (confirm body + that Target is set)
3. Optional `set_repeater_tab_request` (e.g. Cookie / path)
4. `send_repeater_tab_and_get_response`
5. Re-list if you closed/reordered tabs

---

## Errors (stable prefixes)

Messages are wrapped for agents (examples):

- `<Repeater target is not specified; …>`
- `<Repeater tab not found: …>` / `<Invalid Repeater tab id: …>`
- `<Repeater Send button not found…>` / disabled
- `<Repeater response timed out after …ms; …>`
- `<Repeater editor did not update; …>`
- `<Repeater Notes editor not found; …>` / `<Unable to uniquely identify Repeater Notes editor>`
- `<Unable to uniquely identify Repeater … editor>`

Prefer fixing Target / re-listing over retrying Send blindly.

---

## What we will not do

- Shadow Repeater registries as source of truth
- Patch Burp Suite’s JAR for private APIs
- Download UI discovery rules at runtime

Maintainer design: [`maintainer/phases/repeater-ui.md`](../maintainer/phases/repeater-ui.md) · systems map: [`maintainer/systems/repeater-ui.md`](../maintainer/systems/repeater-ui.md).
