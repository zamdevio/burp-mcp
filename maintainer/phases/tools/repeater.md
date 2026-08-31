# Repeater — MCP tool ledger

**Suite tab / UI:** Repeater (message tabs, Send, Target, Notes, groups, per-tab settings).  
**Related:** [`../repeater-east-sidebar.md`](../repeater-east-sidebar.md) (**P0 phase — Notes + rail**) · [`../repeater-ui.md`](../repeater-ui.md) · user guide [`../../../docs/repeater.md`](../../../docs/repeater.md) · systems [`../../systems/repeater-ui.md`](../../systems/repeater-ui.md)

**Path reality:** Montoya can **create** tabs only. List / read / write / Send / Notes / groups / close / settings → **Swing** (re-discover every call; select → settle → act → restore).

**Responses:** Repeater Swing tools return JSON **`RepeaterToolEnvelope`** (`ok`, `message`/`error`, live **`context`**, optional **`data`**). Cross-cutting rules: [`../../systems/suite-ui-session.md`](../../systems/suite-ui-session.md).

**List responses:** Prefer returning **group id**, **tab id**, **parent group**, and **strip index** once group tools exist (today: flat `repeater-tab-N` only).

---

## Context (Swing)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_repeater_context` | Live snapshot (tabs, selection, suite focus) | Swing | done | Not cached; same `context` on every Repeater tool |

---

## Create (Montoya)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `create_repeater_tab` | New HTTP/1.1 tab + request | Montoya | done | Sets target via API |
| `create_repeater_tab_http2` | New HTTP/2 tab | Montoya | done | Prefer for modern HTTPS |

---

## Tab strip — inspect / edit / send (Swing)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `list_repeater_tabs` | List tabs (+ groups when implemented) | Swing | done | Extend for group tree |
| `get_repeater_tab` | Structured tab + messages | Swing | done | |
| `get_repeater_tab_request` | Raw request editor | Swing | done | |
| `get_repeater_tab_response` | Raw response editor | Swing | done | |
| `set_repeater_tab_request` | Replace request | Swing | done | |
| `get_active_repeater_tab` | Selected strip tab | Swing | done | Not keyboard focus elsewhere |
| `select_repeater_tab` | Select strip tab | Swing | done | Leaves selection |
| `set_repeater_tab_title` | Rename tab | Swing | done | Same as context menu “Rename tab” |
| `send_repeater_tab` | Click Send | Swing | done | Blocks if Target not specified |
| `send_repeater_tab_and_get_response` | Send + wait for HTTP | Swing | done | |

---

## Tab lifecycle — close / reopen (context menu)

Maps Burp tab right-click menu (version-sensitive).

| MCP tool | UI action | Path | Status | Notes |
|----------|-----------|------|--------|-------|
| `close_repeater_tab` | Close tab | Swing | done | Last message tab protected |
| `close_other_repeater_tabs` | Close other tabs | Swing | done | Maps context menu |
| `close_repeater_tabs_to_left` | Close tabs to the left | Swing | todo | Relative to tab id |
| `close_repeater_tabs_to_right` | Close tabs to the right | Swing | todo | |
| `reopen_last_closed_repeater_tab` | Reopen closed tab | Swing | todo | If Burp exposes stack; else blocked |

---

## Groups — full group ↔ tab model

Maps **Edit group** dialog: name, color, membership checkboxes, folder on strip.

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `list_repeater_groups` | Groups + member tab ids + color | Swing | todo | Spike Burp 2024/2026 chrome |
| `get_repeater_group` | One group metadata + members | Swing | todo | |
| `create_repeater_group` | New empty group (name, optional color) | Swing | todo | |
| `edit_repeater_group` | Rename, recolor, set member tab set | Swing | todo | Full “Edit group” parity |
| `delete_repeater_group` | Remove group (tabs ungroup or close — spike UI) | Swing | todo | Document destructive behavior |
| `add_repeater_tab_to_group` | Add tab(s) to group | Swing | todo | Context: “Add tab to group” |
| `remove_repeater_tab_from_group` | Move tab out of group (ungroup) | Swing | todo | |
| `move_repeater_tab_to_group` | Alias / single-tab add | Swing | todo | May merge with `add_*` |
| `reorder_repeater_tabs` | Reorder within strip or inside group | Swing | todo | |
| `close_repeater_group` | Close all tabs in group | Swing | todo | Destructive — explicit errors |
| `expand_repeater_group` | Expand group folder | Swing | todo | If separate from collapse |
| `collapse_repeater_group` | Collapse group folder | Swing | todo | |

**Pushback:** `list_repeater_tabs` should gain optional `include_groups: true` or a dedicated list that returns a **tree** (groups + loose tabs) to avoid agents guessing membership.

**Ids:** `repeater-tab-N` is strip index — shifts on close/reorder/manual edits. Mutating tools should return **remaining count** (close today); later optional embedded tab list JSON. No extension-side watch/registry (Burp UI stays source of truth).

---

## Per-tab settings (gear menu)

**Repeater settings for this tab** — not Montoya; Swing or config spike.

| MCP tool | UI setting (indicative) | Path | Status | Notes |
|----------|-------------------------|------|--------|-------|
| `get_repeater_tab_settings` | Read all per-tab toggles | Swing | todo | Burp-version matrix in tool desc |
| `set_repeater_tab_settings` | Patch toggles (partial update) | Swing | todo | |
| `restore_repeater_tab_settings_defaults` | “Restore global defaults” | Swing | todo | |

**Settings to map when spiking (2026.x):**

| Key (proposed) | UI label |
|----------------|----------|
| `update_content_length` | Update Content-Length |
| `unpack_compressed_responses` | Unpack compressed responses |
| `follow_redirections` | Follow redirections (enum: never / on-site / in-scope / always) |
| `process_cookies_in_redirections` | Process cookies in redirections |
| `enforce_protocol_cross_domain` | Enforce protocol choice on cross-domain redirections |
| `normalize_http1_line_endings` | Normalize HTTP/1 line endings |
| `http1_connection_reuse` | Enable HTTP/1 connection reuse |
| `http2_connection_reuse` | Enable HTTP/2 connection reuse |
| `strip_connection_header_http2` | Strip Connection header over HTTP/2 |
| `http2_alpn_override` | Allow HTTP/2 ALPN override |
| `streaming_response_timeout` | Streaming response timeout (enum + custom) |

---

## Tab view settings (context submenu)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_repeater_tab_view_settings` | Submenu options for tab display | Swing | todo | Spike what Burp exposes |
| `set_repeater_tab_view_settings` | Apply view options | Swing | todo | |

---

## Notes (agent → human handoff)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_repeater_tab_notes` | Read Notes panel | Annotations | done | Per-tab `HttpRequestResponse.annotations()` |
| `set_repeater_tab_notes` | Replace Notes | Annotations | done | Fail-closed verify on model |
| `scan_repeater_notes_ui` | Notes UI spike / needle | Swing | done | Debug only; not production notes path |
| `append_repeater_tab_notes` | Append to Notes | Swing | todo | Optional |

---

## East sidebar (Notes rail)

**Phase:** [`../repeater-east-sidebar.md`](../repeater-east-sidebar.md) — ship **with** Notes data path; restore rules in [`../../systems/state-plane.md`](../../systems/state-plane.md).

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_repeater_east_sidebar_state` | Read rail visible + selected tab | Swing | done | Step 2 |
| `set_repeater_east_sidebar_visible` | Collapse / expand east rail | Swing | blocked | Live smoke: collapse verify fails 2026.8 — spike control |
| `select_repeater_east_sidebar_tab` | Notes / Explanations / Custom act… | Swing | done | **Restore** prior sidebar tab after tool |

---

## Target URL (toolbar)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| *(gate inside Send)* | Refuse Send if Target not specified | Swing | done | |
| `get_repeater_tab_target` | Read toolbar Target | Swing | todo | |
| `set_repeater_tab_target` | Set Target URL | Swing | todo | Avoid Configure-target modal |

---

## Cookies / auth (suite-wide — not Repeater-only)

Repeater has **no built-in shared `{{cookie}}` env** per tab. MCP backlog elsewhere:

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_cookie_jar_entries` | List jar cookies | Montoya | todo | See [`../montoya-gaps.md`](../montoya-gaps.md) |
| `set_cookie_jar_entry` | Add/update cookie | Montoya | todo | Shared across tools |
| `clear_cookie_jar` | Clear jar | Montoya | todo | Careful |

**Human workflow today:** login tab → jar or Session handling rule → other tabs send `Cookie` via jar, not body.

---

## Focused editors (any message editor)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_active_editor_contents` | Focused editor text | Swing | done | |
| `set_active_editor_contents` | Set focused editor | Swing | done | |

---

## Out of scope / skip (for now)

| Item | Status | Why |
|------|--------|-----|
| Persistent tab registry in extension | skip | Burp UI is source of truth |
| Patch Burp JAR for private Repeater APIs | skip | Hard constraint |
| Ctrl+Enter as Send substitute | skip | No-op on many Burp builds |
| Third-party BApp install via MCP | skip | User installs manually |

---

## Suggested implement order

1. **East sidebar phase** — Notes via **`Annotations`** (or approved reflection) → `get_repeater_east_sidebar_state` → rail visible/tab tools with **restore** → then mark get/set notes **`done`**
2. ~~**`close_repeater_tab`** + **`close_other_repeater_tabs`**~~
3. **`list_repeater_groups`** + **`create_repeater_group`** + **`edit_repeater_group`** + **`add/remove` tab**
4. **`close_repeater_group`**, reorder, expand/collapse (message tab **groups**, not east rail)
5. **`get/set_repeater_tab_target`**
6. **`get/set_repeater_tab_settings`** (gear menu)
7. Tab view settings, reopen closed tab (if feasible)
8. Cookie jar Montoya tools (suite-wide)

---

## Third-party cookie / env helpers (manual install)

Not part of burp-mcp; documented so hunters do not wait on us for Postman-style vars.

| Source | Name | Fit for shared `auth.t` / tokens |
|--------|------|----------------------------------|
| **Built-in** | Project **Cookie jar** + **Session handling rules** | Best default; no extension |
| [BApp](https://portswigger.net/bappstore) | **Authentication Token Obtain and Replace** | Auto refresh/replace tokens in cookies/body |
| BApp | **Add Custom Header** | JWT/header from session rules |
| BApp | **Sticky Burp, Reusable and Replaceable Environment Variables** | Sticky env-style substitution |
| BApp | **Postman Burp Importer** | Postman collection + **environments** → Repeater |
| User extension | **Variables** suite tab | `{{name}}` and `{{secrets:name}}` in requests; values in extension UI (names may contain `:`) |
| BApp | **Session Handler+** | Extra session rule control |
| BApp | **AutoRepeater** | Repeat with replacements (not jar-centric) |

No mainstream BApp is “Repeater-only shared auth.t variable” — use **jar + session rules** or **Sticky Burp** / **Postman importer** if you want named variables.
