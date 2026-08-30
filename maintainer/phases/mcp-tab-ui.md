# Phase: MCP suite tab UI

**Status:** **v1 shipped** (Tools catalog). UI polish later. v2/v3 planned — do not build yet.

**Code today:** `config/ConfigUi.kt` — left hero; right **Server | Tools** tabs. Server: toggles → auto-approve → advanced → install. Tools: [`ToolsPanel`](../../src/main/kotlin/net/portswigger/mcp/config/components/ToolsPanel.kt) + [`ToolCatalog`](../../src/main/kotlin/net/portswigger/mcp/tools/ToolCatalog.kt).

---

## Problem

Operators and agents discover capabilities from GitHub/docs or by `list_tools` in the client. The Burp **MCP** tab historically only showed server/security/install — not **what** the extension exposes.

---

## Waves

### v1 — Capability catalog (this sprint)

1. **In-tab capability catalog** — searchable list grouped by area (HTTP, Repeater, Proxy, …).
2. **Descriptions aligned with MCP** — `ToolCatalog.record` from `mcpTool` registration; built-in seed for UI before server start.
3. **Capability notes** — badges: `Pro`, `Approval`, `Config edit`, `Swing UI`, `Mutating`.
4. **Connection hint** — SSE URL from config host/port + link to docs/guides on GitHub.
5. **Edition** — hide Pro-tagged tools on Community.
6. **No installer bloat** — no runtime download of UI rules.

### v2 — Per-tool enable (later — do not build yet)

- Persist allowlist/denylist in `McpConfig`.
- Gate `addTool` / call path **server-side** (not UI-only).
- UI toggles per row; default **all on**.
- Never bypass global HTTP / history / config-edit gates.
- Safe deny messages for agents when a tool is disabled.

### v3 — Docs in Burp (later — do not build yet)

- Detail pane renders markdown for the selected tool (bundled short pages from `docs/` or embedded snippets).
- Start small (tool blurbs), not a full VitePress shell inside Swing.
- Keep deep links to GitHub / future VitePress site for long guides.

---

## UX (v1)

| Tab | Content |
|-----|---------|
| **Server** | Existing: enable, permissions, auto-approve, advanced, install |
| **Tools** | Search + list; description + badges; copy tool name; guides link |

---

## Done when (v1)

- [x] User can scroll/search exposed tools and read descriptions in Burp.
- [x] Catalog recorded at registration + built-in seed for offline UI.
- [x] README/docs point to Tools tab as shipped; receipt in `shipped/` (with this sprint).

---

## Related

- [`agent-guides.md`](agent-guides.md) — client setup docs linked from Tools pane.
- [`refactor.md`](refactor.md) — split `Tools.kt` still helpful; not required for v1 seed.
- [`vitepress.md`](vitepress.md) — public site; v3 may deep-link there later.
