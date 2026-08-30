# Live smoke — Burp MCP in Cursor

**Audience:** Contributors and coding agents.  
**Cursor twin:** [`.cursor/rules/live-smoke.mdc`](../../.cursor/rules/live-smoke.mdc)

Smoke in the **same chat** after tool-affecting work.

---

## Prefer smoke over “ship and hope”

For MCP tool or Repeater/Swing changes:

1. Implement + `./gradlew test`
2. Deploy + Cursor MCP reload gate
3. **Live-smoke** (happy path **and** unexpected inputs)
4. Fix regressions found in smoke
5. **Then** commit — only when the user asks

**Do not commit** tool/UI changes before live-smoke completes, **unless** the user explicitly asks to commit without smoke. Always **prefer** doing the smoke; argue for it if the user is about to skip it for feature work.

Docs-only / pure unit-test refactors with no MCP or Swing surface may skip smoke.

---

## Automation split (best we can get)

| Who | What |
|-----|------|
| **Agent** | `./scripts/deploy-extension.sh` → test + embed + `cp` JAR |
| **Burp** | **Auto-reload** on loaded **Burp MCP Server** |
| **User** | Cursor Settings → MCP → reload `burp` — then tell the agent to continue |

Agents **cannot** reload Cursor’s MCP client.

---

## One-time setup

1. Copy [`.env.example`](../../.env.example) → `.env` and set `BURP_EXTENSION_JAR_DEST`. `.env` is gitignored.
2. Burp → Extensions: load `burp-mcp-all.jar`, **Auto-reload** on.
3. Prefer a single MCP extension loaded for smoke.

---

## Deploy

```bash
./scripts/deploy-extension.sh
./scripts/deploy-extension.sh --skip-tests
./scripts/deploy-extension.sh /mnt/c/Users/<you>/BurpSuite/Extensions
```

Script: [`scripts/deploy-extension.sh`](../../scripts/deploy-extension.sh).

---

## Preflight

1. JAR deployed; Burp auto-reload OK.
2. User confirmed Cursor `burp` MCP reloaded.
3. Tools callable; else stop and ask for another reload. Do not invent results.

---

## Smoke matrix

Fail safe. Prefer disposable Repeater tabs. **PASS/FAIL** with exact error strings.

| # | Check | Tools / notes |
|---|--------|----------------|
| A | Connectivity | Cheap call; Repeater extras present on burp-mcp |
| B | List | `list_repeater_tabs` |
| C | Active | `get_active_repeater_tab` |
| D | Read other tab | `get_repeater_tab` / `_request` / `_response` on **non-selected** |
| E | Restore | Selection matches prior after D (ask user if needed) |
| F | Write | `set_repeater_tab_request`; verify; restore |
| G | Select | `select_repeater_tab` — **leaves** selection |
| H | Title | `set_repeater_tab_title` — restore title if changed |
| I | Send | `send_repeater_tab` (no wait) |
| J | Send+wait | `send_repeater_tab_and_get_response` (optional short `timeout_ms`) |
| K | Errors / edges | See below |

### Unexpected inputs (do a few every run)

| Input | Expect |
|-------|--------|
| `tab_id` empty / `nope` / `repeater-tab-999` | Clear `<…>` error; no wrong-tab mutation |
| `set_repeater_tab_request` with empty or garbage HTTP | Fail safe or set and leave editor as-is — no crash |
| `timeout_ms` very small (e.g. `1`) on send-and-wait | Timeout error or clamp; no hang forever |
| `set_repeater_tab_title` empty / whitespace | Clear error |
| Double `list` after close/reorder | Ids may change — re-list before mutate |

Fix clear bugs found in smoke before offering a commit.

**Tracked-repo hygiene:** smoke may use real apps; **do not** copy personal domains, usernames, or machine paths into tests/docs/examples — keep generics (`example.com`) in the repo.

---

## After smoke

- Update [`shipped/README.md`](../shipped/README.md) only when confirmed on live Burp.
- Note Burp version when UI discovery is involved.
- Commit only if the user asked (and smoke passed, or they waived smoke).
