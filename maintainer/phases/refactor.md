# Phase: repo layout refactor

> **Do not execute as one PR.** Follow waves in order. Each wave ends green ([`systems/health.md`](../systems/health.md)) before the next.

**Goal:** Easier maintenance for humans and agents — split fat files, shared **`ui/`** for all Burp Swing discovery, thin **`tools/`** registration.

**Maps:** [`agents/tree.md`](../agents/tree.md) · [`.cursor/rules/tree.mdc`](../../.cursor/rules/tree.mdc)

---

## Global constraints

- No Repeater tab registry or shadow state.
- UI discovery strategies **bundled in JAR** — no runtime download from GitHub.
- Keep Montoya/API tools separate from Swing discovery.
- User README stays install-focused; depth in **`docs/`** and **`maintainer/`**.

---

## Wave 0 — Maintainer + agent truth ✅

- [x] `maintainer/` hub, phases, shipped, systems, agents
- [x] `.cursor/rules/burp-mcp.mdc`, `tree.mdc`
- [x] Migrate root `PLAN.md` / `GAPS.md` into phases; drop root copies
- [x] `docs/`, `scripts/` stubs

**Done when:** Agents have single source of truth; README links contributors to `maintainer/`.

---

## Wave A — Shared UI platform (behavior-preserving)

1. [ ] Extract `net.portswigger.mcp.ui`: `UiSession` (select/restore), `AgentError` messages, `BurpVersionRegistry` (default strategy only at first).
2. [ ] `RepeaterUiDiscovery` delegates to `ui/` — no behavior change intended.
3. [ ] Tests still pass.

**Done when:** `./gradlew test`.

---

## Wave B — Split `Tools.kt`

1. [ ] `tools/register.kt` or per-domain files: `tools/repeater/RepeaterTools.kt`, `tools/proxy/`, etc.
2. [ ] `Tools.kt` becomes orchestrator calling `registerXxx(server, api, config)`.
3. [ ] Serializable param types colocated with domain or `schema/`.

**Done when:** `./gradlew test`; no new tool behavior unless intentional.

---

## Wave C — Burp version strategies

1. [ ] `ui/strategies/` — e.g. default + overrides when 2024.x vs 2026.x diverge.
2. [ ] MCP tab **reminder only**: extension version, Burp version, matched strategy (no installer).
3. [ ] Document supported Burp range in `docs/compatibility.md`.

**Done when:** Live smoke on target Burp versions documented in shipped.

---

## Wave D — Intruder / other Swing tools

Reuse `ui/` session + errors for Intruder tab discovery when prioritized in [`montoya-gaps.md`](montoya-gaps.md).
