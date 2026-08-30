# System overview

## Runtime

1. **Burp loads** `ExtensionBase` → `McpConfig`, `KtorServerManager`, MCP tab UI.
2. **MCP clients** connect via SSE (`host:port`) or stdio **proxy JAR** → SSE.
3. **Tools** registered in `registerTools()` (`tools/Tools.kt`) — Montoya calls + Swing discovery.

## Lanes

| Lane | Packages | Examples |
|------|----------|----------|
| **Montoya API** | `tools/`, `security/`, `schema/` | HTTP send, proxy history, collaborator |
| **Swing UI** | `repeater/` → future `ui/` | Repeater tabs, Send (planned) |
| **Config UI** | `config/` | MCP tab, approvals, host/port |
| **Transport** | `KtorServerManager`, `ServerManager` | SSE, CORS, DNS rebind guards |

## Source of truth

- **Burp** for live UI and session data.
- **No shadow state** for Repeater tabs or UI discovery caches.

## Edition

Some tools register only on **Professional** (scanner, collaborator). Check `Tools.kt` and tests.
