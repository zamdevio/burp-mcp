# Cursor

Connect **Cursor** to burp-mcp over **SSE**.

## Prerequisites

1. Burp Suite running with **burp-mcp** loaded.
2. Suite tab **MCP** → server **Enabled**.
3. Advanced defaults: host **`127.0.0.1`**, port **`9876`** (or note your values).

## Config

Cursor → **Settings → MCP** (or edit `~/.cursor/mcp.json`). Example stdio bridge is common when Cursor cannot open SSE to Windows from WSL; if your Cursor build supports a direct URL server, use the SSE URL below.

**SSE URL (Burp listens here):**

```text
http://127.0.0.1:9876
```

Typical `mcp.json` entry when using a local bridge command that talks SSE to Burp:

```json
{
  "mcpServers": {
    "burp": {
      "command": "/path/to/your/mcp-bridge",
      "args": []
    }
  }
}
```

If your Cursor version accepts an HTTP/SSE MCP server natively, point it at `http://127.0.0.1:9876` per Cursor’s current MCP docs.

**WSL:** If Cursor’s agent runs in WSL and Burp runs on Windows, `127.0.0.1` inside WSL is not Windows. Run:

```bash
./scripts/mcp/check.sh
# or force the Windows host IP:
./scripts/mcp/check.sh --host <windows-ip>
```

Use a working host in your bridge / client config. HTTP `Host` for Burp must remain `127.0.0.1:9876` (the check/test scripts already do this).

## Reload

After changing MCP config or restarting Burp’s MCP server: **Settings → MCP → reload** the `burp` server (or restart Cursor). Burp **Auto-reload** of the extension JAR does **not** refresh Cursor’s MCP client.

## Smoke

1. Confirm tools appear (`url_encode`, `list_repeater_tabs`, …).
2. Call `url_encode` with `hello world` → expect `hello%20world`.
3. Optional: `list_repeater_tabs` with Burp Repeater open.

Repo diagnostics (Burp must be up):

```bash
./scripts/mcp/test.sh
```

## Security

- Use only on systems you are authorized to test.
- HTTP send / history may require approval in the MCP tab; configure **auto-approve** targets carefully.
- Leave **config editing** off unless you intend to change Burp options via MCP.

## Troubleshooting

| Symptom | Check |
|---------|--------|
| No tools | MCP Enabled? Reload Cursor MCP? Port conflict? |
| Connection refused | `./scripts/mcp/check.sh`; wrong host from WSL |
| Tools hang on HTTP | Approval dialog in Burp; auto-approve list |
| Stale tools after JAR update | Reload Cursor MCP after Burp extension reload |

More: [`../install.md`](../install.md) · [`../repeater.md`](../repeater.md) · [`../limitations.md`](../limitations.md).
