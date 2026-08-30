# Agent rules

## Source of truth

- Sprint: [`phases/focus.md`](../phases/focus.md)
- Repeater: [`phases/repeater-ui.md`](../phases/repeater-ui.md)
- Shipped: [`shipped/README.md`](../shipped/README.md) — do not redo closed work
- Layout: [`tree.md`](tree.md)
- Git: [`git.md`](git.md)

## Hard constraints

- **Never** add Repeater tab registry, cache, or `Map<tabId, …>` shadow state.
- **Never** patch or redistribute Burp Suite JAR.
- **Never** download UI discovery rules from GitHub at runtime — strategies ship in extension JAR.
- Swing mutations: **EDT**; select → settle → act → **restore** for tab-targeted UI ops (when enforcing).
- Fail safe on ambiguous editors — clear agent-facing errors, not wrong mutations.

## Scope

- No arbitrary filesystem, shell, telemetry, or unrelated network from MCP tools.
- Respect HTTP/data-access/config gates in [`systems/security.md`](../systems/security.md).

## Docs boundary

- **`maintainer/**`** — contributor/agent only.
- **`docs/**`** — user/contributor reference; plain language; no sprint status.
- Root **README** — install + quick usage.

## Tests

- Unit-test pure logic without Burp.
- Do not commit secrets or live session tokens from manual probes.
- **No personal / hunt-specific data in tracked files** — tests, docs, examples, and comments use generic hosts (`example.com`, `api.example.com`), not real customer/project domains, usernames, or machine paths. Live smoke may hit real targets; keep that out of the repo.
