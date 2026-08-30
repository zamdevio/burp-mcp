<div align="center">

  <h1>burp-mcp</h1>

  [![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](./LICENSE)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Burp Montoya](https://img.shields.io/badge/Montoya-2025.10-FF6633)](https://portswigger.net/burp/extender)
  [![MCP](https://img.shields.io/badge/MCP-Model%20Context%20Protocol-000000)](https://modelcontextprotocol.io/)

  <p><strong>Burp Suite, driven by MCP agents.</strong></p>
  <p>
    A Burp extension that exposes proxy history, Repeater, Intruder, HTTP send,
    Collaborator, config, and more to AI clients over the Model Context Protocol.
  </p>
  <p>
    <a href="https://github.com/zamdevio/burp-mcp">GitHub</a> ·
    <a href="#install">Install</a> ·
    <a href="docs/tools.md">Tools</a> ·
    <a href="docs/README.md">Docs</a> ·
    <a href="#origin">Origin</a>
  </p>
</div>

> **Note:** Treat this README as aspirational until the work in [`maintainer/phases/`](maintainer/phases/) is complete — descriptions here may be ahead of what is shipped and verified.

---

Connect Burp Suite to MCP clients (SSE or stdio). Agents can send HTTP, read proxy and WebSocket history, manage Repeater tabs, push to Intruder, query scanner issues and Collaborator (Pro), and read or write project/user options — with Burp’s approval and config gates where configured.

| Surface | Path | Role |
|---------|------|------|
| **Repository** | [github.com/zamdevio/burp-mcp](https://github.com/zamdevio/burp-mcp) | Source, issues, releases |
| **Install & usage** | this README | Build JAR, load in Burp, connect clients |
| **Tool reference** | [`docs/tools.md`](docs/tools.md) | MCP tool catalog |
| **Repeater** | [`docs/repeater.md`](docs/repeater.md) | Tabs, Send, Target gate |
| **Agent setup** | [`docs/guides/`](docs/guides/) | Cursor, Claude Desktop, Claude Code, SSE/stdio |
| **Architecture** | [`docs/architecture.md`](docs/architecture.md) | Montoya vs Swing, transport |
| **Limitations** | [`docs/limitations.md`](docs/limitations.md) | Edition and UI constraints |
| **Contributing** | [`docs/contributing.md`](docs/contributing.md) · [`maintainer/`](maintainer/) | Contributors and agents |

**Use only against targets you own or are explicitly authorized to test.**

---

## Origin

This project is based on PortSwigger’s official extension:

**[PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server)**

We forked that codebase as the baseline, then extended and reorganized it for agent-driven Burp workflows:

| Layer | What changed |
|-------|----------------|
| **Upstream baseline** | In-Burp MCP server (Ktor SSE), stdio proxy packaging, Claude Desktop installer, Montoya-backed HTTP / proxy / Intruder / Collaborator / config tools |
| **Repeater depth** | Live Swing discovery — list, read, write, select, rename, and **Send** individual tabs (`repeater-tab-N`); blocks Send when Target is not specified |
| **Maintainer layout** | `maintainer/` phases & systems, `.cursor/rules/`, `docs/` for shared planning and reference |
| **Distribution** | Published as **[zamdevio/burp-mcp](https://github.com/zamdevio/burp-mcp)**; build artifact **`burp-mcp-all.jar`** |

Upstream remains PortSwigger’s canonical product. This tree is a maintained derivative with stronger Repeater control and clearer docs for agents.

More detail: [`docs/origins.md`](docs/origins.md).

---

## Why this extension

Burp already has a powerful UI. Agents need a **stable MCP surface** into Montoya and the live suite UI where the API stops short.

- **Montoya-first** for HTTP, history, send-to-Repeater/Intruder, config, Collaborator.
- **Swing discovery** for Repeater tab strip and shared editors when Montoya cannot enumerate tabs.
- **Security gates** — HTTP send and sensitive reads respect approval settings and auto-approve lists in the MCP tab.

---

## Capability overview

| Area | What agents can do | Notes | Edition |
|------|-------------------|-------|---------|
| **HTTP** | Send HTTP/1.1 and HTTP/2; receive full responses | Subject to “require approval” and auto-approved targets | All |
| **Repeater** | Create tabs; list; read/write; select; rename; **Send** / send-and-wait | Live UI; index ids — re-list after changes; Target must be set before Send | All |
| **Intruder** | Send a prepared request into Intruder | Create-only today; no Intruder tab enumeration yet | All |
| **Proxy** | Read HTTP & WebSocket history (incl. regex filters); toggle intercept | History may require data-access approval | All |
| **Organizer** | List organizer items (incl. regex) | Same approval model as history when enabled | All |
| **Editors** | Read/write the **focused** message editor | Distinct from Repeater tab tools | All |
| **Config** | Export / merge project & user JSON options | Opt-in “edit config” toggle; credential filtering available | All |
| **Engine** | Pause / unpause Burp’s task execution engine | Suite-wide effect | All |
| **Scanner** | List scanner issues | — | **Pro** |
| **Collaborator** | Generate payloads; poll interactions | — | **Pro** |
| **Utilities** | URL / Base64 encode-decode; random strings | Stateless helpers | All |

Tool names and parameters: **[`docs/tools.md`](docs/tools.md)**. Repeater workflows: **[`docs/repeater.md`](docs/repeater.md)**. Constraints: **[`docs/limitations.md`](docs/limitations.md)**. In-Burp tool catalog in the MCP tab: [`maintainer/phases/mcp-tab-ui.md`](maintainer/phases/mcp-tab-ui.md).

---

## Install

### Prerequisites

- **Java** on `PATH` (`java --version`)
- **`jar`** on `PATH` (`jar --version`) — required for the embed build

### Build

```bash
git clone https://github.com/zamdevio/burp-mcp.git
cd burp-mcp
./gradlew embedProxyJar
```

Output: **`build/libs/burp-mcp-all.jar`**

### Load in Burp

1. **Extensions** → **Add** → type **Java**
2. Select `build/libs/burp-mcp-all.jar`
3. Open the **MCP** suite tab → enable the server

Step-by-step: [`docs/install.md`](docs/install.md). Client wiring: [`docs/guides/`](docs/guides/) *(in progress)*.

---

## Configuration

In Burp’s **MCP** tab:

| Control | Purpose |
|---------|---------|
| **Enabled** | Start / stop the MCP HTTP+SSE server |
| **Enable tools that can edit your config** | Allows `set_project_options` / `set_user_options` |
| **Require approval for HTTP requests** | Prompt before outbound HTTP tools |
| **Require approval for project data access** | Prompt before history / organizer reads; optional always-allow sub-options |
| **Filter config credentials** | Redact sensitive fields in exported config |
| **Auto-approved HTTP targets** | Hosts/domains allowed without per-request approval |
| **Advanced → host / port** | Default **`127.0.0.1`**, port **`9876`** |

Connect clients to:

```text
http://127.0.0.1:9876
```

### Claude Desktop (stdio)

Use **Install to Claude Desktop** in the MCP tab, or run the packaged proxy. Upstream proxy: [PortSwigger/mcp-proxy](https://github.com/PortSwigger/mcp-proxy).

```bash
/path/to/burp/java -jar /path/to/mcp-proxy-all.jar --sse-url http://127.0.0.1:9876
```

---

## Documentation map

| Need | Doc |
|------|-----|
| Install & load | [docs/install.md](docs/install.md) |
| MCP tools | [docs/tools.md](docs/tools.md) |
| Repeater | [docs/repeater.md](docs/repeater.md) |
| Agent clients (Cursor, Claude, …) | [docs/guides/](docs/guides/) |
| How it is built | [docs/architecture.md](docs/architecture.md) |
| What can go wrong | [docs/limitations.md](docs/limitations.md) |
| Fork / upstream | [docs/origins.md](docs/origins.md) |
| Contribute | [docs/contributing.md](docs/contributing.md) |
| Sprint / phases | [maintainer/phases/focus.md](maintainer/phases/focus.md) |

---

## Development

```bash
./gradlew test
./gradlew embedProxyJar
```

- Planning: [`maintainer/`](maintainer/)
- Cursor rules: [`.cursor/rules/`](.cursor/rules/)

---

## License

[GPL-3.0](LICENSE) — same family as the upstream PortSwigger MCP server extension. See [`docs/origins.md`](docs/origins.md) and [PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server).
