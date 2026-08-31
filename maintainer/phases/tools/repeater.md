# Repeater — MCP tool ledger

**Suite tab / UI:** Repeater (message tabs, Send, Target, Notes, groups).  
**Related:** [`../repeater-ui.md`](../repeater-ui.md) · user guide [`../../../docs/repeater.md`](../../../docs/repeater.md) · systems [`../../systems/repeater-ui.md`](../../systems/repeater-ui.md)

**Path reality:** Montoya can **create** tabs only. List / read / write / Send / Notes / groups → **Swing** (re-discover every call; select → settle → act → restore).

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
| `list_repeater_tabs` | List ids/names/flags | Swing | done | Ids `repeater-tab-N`; re-list after reorder/close |
| `get_repeater_tab` | Structured tab + messages | Swing | done | |
| `get_repeater_tab_request` | Raw request editor | Swing | done | |
| `get_repeater_tab_response` | Raw response editor | Swing | done | |
| `set_repeater_tab_request` | Replace request | Swing | done | |
| `get_active_repeater_tab` | Selected strip tab | Swing | done | Not keyboard focus elsewhere |
| `select_repeater_tab` | Select strip tab | Swing | done | Leaves selection |
| `set_repeater_tab_title` | Rename tab | Swing | done | |
| `send_repeater_tab` | Click Send | Swing | done | Blocks if Target not specified |
| `send_repeater_tab_and_get_response` | Send + wait for HTTP | Swing | done | |

---

## Focused editors (any message editor)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_active_editor_contents` | Focused editor text | Swing | done | Not tab-id based |
| `set_active_editor_contents` | Set focused editor | Swing | done | Must be editable |

---

## Notes (agent → human handoff)

Goal: after an agentic Repeater flow, leave readable notes for the security engineer.

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_repeater_tab_notes` | Read Notes panel for a tab | Swing | done | Select/settle/restore |
| `set_repeater_tab_notes` | Write/replace Notes for a tab | Swing | done | Fail safe if ambiguous |
| `append_repeater_tab_notes` | Append to Notes | Swing | todo | Optional nicety |

---

## Groups / tab organization

Burp Repeater supports grouping tabs. Agents need group-aware list + mutate for tidy projects.

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `list_repeater_groups` | List groups + member tab ids | Swing | todo | Version-sensitive chrome |
| `create_repeater_group` | New empty group | Swing | todo | |
| `rename_repeater_group` | Rename group | Swing | todo | |
| `move_repeater_tab_to_group` | Move tab into group | Swing | todo | |
| `ungroup_repeater_tab` | Remove tab from group | Swing | todo | |
| `reorder_repeater_tabs` | Reorder within strip/group | Swing | todo | |
| `close_repeater_tab` | Close one tab | Swing | todo | Confirm fail-safe |
| `close_repeater_group` | Close all tabs in a group | Swing | todo | Destructive — clear agent errors |
| `expand_collapse_repeater_group` | Expand/collapse group UI | Swing | todo | If discoverable |

---

## Target URL (toolbar)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| *(gate inside Send)* | Refuse Send if Target not specified | Swing | done | Host header alone insufficient |
| `get_repeater_tab_target` | Read toolbar Target display | Swing | todo | |
| `set_repeater_tab_target` | Set Target URL for a tab | Swing | todo | Prefer over dialogs; see include/scope lessons |

---

## Out of scope / skip (for now)

| Item | Status | Why |
|------|--------|-----|
| Persistent tab registry | skip | Burp UI is source of truth |
| Patch Burp JAR for private Repeater APIs | skip | Hard constraint |
| Ctrl+Enter as Send substitute | skip | No-op on many Burp builds |

---

## Suggested implement order

1. ~~Notes get/set~~
2. Close single tab
3. Groups list + move/rename
4. Close group / reorder
5. Explicit get/set Target tools
