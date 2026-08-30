# Git — commits, remotes, history

Cursor twin: [`.cursor/rules/git.mdc`](../../.cursor/rules/git.mdc).

## Remotes

| Remote | Repo | Use |
|--------|------|-----|
| **origin** | [zamdevio/burp-mcp](https://github.com/zamdevio/burp-mcp) | Push / PRs |
| **upstream** | [PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server) | Fetch and cherry-pick; never push |

`main` is protected by convention: no force-push except a user-requested history rewrite.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/) — `type(scope): subject`, not inverted (`chore(init):`, not `Initial(chore):`).

```text
type(scope): subject

<body: why this change exists>
```

| type | Use |
|------|-----|
| `feat` | User-visible capability |
| `fix` | Bug fix |
| `docs` | Docs only |
| `chore` | Repo/meta (init, ignore, BApp metadata) |
| `refactor` | No intended behavior change |
| `test` | Tests only |
| `ci` | GitHub Actions / CI |
| `build` | Gradle / toolchain |

Examples:

```text
feat(repeater): list and read individual Repeater tabs

Montoya cannot enumerate Repeater tabs; agents need live UI reads.
```

```text
fix(config): bind MCP server to 127.0.0.1 by default

Local-only listen is the safe default for desktop Burp.
```

```text
chore(init): seed burp-mcp from PortSwigger mcp-server
```

- Imperative subject after the colon; no trailing period.
- Body explains **why**; skip only for trivial one-liners.
- Do not dump `git status` into the message.

## Do not commit

- Secrets, `.env`, Burp project/session exports, Collaborator secrets
- `build/`, `.gradle/`, `.kotlin/` (gitignore)
- Vendored `libs/` (stdio proxy is fetched to `build/proxy/`)
- `maintainer/temp/*` scratch — keep `.gitkeep` only

## Gates

1. User asked for the commit.
2. `./gradlew test` when the change is code (docs-only may skip).
3. No `--no-verify` unless the user says so.

## Origin of this tree

First commit is a **new history**. The snapshot is a GPL-3.0 **fork/copy** of PortSwigger’s MCP server plus local Repeater tools, maintainer layout, and docs. Attribution: [`docs/origins.md`](../../docs/origins.md), `LICENSE`, `BappDescription.html`.
