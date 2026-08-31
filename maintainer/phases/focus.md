# Focus — current sprint

**Updated:** 2026-08-31

---

## Now

1. **Repeater east sidebar phase** — Notes data (Annotations path) + rail tab select + collapse/expand, all with **restore**. **Ship before** marking Notes `done`.  
   → [`repeater-east-sidebar.md`](./repeater-east-sidebar.md) · ledger [`tools/repeater.md`](./tools/repeater.md) · board [`in-progress.md`](./in-progress.md)

2. **Live-smoke** any change to Notes / east rail / Repeater envelope tools on Burp 2026.x.

## Queued (after east sidebar)

| Phase | Doc | Why |
|-------|-----|-----|
| Repeater groups | [`tools/repeater.md`](./tools/repeater.md) | Tab organization |
| Target scope list | [`tools/target.md`](./tools/target.md) | `list_scope_rules` spike |
| Sitemap (+ approval) | [`tools/target.md`](./tools/target.md) · [`montoya-gaps.md`](./montoya-gaps.md) | Paginated site map |
| MCP tab v2/v3 | [`mcp-tab-ui.md`](./mcp-tab-ui.md) | Per-tool enable; markdown docs in Burp |
| VitePress site | [`vitepress.md`](./vitepress.md) | Publish: `apps/docs` + CF Pages |
| Refactor Wave B | [`refactor.md`](./refactor.md) | Split `Tools.kt` when touching many tools |

## Before rename → `zamdevio/burp-mcp`

- [x] README positioning + phases for UI/guides
- [x] Default MCP host **`127.0.0.1`** in `McpConfig`
- [x] Git commit/remote rules (`.cursor/rules/git.mdc` · `maintainer/agents/git.md`)
- [x] `./gradlew test` green
- [x] Push to [github.com/zamdevio/burp-mcp](https://github.com/zamdevio/burp-mcp)
- [ ] Local folder rename (scratch notes stay in `maintainer/temp/`, untracked)

## Shipped recently (see [`../shipped/README.md`](../shipped/README.md))

- Repeater **`get_repeater_context`** + JSON **`RepeaterToolEnvelope`** on Swing tools
- Repeater **close** tab tools
- **`scan_repeater_notes_ui`** (debug)
- Suite / state-plane docs for session + east sidebar restore rules

## Explicitly not done (do not assume in agents)

- **`get_repeater_tab_notes` / `set_repeater_tab_notes`** — ledger **`blocked`** until east sidebar Step 1 passes
- East rail visibility / rail tab MCP tools — **todo** (same phase)
- Repeater groups — **todo**

## Not now

- Runtime skin packs / external UI rule downloads
- Patching Burp Suite JAR or committing decompiled Burp sources
- MCP tab **UI polish** (layout/visual) — later
- MCP tab v2 / v3 — planned only
