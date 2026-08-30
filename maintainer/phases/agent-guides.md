# Phase: Agent client guides

**Status:** Shipped (v1 docs) — MCP tab deep-links can follow with [`mcp-tab-ui.md`](mcp-tab-ui.md).

**Public index:** [`docs/guides/README.md`](../../docs/guides/README.md)

---

## Problem

Burp-side setup is one half; each MCP **host** (IDE, desktop app, CLI) has different transport (SSE vs stdio), config file location, and restart rules.

---

## Goals

1. **Per-client guides** — copy-paste configs for the clients we care about most.
2. **Two transport paths** — document **SSE URL** vs **stdio proxy** clearly; every guide ends with verify / smoke steps.
3. **Security callouts** — authorized targets only; HTTP approval and auto-approve list.
4. **Cross-link** — root README, `docs/install.md`, and (with catalog phase) MCP tab install / tools pane.

---

## Shipped guides (v1)

| Guide | Host | Transport |
|-------|------|-----------|
| [`cursor.md`](../../docs/guides/cursor.md) | Cursor | SSE (+ bridge as needed) |
| [`claude-desktop.md`](../../docs/guides/claude-desktop.md) | Claude Desktop | stdio proxy + tab installer |
| [`claude-code.md`](../../docs/guides/claude-code.md) | Claude Code | SSE or stdio |
| [`generic-sse.md`](../../docs/guides/generic-sse.md) | Any SSE client | Direct SSE |
| [`generic-stdio.md`](../../docs/guides/generic-stdio.md) | Any stdio client | `mcp-proxy-all.jar` |

Connectivity helpers: [`scripts/mcp/check.sh`](../../scripts/mcp/check.sh), [`test.sh`](../../scripts/mcp/test.sh).

**Later clients:** Windsurf, Continue, Zed, LM Studio — add when verified.

---

## Done when

- [x] P0/P1 guides merged and linked from README + `docs/install.md`.
- [ ] MCP tab install / tools pane links to `docs/guides/` ([`mcp-tab-ui.md`](mcp-tab-ui.md)).
- [x] Receipt in `shipped/`.

---

## Related

- [`mcp-tab-ui.md`](mcp-tab-ui.md) — in-Burp tool list + link to guides.
- Upstream install actions in `InstallationPanel` — align wording, don’t fork blindly.
