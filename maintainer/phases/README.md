# Phases — maintainer hub

**Not for end users** — planning lives here; public copy stays in root **`README.md`** and **`docs/`**.

---

## Start here

| Doc | Role |
|-----|------|
| [`focus.md`](./focus.md) | **Current sprint focus** |
| [`../shipped/README.md`](../shipped/README.md) | Closed work — check before re-implementing |

Scratch / spikes: **`maintainer/temp/`** (gitignored).

---

## Plans

| Doc | Status |
|-----|--------|
| [`repeater-ui.md`](./repeater-ui.md) | **Active** — enforce select/settle/restore, Send, agent errors |
| [`mcp-tab-ui.md`](./mcp-tab-ui.md) | **Active** — v1 Tools catalog; v2/v3 planned |
| [`agent-guides.md`](./agent-guides.md) | **Shipped (docs)** — Cursor, Claude, generic SSE/stdio |
| [`montoya-gaps.md`](./montoya-gaps.md) | **Backlog** — API-first new tools |
| [`refactor.md`](./refactor.md) | **Ongoing** — layout + `ui/` + split tools (waves) |
| [`vitepress.md`](./vitepress.md) | **Planned (publish)** — `apps/docs` VitePress + CF Pages (pattern: gform) |

---

## Lifecycle

1. Scope from **`focus.md`** and one phase doc — one slice per change when possible.
2. Close slices in **`shipped/`** with date + one line.
3. Session noise → **`maintainer/temp/`** only.
4. Gates before merge: [`systems/health.md`](../systems/health.md).
