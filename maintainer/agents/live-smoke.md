# Live smoke — Burp MCP in Cursor

**Audience:** Contributors and coding agents.  
**Cursor twin:** [`.cursor/rules/live-smoke.mdc`](../../.cursor/rules/live-smoke.mdc)

Smoke in the **same chat** after tool-affecting work.

---

## Automation split (best we can get)

| Who | What |
|-----|------|
| **Agent** | `./scripts/deploy-extension.sh` → `./gradlew test` + `embedProxyJar` + `cp` JAR to Burp Extensions |
| **Burp** | **Auto-reload** enabled on the loaded **Burp MCP Server** extension (Extensions table → Auto-reload). File overwrite reloads the extension / SSE server |
| **User** | **Only** Cursor Settings → MCP → reload `burp` — then tell the agent to continue smoking |

Agents **cannot** reload Cursor’s MCP client. Do not wait for Cursor to auto-pick up a new tool list — it usually stays stale until manual reload.

---

## One-time setup

1. Copy [`.env.example`](../../.env.example) → `.env` and set `BURP_EXTENSION_JAR_DEST` (directory or full `.jar` path). `.env` is gitignored.
2. In Burp → Extensions: load `burp-mcp-all.jar`, leave **Extension loaded** on, enable **Auto-reload**.
3. Prefer a single MCP extension loaded for smoke (disable other MCP Server forks so tool counts aren’t confusing).

---

## Deploy

```bash
# Default: test + embed + cp using .env
./scripts/deploy-extension.sh

# Faster iterate after tests already green
./scripts/deploy-extension.sh --skip-tests

# Override destination for this run
./scripts/deploy-extension.sh /mnt/c/Users/<you>/BurpSuite/Extensions
```

Script: [`scripts/deploy-extension.sh`](../../scripts/deploy-extension.sh).

---

## Preflight (before smoke calls)

1. Agent deployed JAR; Burp auto-reload had a chance to run (or user confirms extension reloaded).
2. User confirms Cursor `burp` MCP was reloaded.
3. `user-burp` tools callable; if not — stop, ask for another Cursor reload. Do not invent results.

---

## Smoke matrix (adjust to what changed)

Fail safe. Prefer disposable Repeater tabs. Report **PASS/FAIL** per step with exact error strings.

| # | Check | Tools / notes |
|---|--------|----------------|
| A | Tool list / connectivity | Cheap call; expect local fork tool set (Repeater extras present) when burp-mcp is loaded |
| B | List tabs | `list_repeater_tabs` — ids `repeater-tab-N` |
| C | Active tab | `get_active_repeater_tab` |
| D | Read other tab | `get_repeater_tab` / `_request` / `_response` on a **non-selected** tab |
| E | Restore | After D, suite + Repeater selection should match prior (ask user if agent cannot see UI) |
| F | Write | `set_repeater_tab_request` on a disposable tab; verify; selection restored |
| G | Select | `select_repeater_tab` — **leaves** selection (intentional) |
| H | Title | `set_repeater_tab_title` — rename; restore sane title if you changed it |
| I | Errors | Bad / missing id → clear agent error; no wrong-tab mutation |

When only Montoya/HTTP tools changed, smoke those instead of the full Repeater matrix.

---

## After smoke

- Update [`shipped/README.md`](../shipped/README.md) only when behavior is confirmed on live Burp.
- Note Burp version in the PR or shipped receipt when UI discovery is involved.
