# Tree layout — packages, models, split files

Cursor twin: [`.cursor/rules/tree.mdc`](../../.cursor/rules/tree.mdc).

## Why

Kotlin uses **packages**, not TS barrels — but the same ideas apply: **domain folders**, **small files**, **no god classes**, tests mirror main.

`Tools.kt` (~600+ lines) should shrink via [`phases/refactor.md`](../phases/refactor.md).

---

## Root layout

```text
src/main/kotlin/net/portswigger/mcp/
  ExtensionBase.kt          # Burp entry
  KtorServerManager.kt      # SSE MCP server
  ServerManager.kt
  SwingDispatcher.kt
  config/                   # MCP tab UI + McpConfig
  providers/                # Claude / proxy install helpers
  schema/                   # JSON schema, history DTOs, serialization
  security/                 # HTTP + data access approvals
  tools/
    McpTool.kt              # registration helpers
    Tools.kt                # registerTools — split over time
  repeater/                 # Repeater Swing discovery (today)
  ui/                       # (planned) shared UI session, version strategies, AgentError

src/test/kotlin/net/portswigger/mcp/
  ...                       # same package paths as main

maintainer/                 # not user docs
docs/                       # user/contributor reference
scripts/                    # optional shell helpers (Gradle stays ./gradlew)
# stdio proxy JAR: fetched to build/proxy/ (not committed)
```

---

## Package rules

| Kind | Home |
|------|------|
| `@Serializable` MCP params + shared DTOs | `schema/` or domain package (e.g. `repeater/RepeaterTabModels.kt`) |
| Montoya tool handlers | `tools/` — split into `tools/repeater/`, `tools/proxy/`, … |
| Swing / Burp UI | `repeater/`, future `ui/`, future `intruder/` |
| Constants (error prefixes, tool titles, timeouts) | `shared/constants/` when created, or colocated until Wave B |
| Config UI Swing | `config/` only |

- **Do not** put Swing tree walking in `tools/` — call discovery layer.
- **Do not** add duplicate DTOs for the same MCP shape.

---

## File size

- Prefer **&lt; ~250–300 lines** per file; split by concern into subpackage.
- One domain per directory once you have 2+ files (already true for `repeater/`, `config/components/`).

---

## Tests

- Mirror package: `src/test/kotlin/net/portswigger/mcp/repeater/RepeaterUiDiscoveryTest.kt`
- Synthetic Swing trees OK; no pixel/layout/timing brittle tests.
- Integration: `ToolsKtTest`, `McpServerIntegrationTest` with mocked Montoya.

---

## Gradle

- Versions: `gradle/libs.versions.toml`
- Do not commit `build/`, `.gradle/`, or `.kotlin/`
