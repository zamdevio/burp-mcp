# Phase: Montoya API coverage gaps

Maps **public Montoya API** (`2025.10` line, Burp 2026.x compatible) to MCP tools. **Prefer API-first**; use Swing only where Montoya cannot list/read UI (Repeater tabs, Send, etc.).

Legend: **Exposed** · **Gap** · **Skip**

---

## Already exposed

| Area | MCP tools |
|------|-----------|
| HTTP | `send_http1_request`, `send_http2_request` |
| Repeater | `create_repeater_tab`, `create_repeater_tab_http2`, tab list/read/write/select/title/Send (Swing) |
| Intruder | `send_to_intruder` |
| Proxy | HTTP/WS history + regex, `set_proxy_intercept_state`, `get_proxy_intercept_state` |
| Organizer | items + regex |
| Scope / info | `is_in_scope`, `include_in_scope`, `exclude_from_scope`, `get_burp_version`, `get_project_info` |
| Scanner | `get_scanner_issues` (Pro) |
| Collaborator | generate payload, get interactions (Pro) |
| Config | `output_*` / `set_*` project & user options |
| Engine | `set_task_execution_engine_state` |
| Utilities | url/base64/random |
| Editors | `get/set_active_editor_contents` |

---

## High-value gaps (API-ready)

| Montoya API | Suggested MCP tool(s) | Notes |
|-------------|----------------------|--------|
| ~~`Proxy.isInterceptEnabled()`~~ | ~~`get_proxy_intercept_state`~~ | **Shipped** |
| ~~`Scope.isInScope(url)`~~ | ~~`is_in_scope`~~ | **Shipped** |
| ~~`Scope.includeInScope` / `excludeFromScope`~~ | ~~`include_in_scope`, `exclude_from_scope`~~ | **Shipped** |
| `SiteMap.requestResponses()` | `get_sitemap_entries` (paginated) | Reuse data-access approval |
| `SiteMap.add(...)` | `add_to_sitemap` | Niche |
| `Http.cookieJar()` | cookie helpers | Session testing |
| `Http.sendRequests` (batch) | batch send | Fewer round-trips |
| ~~`BurpSuite.version()`~~ | ~~`get_burp_version`~~ | **Shipped** |
| ~~`Project.name()` / `id()`~~ | ~~`get_project_info`~~ | **Shipped** |
| `Decoder.sendToDecoder` | `send_to_decoder` | |
| `Comparer.sendToComparer` | `send_to_comparer` | |

---

## Medium / UI-heavy gaps

| Area | Gap |
|------|-----|
| **Intruder** | Create-only; tab list/read → Swing (Repeater pattern) |
| **Sequencer / Logger** | No Montoya module — Swing or skip |

---

## Scanner (Pro, guardrails)

| API | Suggested tools | Risk |
|-----|-----------------|------|
| `Scanner.startCrawl` | `start_crawl` | Long-running |
| `Scanner.startAudit` | `start_audit` | Active scan |
| `Scanner.generateReport` | file write — path policy | |
| `Scanner.bChecks()` | advanced | |

---

## Skip for MCP (default)

| API | Why |
|-----|-----|
| `Ai.prompt()` | External agents already |
| `Bambda.importBambda` | Code execution surface |

---

## Priority order

1. ~~Finish Repeater enforce + Send~~ ([`repeater-ui.md`](repeater-ui.md))
2. ~~`get_proxy_intercept_state`, `get_burp_version`, `is_in_scope`~~
3. Sitemap (paginated + approval)
4. ~~Scope include/exclude~~ / ~~`get_project_info`~~
5. Cookie jar
6. Intruder tab inspection (Swing)
7. Scanner crawl/audit (careful)

---

## Out of scope

- Patching Burp JAR, shadow registries, arbitrary FS/shell, shutdown Burp unless explicitly requested
