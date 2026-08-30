# Contributing

Thanks for improving **burp-mcp**.

## Quick start

1. Read [`origins.md`](origins.md) (upstream relationship).
2. Skim [`architecture.md`](architecture.md) and [`limitations.md`](limitations.md).
3. For current work: [`maintainer/phases/focus.md`](../maintainer/phases/focus.md).
4. Onboarding for agents: [`maintainer/agents/onboarding.md`](../maintainer/agents/onboarding.md).
5. Before claiming done: `./gradlew test`. Commits: [`maintainer/agents/git.md`](../maintainer/agents/git.md).

## Where docs live

| Audience | Location |
|----------|----------|
| Users / install / tools | `docs/` + root `README.md` |
| Sprint, systems, shipped receipts | `maintainer/` |
| Always-on agent rules | `.cursor/rules/` |

Do not put sprint checklists in user docs — link to `maintainer/phases/` instead.

## Upstream

Derived from [PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server). Preserve GPL notices; prefer Montoya over Swing; no Burp JAR patching; no Repeater registry. See [`maintainer/agents/rules.md`](../maintainer/agents/rules.md).

## User-facing docs to keep accurate

- [`install.md`](install.md)
- [`tools.md`](tools.md)
- [`limitations.md`](limitations.md)

When you ship a tool or change behavior, update those and add a line under [`maintainer/shipped/`](../maintainer/shipped/).
