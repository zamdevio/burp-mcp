# MCP tools registration

## Pattern

- Serializable `@Serializable data class` params → tool name via `toLowerSnakeCase()` (`tools/McpTool.kt`).
- Handlers in `tools/Tools.kt` today — split by domain per [`phases/refactor.md`](../phases/refactor.md).
- Paginated tools implement `Paginated` (`count`, `offset`).

## Security hooks

- **HTTP send:** `HttpRequestSecurity` + user approval / auto-approve targets.
- **History / organizer / WS:** `DataAccessSecurity` when enabled in config.
- **Config set:** gated by `configEditingTooling` in MCP tab.

## Adding a tool

1. Prefer **Montoya API** if it exists ([`phases/montoya-gaps.md`](../phases/montoya-gaps.md)).
2. If Swing required, use **`ui/`** session (select → act → restore) when available.
3. Add tests in `src/test/.../tools/` or domain test package.
4. Document user-facing summary in **`docs/tools.md`**.
