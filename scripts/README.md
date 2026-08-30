# Scripts

Optional helpers for maintainers. **Build and test stay on Gradle;** deploy wraps them for live smoke.

```bash
./gradlew test
./gradlew embedProxyJar
```

## MCP connectivity (`scripts/mcp/`)

When the Burp MCP server is **Enabled**, from the repo root:

```bash
./scripts/mcp/check.sh              # TCP + SSE smoke
./scripts/mcp/test.sh               # initialize → tools/list → url_encode
./scripts/mcp/check.sh --host 172.x.x.x --port 9876
```

| Script | Role |
|--------|------|
| [`mcp/check.sh`](mcp/check.sh) | Reachability + SSE session endpoint |
| [`mcp/test.sh`](mcp/test.sh) | Full MCP handshake + harmless tool call |

Shared helpers live in [`mcp/lib/`](mcp/lib/) (`bmcp_*`). Host auto-detects by platform (localhost, then WSL gateway when needed). Pass `--host` / `--port` or set `BURP_MCP_HOST` / `BURP_MCP_PORT` in `.env`.

These scripts are for **local diagnostics** — they are not a CI gate (Burp must be running).

## Live deploy

```bash
cp .env.example .env   # once — set BURP_EXTENSION_JAR_DEST
./scripts/deploy-extension.sh
```

Copies `build/libs/burp-mcp-all.jar` to the Burp Extensions path from `.env` (or a CLI path). See [`maintainer/agents/live-smoke.md`](../maintainer/agents/live-smoke.md).

Do not duplicate Gradle tasks without reason. Gates: **`maintainer/systems/health.md`**.
