# Claude Code (terminal)

Connect **Claude Code** (CLI) to burp-mcp. Prefer **SSE** when the CLI supports an HTTP MCP URL; otherwise use the **stdio proxy**.

## Prerequisites

1. Burp running, burp-mcp loaded, MCP **Enabled**.
2. Default URL: `http://127.0.0.1:9876`.

## SSE

If your Claude Code build accepts an MCP HTTP/SSE endpoint, register:

```text
http://127.0.0.1:9876
```

Follow Anthropic’s current CLI docs for the exact `claude mcp add` / config file flags (they change by version). After add, restart or rehash the session as documented.

## Stdio proxy

```bash
java -jar /path/to/mcp-proxy-all.jar --sse-url http://127.0.0.1:9876
```

Point Claude Code’s stdio MCP server entry at that command (same pattern as [`claude-desktop.md`](claude-desktop.md)).

## Smoke

- In the CLI session: list MCP tools / call `url_encode`.
- Host reachability: `./scripts/mcp/check.sh` then `./scripts/mcp/test.sh`.

## Security

Same as other clients — authorized testing only; Burp approval gates still apply.

## Troubleshooting

- CLI on WSL + Burp on Windows: use `./scripts/mcp/check.sh --host <ip>` and put that IP in the SSE URL **path** host while keeping Burp’s configured listen/Host rules in mind (scripts use `Host: 127.0.0.1:port`).
- Proxy JAR: extract from MCP tab or `build/proxy/` after Gradle embed.

See [`generic-sse.md`](generic-sse.md) · [`generic-stdio.md`](generic-stdio.md) · [`../install.md`](../install.md).
