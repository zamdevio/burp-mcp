# Focus — current sprint

**Updated:** 2026-08-30

---

## Now

1. ~~**Repeater enforce**~~ — select → settle → act → **restore** for tab-targeted get/set (`RepeaterTabSession`).
2. ~~**Repeater Send**~~ — `send_repeater_tab` + `send_repeater_tab_and_get_response` (Swing, fail-safe).

## Queued (good next chat after rename)

| Phase | Doc | Why |
|-------|-----|-----|
| MCP tab tool catalog | [`mcp-tab-ui.md`](mcp-tab-ui.md) | Operators see tools + descriptions in Burp; shared metadata with registration |
| Agent client guides | [`agent-guides.md`](agent-guides.md) | Cursor, Claude Desktop, Claude Code, generic SSE/stdio |
| VitePress site | [`vitepress.md`](vitepress.md) | Publish: `apps/docs` + CF Pages (follow gform) |
| Montoya gaps | [`montoya-gaps.md`](montoya-gaps.md) | `get_proxy_intercept_state`, `get_burp_version`, `is_in_scope`, … |

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

## Not now

- Runtime skin packs / external UI rule downloads
- Patching Burp Suite JAR
