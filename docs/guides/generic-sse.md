# Generic SSE client

Any MCP host that speaks **HTTP + Server-Sent Events** can attach directly to burp-mcp.

## Prerequisites

Burp + burp-mcp loaded; MCP tab server **Enabled**.

## Connection

```text
http://127.0.0.1:9876
```

Replace host/port with Advanced settings from the MCP tab if you changed them.

**Important:** Many builds of the server validate the HTTP `Host` header as `127.0.0.1:<port>`. Connecting via another IP (e.g. WSL → Windows gateway) still requires that Host header. The repo helpers do this for you:

```bash
./scripts/mcp/check.sh --host <connect-ip>
./scripts/mcp/test.sh --host <connect-ip>
```

## Smoke

After the client connects:

1. `tools/list` — expect HTTP, Repeater, proxy, utilities (and Pro tools on Professional).
2. `tools/call` `url_encode` with `content: "hello world"`.

## Security

Authorized targets only. Approvals / auto-approve in Burp still apply.

## Troubleshooting

| Symptom | Check |
|---------|--------|
| TCP OK, HTTP 403 | Wrong `Host` header — use `127.0.0.1:port` |
| No `event: endpoint` | Not the MCP SSE port; wrong extension/server |
| Empty tool list | Server failed mid-init; check Burp extension Errors |

Install overview: [`../install.md`](../install.md).
