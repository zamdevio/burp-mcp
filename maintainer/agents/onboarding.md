# Onboarding

**Audience:** Contributors and coding agents.  
**Time:** ~30–60 minutes to ship a safe vertical slice.

User install docs: root **`README.md`**. Tool reference: **`docs/tools.md`**.

---

## Mental model

| Layer | Path | Change behavior here? |
|-------|------|------------------------|
| **MCP tools** | `tools/` | Register handlers; keep thin |
| **Montoya + security** | `tools/`, `security/` | API calls, approvals |
| **Swing discovery** | `repeater/` → `ui/` | Burp UI only |
| **MCP transport** | `KtorServerManager.kt` | SSE server |
| **Config UI** | `config/` | MCP tab in Burp |
| **DTOs / JSON** | `schema/` | Serializable shapes |
| **Plans** | `maintainer/phases/` | When — not a substitute for code |
| **Maps** | `maintainer/systems/` | How — today |

**Golden rule:** No Repeater tab registry. Burp UI is source of truth.

---

## Day 0 — environment

```bash
./gradlew test
./gradlew embedProxyJar
```

Load `build/libs/burp-mcp-all.jar` in Burp for UI work.

---

## Reading order (~8 files)

| # | Doc |
|---|-----|
| 1 | [`phases/focus.md`](../phases/focus.md) |
| 2 | [`phases/in-progress.md`](../phases/in-progress.md) — handoff / what's blocked |
| 3 | [`phases/repeater-east-sidebar.md`](../phases/repeater-east-sidebar.md) if Notes or east rail |
| 4 | [`phases/repeater-ui.md`](../phases/repeater-ui.md) if touching Repeater Swing |
| 5 | [`systems/overview.md`](../systems/overview.md) |
| 6 | [`systems/health.md`](../systems/health.md) |
| 7 | [`agents/tree.md`](tree.md) |
| 8 | [`agents/rules.md`](rules.md) |
| 9 | [`shipped/README.md`](../shipped/README.md) |
| 10 | [`agents/live-smoke.md`](live-smoke.md) after tool / Repeater UI changes |
| 11 | [`phases/tools/`](../phases/tools/) when adding or planning MCP tools |
| 12 | [`phases/montoya-gaps.md`](../phases/montoya-gaps.md) short Montoya index (detail in `tools/`) |

---

## Before commit

See [`git.md`](git.md). Code changes: `./gradlew test`. UI discovery: Repeater smoke in Burp + note version in PR or `shipped/`.
