# Focus — current sprint

**Updated:** 2026-08-30

---

## Now

1. ~~**Agent client guides**~~ — [`docs/guides/`](../../docs/guides/) + [`agent-guides.md`](agent-guides.md).
2. ~~**scripts/mcp**~~ — `check.sh` / `test.sh` connectivity helpers.
3. ~~**MCP tab catalog v1**~~ — Tools pane + `ToolCatalog` ([`mcp-tab-ui.md`](mcp-tab-ui.md)).

## Queued

| Phase | Doc | Why |
|-------|-----|-----|
| MCP tab v2/v3 | [`mcp-tab-ui.md`](mcp-tab-ui.md) | Per-tool enable; markdown docs in Burp |
| Montoya gaps | [`montoya-gaps.md`](montoya-gaps.md) | `get_proxy_intercept_state`, `get_burp_version`, `is_in_scope`, … |
| VitePress site | [`vitepress.md`](vitepress.md) | Publish: `apps/docs` + CF Pages (follow gform) |
| Refactor Wave B | [`refactor.md`](refactor.md) | Split `Tools.kt` when touching many tools |

## Before rename → `zamdevio/burp-mcp`

- [x] README positioning + phases for UI/guides
- [x] Default MCP host **`127.0.0.1`** in `McpConfig`
- [x] Git commit/remote rules (`.cursor/rules/git.mdc` · `maintainer/agents/git.md`)
- [x] `./gradlew test` green
- [x] Push to [github.com/zamdevio/burp-mcp](https://github.com/zamdevio/burp-mcp)
- [ ] Local folder rename (scratch notes stay in `maintainer/temp/`, untracked)

## Done (2026-08-30)

- Docs/agents scaffold; README refresh; phase docs **mcp-tab-ui**, **agent-guides**
- Repeater enforce + Send (+ Target-not-specified gate); live-smoke deploy script
- User guide [`docs/repeater.md`](../../docs/repeater.md); VitePress phase [`vitepress.md`](vitepress.md)
- Agent guides v1; `scripts/mcp` check/test; MCP tab Tools catalog v1

## Not now

- Runtime skin packs / external UI rule downloads
- Patching Burp Suite JAR
- MCP tab **UI polish** (layout/visual) — later
- MCP tab v2 (per-tool on/off) / v3 (full MD in Burp) — planned only
