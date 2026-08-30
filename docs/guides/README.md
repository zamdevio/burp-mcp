# Agent setup guides

Step-by-step wiring from **burp-mcp** to popular MCP hosts. Burp must be running with the extension loaded and the MCP server **Enabled** (default URL **`http://127.0.0.1:9876`**).

| Guide | Transport | Status |
|-------|-----------|--------|
| [Cursor](cursor.md) | SSE (+ bridge as needed) | Ready |
| [Claude Desktop](claude-desktop.md) | Stdio proxy | Ready |
| [Claude Code (terminal)](claude-code.md) | SSE or stdio | Ready |
| [Generic SSE client](generic-sse.md) | SSE | Ready |
| [Generic stdio + proxy](generic-stdio.md) | Stdio proxy | Ready |

Also: [`../install.md`](../install.md) · connectivity: [`../../scripts/mcp/check.sh`](../../scripts/mcp/check.sh) / [`test.sh`](../../scripts/mcp/test.sh).

## Quick reference

| Transport | When | URL / command |
|-----------|------|----------------|
| **SSE** | Client supports HTTP+SSE | `http://127.0.0.1:9876` |
| **Stdio** | Client only speaks stdin/stdout | `java -jar mcp-proxy-all.jar --sse-url http://127.0.0.1:9876` |

After connect, confirm tools appear (`url_encode`, Repeater, proxy history, …). Prefer `./scripts/mcp/test.sh` to verify the Burp side before debugging the client.

## Security

Use only on systems you are authorized to test. Understand HTTP approval and the MCP tab auto-approve list before broad agent use.
