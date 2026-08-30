# Maintainer directory

Contributor-only planning and systems maps. **`README.md`** at repo root is user-facing install/usage; **`maintainer/`** is not published as end-user docs.

**Agent guides:** [`agents/README.md`](agents/README.md) · **Onboarding:** [`agents/onboarding.md`](agents/onboarding.md)

---

## Layout

| Need | Where |
|------|--------|
| **What to build next** | [`phases/focus.md`](phases/focus.md) |
| **Open plans** | [`phases/`](phases/) — Repeater, Montoya gaps, refactor |
| **What not to redo** | [`shipped/README.md`](shipped/README.md) |
| **How subsystems wire** | [`systems/README.md`](systems/README.md) |
| **Repo tree rules** | [`agents/tree.md`](agents/tree.md) · [`.cursor/rules/tree.mdc`](../.cursor/rules/tree.mdc) |
| **Git** | [`agents/git.md`](agents/git.md) · [`.cursor/rules/git.mdc`](../.cursor/rules/git.mdc) |

**Engine truth:** Montoya/API logic and MCP registration in **`tools/`** (split over time). Swing/Burp UI discovery in **`repeater/`** → future shared **`ui/`**. Ktor MCP transport in **`KtorServerManager`**. Config UI in **`config/`**.

**Scratch:** **`maintainer/temp/`** (gitignored — never commit plans or probes here).

---

## Entrypoints

| Doc | Role |
|-----|------|
| [`phases/focus.md`](phases/focus.md) | **Current sprint** |
| [`phases/repeater-ui.md`](phases/repeater-ui.md) | Repeater MCP + Swing discovery |
| [`phases/montoya-gaps.md`](phases/montoya-gaps.md) | API tools not yet exposed |
| [`phases/refactor.md`](phases/refactor.md) | Repo layout waves (`ui/`, split `Tools.kt`) |
| [`phases/mcp-tab-ui.md`](phases/mcp-tab-ui.md) | Tool catalog in the Burp MCP tab |
| [`phases/agent-guides.md`](phases/agent-guides.md) | Cursor / Claude / terminal client docs |
| [`shipped/README.md`](shipped/README.md) | Shipped receipts |
| [`systems/health.md`](systems/health.md) | Build/test gates |
