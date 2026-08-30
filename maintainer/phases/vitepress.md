# Phase: VitePress docs site (CF Pages)

**Status:** Planned — run at **publish** (or immediately before) once public Markdown is in good shape.  
**Not now:** scaffolding `apps/docs` during Repeater/cleanup sprints.

**Reference implementation:** [`~/Tools/gform`](file:///home/amf/Tools/gform) — especially `apps/docs/`, root `docs/`, `pnpm docs:*`, and CF Pages deploy. Copy **structure and workflow**, not brand/theme verbatim (burp-mcp gets its own palette/logo).

---

## Problem

Repo already has user Markdown under **`docs/`** (install, tools, architecture, …). That is fine for GitHub browsing, but hosting a real docs site needs:

- VitePress (or similar) app, sidebar/nav, search, clean URLs
- Deploy target (Cloudflare Pages)
- Clear split: **canonical Markdown** vs **site shell**

Without a phase, publish work tends to invent a one-off layout that drifts from gform and from `maintainer/` vs `docs/` boundaries.

---

## Goals

1. **Canonical content stays in root `docs/`** — human-editable, linked from README; agents keep writing here.
2. **Site app at `apps/docs/`** — VitePress config, theme, public assets, sidebar; synced content under `apps/docs/content/` (gitignored or generated — match gform).
3. **Deploy to Cloudflare Pages** — build artifact from `apps/docs`; custom domain later.
4. **Same maintainer habits as gform** — manual sidebar updates when pages change; `docs:sync` / `docs:dev` / `docs:build` / `docs:deploy` from repo root (or Gradle/npm hybrid — decide at scaffold time; prefer **pnpm** scripts mirroring gform if the monorepo gains `package.json`).
5. **No maintainer leak** — `maintainer/**` stays off the public site (same rule as today).

---

## Non-goals (this phase)

- Rewriting all tool docs before the site exists
- Porting gform CLI branding / cyan theme as-is
- Auto-generating every page from Kotlin (optional later; MCP tab catalog is a separate phase)

---

## Follow gform (checklist)

| gform | burp-mcp target |
|-------|-----------------|
| Root `docs/` = source of truth | Keep / grow `docs/` |
| `apps/docs/` VitePress app | `apps/docs/` |
| `srcDir: 'content'`, sync from `docs/` | Same |
| Manual `.vitepress/sidebar.ts` | Same |
| `pnpm docs:sync` / `docs:dev` / `docs:build` / `docs:deploy` | Same script names where practical |
| CF Pages (e.g. `*.pages.dev`) | New project for burp-mcp |
| Custom home `apps/docs/index.md` (layout home) | Product landing distinct from `docs/README.md` |

**Mention in PRs / shipped notes:** “Docs site patterned on gform `apps/docs`.”

---

## Content IA (public site)

Align sidebar with **Burp / MCP domains**, not a vague `tabs/` bucket (see discussion in cleanup chats). Suggested v1 groups:

| Sidebar group | Source under `docs/` (evolve toward) |
|---------------|--------------------------------------|
| Start | install, guides, origins |
| Tools by area | `tools.md` index + flat pages (`repeater.md`, later `proxy.md`, …) — **no** `docs/tabs/` |
| Concepts | architecture, limitations |
| Contribute | contributing |

---

## When to execute

| Trigger | Action |
|---------|--------|
| Cleanup / pre-commit docs pass | Shape `docs/` IA only — **no** `apps/docs` required yet |
| Publish / public launch | Scaffold `apps/docs`, sync, CF Pages, ship URL |
| After MCP tab catalog / agent-guides | Wire sidebar deep links to guides + tool pages |

---

## Acceptance

- [ ] `apps/docs` builds locally; sidebar covers install + at least Repeater + limitations
- [ ] Sync from root `docs/` is documented and idempotent
- [ ] CF Pages deploy green; `maintainer/` not in published output
- [ ] Phase closed in `shipped/` with site URL

---

## Related

- [`agent-guides.md`](./agent-guides.md) — client setup pages under `docs/guides/`
- [`mcp-tab-ui.md`](./mcp-tab-ui.md) — in-Burp catalog; link out to site later
- [`focus.md`](./focus.md) — sprint queue
- gform: `apps/docs/README.md`, `docs/README.md`
