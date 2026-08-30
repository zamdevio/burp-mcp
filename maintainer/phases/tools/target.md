# Target — MCP tool ledger

**Suite tab / UI:** Target → **Scope**, **Site map**, **Issues**.  
**Related:** [`../montoya-gaps.md`](../montoya-gaps.md) · public catalog [`../../../docs/tools.md`](../../../docs/tools.md)

**Path reality:** Suite-wide scope **query/include/exclude** exist on Montoya (`Scope`). The Scope **table** (list rows, Edit, Remove, Enabled, Include subdomains, advanced control) is richer than those three calls — full CRUD may need **Swing** or project options JSON. Site map has Montoya APIs. Issues overlap Scanner (Pro).

---

## Scope — query / mutate (Montoya)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `is_in_scope` | URL in Suite-wide scope? | Montoya | done | |
| `include_in_scope` | Include URL in scope | Montoya | done | May not always add a visible **Prefix** row like the UI Add dialog; verify in Target → Scope |
| `exclude_from_scope` | Exclude URL from scope | Montoya | done | Live smoke left a visible Exclude prefix |

---

## Scope — table / UI parity (Add, Edit, Remove, …)

Matches Target → Scope chrome (Include / Exclude lists).

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `list_scope_rules` | List include + exclude rules (prefix, enabled, subdomains, side) | Swing or Config | todo | Spike first: Montoya vs Swing vs `output_project_options` |
| `add_scope_rule` | Add include/exclude rule with options | Swing or Montoya | todo | Map UI: Enabled, Prefix, Include subdomains |
| `edit_scope_rule` | Edit existing rule by id/index | Swing | todo | |
| `remove_scope_rule` | Remove rule from include or exclude list | Swing | todo | Needed to clean smoke excludes |
| `set_scope_rule_enabled` | Toggle Enabled checkbox | Swing | todo | |
| `set_scope_include_subdomains` | Toggle Include subdomains | Swing | todo | |
| `set_advanced_scope_control` | Toggle “Use advanced scope control” | Swing or Config | todo | Changes table columns |
| `paste_scope_url` | Paste URL into include/exclude (UI action) | Swing | skip | Prefer `add_scope_rule` / include tools |

---

## Site map

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_sitemap_entries` | Paginated site map request/responses | Montoya | todo | Data-access approval like history |
| `get_sitemap_entries_regex` | Filter site map | Montoya | todo | |
| `add_to_sitemap` | `SiteMap.add(...)` | Montoya | todo | Niche |
| `send_sitemap_item_to_repeater` | Open item in Repeater | Montoya/Swing | todo | If API allows |

---

## Issues (Target → Issues / Scanner)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_scanner_issues` | List scanner issues | Montoya | done | **Pro**; also listed under Scanner |
| `get_target_issues` | Issues as shown under Target | Montoya/Swing | todo | May alias scanner issues — spike |
| `export_issues_report` | Generate report | Montoya | blocked | Path policy / FS safety |

---

## Suite info (often used with Target work)

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `get_burp_version` | Burp product / edition / build | Montoya | done | |
| `get_project_info` | Project name + id | Montoya | done | |

---

## Out of scope / skip

| Item | Status | Why |
|------|--------|-----|
| Patch Burp for private Scope model | skip | Hard constraint |
| Silent mass scope wipe | skip | Too destructive without explicit tool + confirm |

---

## Suggested implement order

1. Spike: can we **list** scope rules without Swing? (`list_scope_rules`)
2. `remove_scope_rule` (cleanup + UI parity)
3. `add_scope_rule` / edit / enabled / subdomains (match Scope panel)
4. `get_sitemap_entries` (+ approval)
5. Issues Target-tab parity if distinct from `get_scanner_issues`
