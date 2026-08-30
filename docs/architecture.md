# Architecture

High-level map for operators and contributors. Implementation detail for agents: [`maintainer/systems/`](../maintainer/systems/).

## Runtime shape

```text
MCP client  ──SSE──►  Ktor server (in Burp)  ──►  tool handlers
                ▲                                    │
                │                              ┌─────┴─────┐
         optional stdio                        │           │
         mcp-proxy                             ▼           ▼
                                         Montoya API   Swing UI
                                         (HTTP, hist.,  (Repeater
                                          config, …)     tabs, …)
```

- **Transport:** Ktor SSE inside the Burp process (`KtorServerManager`). Clients may attach via SSE or via the packaged **stdio proxy**.
- **Registration:** tools registered from `tools/` (today largely `Tools.kt`; domain splits planned).
- **Security:** mutating / sensitive paths go through `security/` (approvals, auto-approve targets, config-edit flag).

## Two access paths

| Path | When | Examples |
|------|------|----------|
| **Montoya API** | Public API exposes the capability | `send_http1_request`, proxy history, `create_repeater_tab`, Collaborator |
| **Swing discovery** | API cannot list or read suite UI state | `list_repeater_tabs`, get/set Repeater request text, select/rename tab |

Rules of thumb:

- Prefer Montoya when it exists ([`maintainer/phases/montoya-gaps.md`](../maintainer/phases/montoya-gaps.md)).
- Never patch Burp’s own JAR.
- Never keep a long-lived Repeater “registry” that can disagree with the UI — re-discover on each call.
- Tab-targeted Swing ops should **select → settle → act → restore** so agents do not leave the user’s selection wrong ([`maintainer/phases/repeater-ui.md`](../maintainer/phases/repeater-ui.md)).

## Packages (Kotlin)

| Package / area | Role |
|----------------|------|
| `tools/` | MCP tool registration and handlers |
| `repeater/` | Repeater models + Swing discovery |
| `security/` | Approvals and sensitive-operation gates |
| `config/` | Burp MCP suite-tab UI and config persistence |
| `schema/` | Shared serializable DTOs (where used) |
| future `ui/` | Shared Swing session / version strategies (planned) |

Tests mirror main under `src/test/kotlin/...`.

## Build

- Gradle Kotlin DSL; versions in `gradle/libs.versions.toml`.
- Montoya API dependency line: **2025.10** (compatible with recent Burp 2026.x in practice — always re-validate UI tools on your install).
- Fat extension: `./gradlew embedProxyJar` → `build/libs/burp-mcp-all.jar` (Gradle also fetches PortSwigger’s stdio **mcp-proxy** into `build/proxy/`, then embeds it). Do not commit `libs/`.
