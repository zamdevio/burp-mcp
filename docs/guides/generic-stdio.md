# Generic stdio + proxy

MCP clients that only speak **stdio** need the bundled **mcp-proxy**, which bridges stdio ↔ Burp’s SSE URL.

## Prerequisites

1. Burp MCP server **Enabled** at `http://127.0.0.1:9876` (default).
2. Proxy JAR:
   - MCP tab → **Extract server proxy**, or
   - `./gradlew embedProxyJar` → `build/proxy/mcp-proxy-all.jar` (also embedded in the extension JAR).

## Command

```bash
java -jar /absolute/path/to/mcp-proxy-all.jar --sse-url http://127.0.0.1:9876
```

Wire that as the client’s `command` + `args` (see [`claude-desktop.md`](claude-desktop.md) for a JSON example).

## Smoke

1. Start the client so it spawns the proxy.
2. List tools / call `url_encode`.
3. Optionally verify Burp’s SSE side alone: `./scripts/mcp/test.sh`.

## Security

Proxy has the same reach as the MCP server. Keep config-edit and broad auto-approve off unless intentional. Authorized testing only.

## Troubleshooting

| Symptom | Check |
|---------|--------|
| Proxy exits | Burp MCP down; bad `--sse-url`; Java missing |
| Client sees no tools | Proxy stderr; SSE Host/port; client JSON syntax |
| Cross-OS / WSL | `--sse-url` must use a host the **proxy process** can TCP to; use `./scripts/mcp/check.sh` from that environment |

Proxy upstream project: [PortSwigger/mcp-proxy](https://github.com/PortSwigger/mcp-proxy). More: [`../install.md`](../install.md) · [`generic-sse.md`](generic-sse.md).
