# Scripts

Optional helpers for maintainers. **Build and test stay on Gradle;** deploy wraps them for live smoke.

```bash
./gradlew test
./gradlew embedProxyJar
```

## Live deploy

```bash
cp .env.example .env   # once — set BURP_EXTENSION_JAR_DEST
./scripts/deploy-extension.sh
```

Copies `build/libs/burp-mcp-all.jar` to the Burp Extensions path from `.env` (or a CLI path). See [`maintainer/agents/live-smoke.md`](../maintainer/agents/live-smoke.md).

Do not duplicate Gradle tasks without reason. Gates: **`maintainer/systems/health.md`**.
