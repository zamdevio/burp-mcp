# Install

## Prerequisites

- A supported **Burp Suite** install (Community or Professional; Pro required for scanner / Collaborator tools).
- **Java** available on `PATH` (`java --version`).
- **`jar`** available on `PATH` (`jar --version`) — used by the Gradle embed task.

## Build the extension JAR

From the repository root:

```bash
./gradlew embedProxyJar
```

Produces:

```text
build/libs/burp-mcp-all.jar
```

That JAR embeds the MCP stdio proxy used by clients that cannot speak SSE directly.

Optional checks:

```bash
./gradlew test
```

## Load in Burp Suite

1. Open Burp → **Extensions** → **Installed** → **Add**.
2. Extension type: **Java**.
3. Select `build/libs/burp-mcp-all.jar`.
4. Confirm the extension loads without errors in the **Output** / **Errors** panes.
5. Open the suite tab named **MCP**.
6. Enable the server. Note host/port under **Advanced** (defaults: host **`127.0.0.1`**, port **`9876`**).

Reload the JAR after rebuilding if you already had an older copy loaded.

## Connect an MCP client

### SSE (many IDE / agent hosts)

Point the client at:

```text
http://127.0.0.1:9876
```

Client guides: [`guides/README.md`](guides/README.md).

### Stdio proxy (e.g. Claude Desktop)

Prefer the **Install to Claude Desktop** action in the MCP tab when available. Manual form:

```bash
/path/to/burp/java -jar /path/to/mcp-proxy-all.jar --sse-url http://127.0.0.1:9876
```

The stdio proxy is fetched at build time into `build/proxy/mcp-proxy-all.jar` and embedded in the extension JAR. Source: [PortSwigger/mcp-proxy](https://github.com/PortSwigger/mcp-proxy) (binary vendored upstream in [PortSwigger/mcp-server](https://github.com/PortSwigger/mcp-server)).

## After connect

1. List tools in the client — you should see HTTP, Repeater, proxy history, utilities, and (on Pro) Collaborator / scanner tools.
2. Prefer **`list_repeater_tabs`** before tab-targeted reads if you work with Repeater.
3. Enable **config edit** in the MCP tab only when you intentionally allow `set_*_options`.

See [`tools.md`](tools.md) and [`limitations.md`](limitations.md).
