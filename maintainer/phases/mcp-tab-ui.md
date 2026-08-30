# Phase: MCP suite tab UI

**Status:** Planned — after Repeater enforce + Send land, or in parallel if UI-only.

**Code today:** `config/ConfigUi.kt` — left column hero (“Burp MCP Server” + MCP link), right scroll column: server toggles → auto-approve → advanced host/port → install/extract proxy.

---

## Problem

Operators and agents discover capabilities from GitHub/docs or by `list_tools` in the client. The Burp **MCP** tab only shows server/security/install — not **what** the extension exposes or **how** tools behave (Pro gates, approval, Repeater vs editor).

---

## Goals

1. **In-tab capability catalog** — readable list of registered MCP tools grouped by area (HTTP, Repeater, Proxy, …).
2. **Descriptions aligned with MCP** — same strings (or shared metadata) used at registration time so docs, UI, and `list_tools` never drift.
3. **Capability notes** — short badges: `Pro`, `Approval`, `Config edit`, `Swing UI`, `Mutating`.
4. **Connection hint** — prominent `http://127.0.0.1:{port}` + link to [`docs/guides/`](../docs/guides/) for client setup.
5. **No installer bloat** — version/strategy reminder only if needed; **no** runtime download of UI rules from GitHub.

---

## UX direction (proposal)

Keep the existing two-column layout; evolve **left column** from static hero to **tabbed or stacked**:

| Pane | Content |
|------|---------|
| **Settings** *(current right column)* | Unchanged flow: enable, permissions, auto-approve, advanced, install |
| **Tools & capabilities** *(new)* | Search/filter list; expand row → description + notes; copy tool name |

Alternative if space is tight: **sub-tabs** at top of right column — `Server` | `Tools` | `Install`.

Use existing `Design.kt` tokens; EDT-only updates; no blocking on tool enumeration.

---

## Implementation sketch

1. **Tool metadata registry** — extract name + description + tags from registration (`tools/McpTool.kt` / domain modules) into a read-only `ToolCatalog` consumed by Ktor **and** Swing.
2. **`ToolsPanel.kt`** (new under `config/components/`) — `JTable` or card list; optional search field.
3. **Pro / edition** — hide or gray Pro-only rows on Community (mirror runtime tool registration).
4. **Tests** — lightweight test that catalog size matches registered tool count (no full Swing UI test required initially).

---

## Out of scope (this phase)

- Editing tool enablement per tool (global gates stay in MCP tab checkboxes).
- Embedding full markdown docs — link out to `docs/tools.md` / future `docs/tools/*.md`.
- Replacing PortSwigger branding in suite tab title (optional rename to “burp-mcp” is product decision).

---

## Done when

- [ ] User can scroll/search all exposed tools and read descriptions without leaving Burp.
- [ ] Catalog source is single shared metadata (not duplicated strings in Swing).
- [ ] README/docs point to this as shipped; receipt in `shipped/`.

---

## Related

- [`agent-guides.md`](agent-guides.md) — client setup docs linked from install panel.
- [`refactor.md`](refactor.md) — split `Tools.kt` makes catalog extraction easier.
