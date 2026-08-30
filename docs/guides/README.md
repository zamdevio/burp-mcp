# Agent setup guides

Step-by-step wiring from **Burp MCP** to popular MCP hosts. Burp must be running with the extension loaded and the MCP server **Enabled** (default URL **`http://127.0.0.1:9876`**).

| Guide | Status |
|-------|--------|
| [Cursor](cursor.md) | Planned |
| [Claude Desktop](claude-desktop.md) | Planned |
| [Claude Code (terminal)](claude-code.md) | Planned |
| [Generic SSE client](generic-sse.md) | Planned |
| [Generic stdio + proxy](generic-stdio.md) | Planned |

**Planning:** [`maintainer/phases/agent-guides.md`](../maintainer/phases/agent-guides.md)

Until each page is written, use [`install.md`](../install.md) and the MCP tab **Install to Claude Desktop** / **Extract server proxy** actions.

## Quick reference

| Transport | When | URL / command |
|-----------|------|----------------|
| **SSE** | Client supports HTTP+SSE natively | `http://127.0.0.1:9876` |
| **Stdio** | Client only speaks MCP over stdin/stdout | Run proxy JAR with `--sse-url http://127.0.0.1:9876` |

After connect, confirm tools appear in the client (HTTP, Repeater, proxy history, etc.).
