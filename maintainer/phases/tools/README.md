# Tool coverage ledgers (by Burp area)

**Audience:** maintainers and agents.  
**Goal:** make Burp as remotely controllable as we safely can over MCP — without boiling the ocean in one PR.

Public tool reference stays in [`docs/tools.md`](../../../docs/tools.md). Design spikes for a single area may still live as sibling phase docs (e.g. [`../repeater-ui.md`](../repeater-ui.md)).

---

## How to use

1. Pick an **area file** below (or add a new one).
2. Add or update rows in the tables when you invent or ship a tool.
3. Implement **one group / one slice** at a time; mark **Status** when done; receipt in [`../../shipped/`](../../shipped/).
4. Prefer **Montoya**; use **Swing** only when the API cannot list/read/mutate the UI (Repeater tabs, Notes, groups, Scope table chrome, …).
5. Never patch Burp’s JAR; no shadow registries as source of truth.

---

## Status legend

| Status | Meaning |
|--------|---------|
| `done` | Shipped in this repo (MCP tool registered + tested) |
| `todo` | Wanted; not implemented yet |
| `blocked` | Desired but blocked (no API, unsafe, or needs spike) |
| `skip` | Explicitly out of scope for MCP |

In prose you can write ✅ / — / ✗; in tables prefer the tokens above so agents can grep.

---

## Table template (copy into a new area file)

```markdown
# <Burp area> — MCP tool ledger

**Suite tab / UI:** …
**Related:** …

## Done / backlog

### <Group name>

| MCP tool | Capability | Path | Status | Notes |
|----------|------------|------|--------|-------|
| `snake_case_name` | Short what | Montoya / Swing | todo | |
```

**Columns**

| Column | Content |
|--------|---------|
| MCP tool | Proposed or shipped `snake_case` name |
| Capability | What the agent can do |
| Path | `Montoya` · `Swing` · `Both` · `Config JSON` |
| Status | `done` / `todo` / `blocked` / `skip` |
| Notes | Caveats, approval gates, Burp version sensitivity |

---

## Adding a new area file

1. Copy [`_TEMPLATE.md`](./_TEMPLATE.md) → `maintainer/phases/tools/<area>.md` (filename = concept: `proxy.md`, `intruder.md`, … — **not** `tabs/`). Or paste the table template above.
2. Fill groups + rows; link it from the index table below.
3. Optionally add a one-line pointer from [`../focus.md`](../focus.md) when that area is “Now”.

---

## Area index

| Area file | Burp UI | Priority |
|-----------|---------|----------|
| [`repeater.md`](./repeater.md) | Repeater (+ focused editors) | **Starter** |
| [`target.md`](./target.md) | Target → Scope, Site map, Issues | **Starter** |
| `proxy.md` | Proxy | stub when needed |
| `intruder.md` | Intruder | stub when needed |
| `organizer.md` | Organizer | stub when needed |
| `sequencer.md` | Sequencer | stub when needed |
| `decoder.md` | Decoder | stub when needed |
| `comparer.md` | Comparer | stub when needed |
| `logger.md` | Logger | stub when needed |
| `collaborator.md` | Collaborator (Pro) | stub when needed |
| `scanner.md` | Scanner / crawl / audit (Pro) | stub when needed |
| `http.md` | Suite-wide HTTP send / cookies / batch | stub when needed |
| `config.md` | Project/user options, engine | stub when needed |

Cross-cutting Montoya API notes: [`../montoya-gaps.md`](../montoya-gaps.md) (points here for detail).

---

## Implementation checklist (per tool)

- [ ] Row exists in the area ledger (`todo` → work starts)
- [ ] Register via `mcpTool` / catalog seed ([`ToolCatalog`](../../../src/main/kotlin/net/portswigger/mcp/tools/ToolCatalog.kt))
- [ ] Unit test where mockable; live-smoke for Swing/UI
- [ ] [`docs/tools.md`](../../../docs/tools.md) (+ area user doc if any)
- [ ] Status → `done`; [`shipped/`](../../shipped/) one-liner
