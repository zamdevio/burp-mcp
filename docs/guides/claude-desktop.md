# Claude Desktop

Connect **Claude Desktop** to burp-mcp via the **stdio proxy** (Claude speaks stdio; Burp speaks SSE).

## Prerequisites

1. Burp Suite running with **burp-mcp** loaded.
2. Suite tab **MCP** → server **Enabled** (`http://127.0.0.1:9876` by default).
3. Java available for the proxy process Claude Desktop will spawn.

## Preferred: Install from the MCP tab

1. Open Burp → **MCP**.
2. Under **Installation**, use **Install to Claude Desktop** (or the equivalent provider button).
3. Restart **Claude Desktop** completely.
4. Confirm a Burp / MCP server appears in Claude’s connector / MCP settings.

## Manual stdio config

Extract the proxy from the MCP tab (**Extract server proxy**) or build the repo (`./gradlew embedProxyJar` → proxy also under `build/proxy/mcp-proxy-all.jar`).

Claude Desktop config (path varies by OS), shape:

```json
{
  "mcpServers": {
    "burp": {
      "command": "java",
      "args": [
        "-jar",
        "/absolute/path/to/mcp-proxy-all.jar",
        "--sse-url",
        "http://127.0.0.1:9876"
      ]
    }
  }
}
```

Use Burp’s bundled Java if the system `java` is wrong/missing.

## Smoke

1. Open a Claude chat that has MCP tools enabled.
2. Ask it to list MCP tools or run a harmless `url_encode`.
3. From this repo (optional): `./scripts/mcp/test.sh` verifies the SSE side Burp exposes.

## Security

Authorized targets only. Approvals and auto-approve in the Burp MCP tab still apply to HTTP/history tools.

## Troubleshooting

| Symptom | Check |
|---------|--------|
| Server missing after install | Full quit/reopen Claude Desktop; validate JSON |
| Proxy exits immediately | Java path; `--sse-url` reachable; MCP Enabled in Burp |
| From another machine / WSL | Proxy must reach Burp’s listen address; see [`generic-stdio.md`](generic-stdio.md) |

Also: [`../install.md`](../install.md) · [`generic-stdio.md`](generic-stdio.md).
