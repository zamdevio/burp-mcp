# Health gates

Required before claiming done or opening a PR:

```bash
./gradlew test
```

Build extension JAR for manual Burp load:

```bash
./gradlew embedProxyJar
# output: build/libs/burp-mcp-all.jar
```

Optional (when added to CI):

```bash
./gradlew build
```

## Manual smoke (Repeater / UI tools)

1. Load JAR in Burp, enable MCP.
2. Open Repeater with ≥2 tabs.
3. Client: `list_repeater_tabs`, `get_repeater_tab_request` on a tab id.

Document Burp version used in PR or `shipped/` note when UI discovery changes.

CI (GitHub Actions) runs the same `./gradlew test` under **xvfb** so Swing Repeater tests are not headless.
