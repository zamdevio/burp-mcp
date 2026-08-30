# Origins

## Upstream

**burp-mcp** is derived from PortSwigger’s official Burp MCP extension:

- Repository: [github.com/PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server)
- Role: in-process MCP server for Burp Suite (Montoya + Ktor SSE), with optional stdio proxy packaging and Claude Desktop install helpers

That project established the transport, tool registration patterns, security approvals, and the first generation of Montoya-backed tools (HTTP send, proxy history, send-to-Repeater/Intruder, Collaborator, config export/merge, utilities).

## This derivative

This repository started as a **fork / copy** of that codebase, then was extended and reorganized for agent-driven Burp work:

1. **Repeater tab control** — Swing UI discovery so agents can list and operate on individual Repeater tabs (`repeater-tab-N`), not only create tabs or touch the focused editor.
2. **Maintainer / agent scaffolding** — `maintainer/phases`, systems maps, `.cursor/rules`, and `docs/` so humans and coding agents share one backlog and layout.
3. **Distribution** — maintained as **[zamdevio/burp-mcp](https://github.com/zamdevio/burp-mcp)**; Gradle artifact **`burp-mcp-all.jar`**.

We do **not** claim to replace PortSwigger’s official extension in the BApp Store or as PortSwigger’s product. Credit for the baseline design and implementation belongs to the upstream authors.

## License

Released under **GPL-3.0** (see root [`LICENSE`](../LICENSE)), consistent with distributing a modified GPL work. Preserve copyright notices from upstream where they appear in source.

## Tracking upstream

When useful, add PortSwigger’s repo as `upstream` and cherry-pick or merge carefully around local Repeater / package changes. Do not assume API or UI compatibility without re-testing on your Burp version.
