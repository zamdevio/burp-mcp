# Phase: Montoya API coverage gaps

**Prefer API-first**; use Swing only where Montoya cannot list/read UI.

**Detail ledgers (source of truth for done/todo by Burp area):** [`tools/README.md`](./tools/README.md) — start with [`tools/repeater.md`](./tools/repeater.md) and [`tools/target.md`](./tools/target.md).

This file stays a **short Montoya-oriented index**. When you add or ship a tool, update the matching `tools/<area>.md` row (and `shipped/`).

---

## Already exposed (summary)

| Area | Ledger | Highlights |
|------|--------|------------|
| HTTP | [`tools/http.md`](./tools/http.md) *(stub later)* | `send_http1_request`, `send_http2_request` |
| Repeater | [`tools/repeater.md`](./tools/repeater.md) | Create + Swing list/read/write/Send |
| Target / Scope | [`tools/target.md`](./tools/target.md) | `is_in_scope`, include/exclude; table CRUD todo |
| Proxy | *(stub)* | History + intercept get/set |
| Organizer / Scanner / Collaborator / Config | *(stub)* | See [`docs/tools.md`](../../docs/tools.md) |

---

## High-value Montoya still open

| Montoya API | Suggested MCP tool(s) | Ledger |
|-------------|----------------------|--------|
| `SiteMap.requestResponses()` | `get_sitemap_entries` | [`target.md`](./tools/target.md) |
| `SiteMap.add(...)` | `add_to_sitemap` | [`target.md`](./tools/target.md) |
| `Http.cookieJar()` | cookie helpers | `http.md` later |
| `Http.sendRequests` (batch) | batch send | `http.md` later |
| `Decoder.sendToDecoder` | `send_to_decoder` | `decoder.md` later |
| `Comparer.sendToComparer` | `send_to_comparer` | `comparer.md` later |

Shipped recently: intercept state, burp version, scope query/include/exclude, project info — see Target / Proxy rows in ledgers.

---

## Medium / UI-heavy

| Area | Gap | Ledger |
|------|-----|--------|
| **Repeater** | Notes, groups, close tab | [`repeater.md`](./tools/repeater.md) |
| **Target Scope table** | List/edit/remove/enable/subdomains | [`target.md`](./tools/target.md) |
| **Intruder** | Tab list/read | `intruder.md` later |
| **Sequencer / Logger** | No Montoya module | stub or skip |

---

## Scanner (Pro, guardrails)

| API | Suggested tools | Risk |
|-----|-----------------|------|
| `Scanner.startCrawl` | `start_crawl` | Long-running |
| `Scanner.startAudit` | `start_audit` | Active scan |
| `Scanner.generateReport` | file write — path policy | |

---

## Skip for MCP (default)

| API | Why |
|-----|-----|
| `Ai.prompt()` | External agents already |
| `Bambda.importBambda` | Code execution surface |

---

## Out of scope

- Patching Burp JAR, shadow registries, arbitrary FS/shell, shutdown Burp unless explicitly requested
