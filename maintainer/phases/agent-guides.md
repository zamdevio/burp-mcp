# Phase: Agent client guides

**Status:** Planned — docs-only first; optional deep links from MCP tab install panel.

**Public index:** [`docs/guides/README.md`](../../docs/guides/README.md)

---

## Problem

Burp-side setup is one half; each MCP **host** (IDE, desktop app, CLI) has different transport (SSE vs stdio), config file location, and restart rules. Users bounce between upstream PortSwigger notes and ad-hoc forum posts.

---

## Goals

1. **Per-client guides** — copy-paste configs for the clients we care about most.
2. **Two transport paths** — document **SSE URL** vs **stdio proxy** clearly; every guide ends with “verify with `list_tools`”.
3. **Security callouts** — remind: authorized targets only; understand HTTP approval and auto-approve list.
4. **Cross-link** — root README, `docs/install.md`, and (later) MCP tab **Installation** panel link to the right guide.

---

## Target guides (v1)

| Guide | Host | Transport | Priority |
|-------|------|-----------|----------|
| `cursor.md` | Cursor (Agent / MCP settings) | SSE to `http://127.0.0.1:9876` | P0 |
| `claude-desktop.md` | Claude Desktop | stdio via bundled proxy + tab installer | P0 |
| `claude-code.md` | Claude Code (terminal) | stdio or SSE per host docs | P1 |
| `generic-sse.md` | Any SSE-capable MCP client | Direct SSE URL | P1 |
| `generic-stdio.md` | Any stdio MCP client | `mcp-proxy-all.jar --sse-url …` | P1 |

**Later:** Windsurf, Continue, Zed, LM Studio, custom `mcp.json` examples — add when we have verified steps.

---

## Each guide template

1. Prerequisites (Burp running, extension loaded, MCP **Enabled**).
2. Connection string / config snippet (redact secrets).
3. Restart or reload client.
4. Smoke test (e.g. `list_repeater_tabs` or harmless utility tool).
5. Troubleshooting (port in use, wrong Java for proxy, approval dialogs blocking HTTP).

---

## Repo layout (target)

```text
docs/guides/
  README.md          # index + matrix (client × transport)
  cursor.md
  claude-desktop.md
  claude-code.md
  generic-sse.md
  generic-stdio.md
```

Optional future: `docs/tools/` split mirrors tool catalog — not required for guides v1.

---

## Done when

- [ ] P0 guides merged and linked from README + `docs/install.md`.
- [ ] MCP tab install panel links to `docs/guides/` (can ship with [`mcp-tab-ui.md`](mcp-tab-ui.md) or earlier as hyperlinks only).
- [ ] Receipt in `shipped/`.

---

## Related

- [`mcp-tab-ui.md`](mcp-tab-ui.md) — in-Burp tool list + link to guides.
- Upstream: PortSwigger manual install steps in `InstallationPanel` — align wording, don’t fork blindly.
