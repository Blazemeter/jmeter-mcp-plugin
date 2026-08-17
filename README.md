# BlazeMeter MCP Plugin for JMeter

---

<picture>
 <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/Blazemeter/jmeter-bzm-commons/refs/heads/master/src/main/resources/dark-theme/blazemeter-by-perforce-logo.png">
 <img src="https://raw.githubusercontent.com/Blazemeter/jmeter-bzm-commons/refs/heads/master/src/main/resources/light-theme/blazemeter-by-perforce-logo.png">
</picture>

Apache JMeter plugin that integrates the official [Model Context Protocol (MCP) Java SDK](https://java.sdk.modelcontextprotocol.io/) so you can exercise MCP servers from JMeter. Use it for **functional checks**, **automated regression tests** (assertions, CI pipelines, non-GUI runs), **exploratory debugging** in the GUI, or **load and soak testing** when you scale threads — the same elements cover every scenario. The plugin shares a single MCP client across threads, invokes server operations (`ping`, `listTools`, `callTool`, and more), and records each call as a JMeter sample result.

The plugin ships three JMeter components:

| Component | JMeter category | Purpose |
| --- | --- | --- |
| **bzm - MCP Client Config** | Config Element | Builds and shares a single `McpSyncClient` for the test run. Supports STDIO, SSE, and Streamable HTTP transports. |
| **bzm - MCP Server Process** | Config Element | Spawns an MCP HTTP/SSE server subprocess from the test plan (for example `npx @modelcontextprotocol/server-everything sse`). |
| **bzm - MCP Sampler** | Sampler | Invokes MCP operations against the shared client and records JMeter sample results. |

> [!IMPORTANT]
> Requires **Java 17+** (required by the MCP Java SDK).

> [!NOTE]
> **Compatibility:** Use **Java** and **Apache JMeter** versions that are **supported together** for the JMeter release you run. Through **JMeter 5.6.3**, JMeter does **not** support **Java** versions **newer than 21** — use **Java 17** or **Java 21** with those JMeter lines unless your vendor documents otherwise. For newer JMeter versions, follow Apache’s prerequisites in [Getting Started](https://jmeter.apache.org/usermanual/get-started.html) and the release notes for that version.

## Index

* **Install**
  * Prerequisites
  * Installation using Plugins Manager
  * Updating
  * Manual installation
  * Verifying the installation
* **How the plugin works**
  * Concept
  * Client lifecycle
* **Creating the test plan**
  * Option A: STDIO (local subprocess)
  * Option B: SSE or Streamable HTTP (in-plan server)
  * Option C: SSE or Streamable HTTP (external server)
* **MCP Client Config**
* **MCP Server Process**
* **MCP Sampler**
  * Operations
* **Results**
* **Examples**
* **Building from source**
* **License**

## Prerequisites

1. **Apache JMeter** installed and runnable with a **Java** version that matches that JMeter release (see the **Important** / **Note** alerts at the top of this README).
2. **JMeter Plugins Manager** installed inside JMeter. Plugins Manager is the usual way to add and update community plugins such as this one.

> [!NOTE]
> Install this plugin via **Plugins Manager** when possible. Start from the prerequisites, then install, verify, and update as needed.

## Installation using Plugins Manager

1. Start **JMeter** and open **Plugins Manager** (typically **Options → Plugins Manager** depending on your JMeter build).
2. Open the **Available Plugins** tab and search for **MCP** or **BlazeMeter MCP**.
3. Select it, click **Apply Changes and Restart JMeter**, and wait for the install to finish.

## Updating

If the plugin is already installed, open **Plugins Manager → Installed Plugins**, find the same entry, and install updates when offered (then restart JMeter when prompted), same as other Plugins Manager extensions.

## Manual installation

Use this path when Plugins Manager is not an option—for example offline installs.

1. Open **[Releases](https://github.com/Blazemeter/jmeter-mcp-plugin/releases)** for this repository and choose the plugin version that matches your JMeter line (the **Latest** tag is usually the right default).
2. Under **Assets** for that release, download **`jmeter-mcp-plugin-<version>.jar`**.
3. Copy **`jmeter-mcp-plugin-<version>.jar`** into **`<JMETER_HOME>/lib/ext`**.
4. **Restart JMeter**.

The shaded JAR bundles the MCP SDK and Jackson 3 so no extra dependencies are required at runtime. JMeter core and SLF4J are intentionally excluded — they are provided by the host JMeter installation.

To build the JAR yourself from this repository, see **Building from source** below.

## Verifying the installation

1. In **Plugins Manager → Installed Plugins**, confirm the BlazeMeter MCP plugin is listed.
2. In a test plan, check that these elements are available:
   - **Add → Config Element → bzm - MCP Client Config**
   - **Add → Config Element → bzm - MCP Server Process**
   - **Add → Sampler → bzm - MCP Sampler**

## How the plugin works

### Concept

This plugin hides MCP transport and session details behind familiar JMeter elements. A **bzm - MCP Client Config** element registers connection settings and owns one shared MCP client for the test run. **bzm - MCP Sampler** elements reference that client by **Variable Name** and execute MCP operations; each operation becomes a sample with JSON response data and MCP metadata in response headers.

Typical uses:

- **Smoke / functional testing** — one sampler (or a short sequence) with a Thread Group at 1 thread; inspect results in View Results Tree or fail the run with assertions.
- **Automated tests** — save a `.jmx` test plan, add Response Assertions or JSON assertions on tool output, and run headless with `jmeter -n -t plan.jmx -l results.jtl` in CI.
- **Load testing** — increase threads and iterations to stress concurrent MCP sessions and measure latency and throughput under load.

Supported transports:

- **STDIO** — spawn a local MCP server process and communicate over stdin/stdout (typical for CLI-based MCP servers).
- **SSE** — connect to an MCP server over Server-Sent Events at a base URL.
- **Streamable HTTP** — connect to an MCP server over Streamable HTTP (default endpoint `/mcp`).

For HTTP-based demos, **bzm - MCP Server Process** can start a reference MCP server (for example `@modelcontextprotocol/server-everything`) before the client connects.

### Client lifecycle

- The Config Element implements `TestStateListener`. On `testStarted()` it registers connection settings in `McpClientRegistry`.
- **Connect on test start** (off by default): when enabled, the registry schedules connect + `initialize()` on a background thread during `testStarted()` so the engine thread is not blocked. Samplers wait for that connect to finish if it is still in progress. When disabled, the **first** **bzm - MCP Sampler** that references the same **Variable Name** performs connect + init. Later samples reuse the same client. The client is thread-safe across JMeter threads.
- **Sample elapsed time** measures only the MCP operation (for example `PING`, `CALL_TOOL`). Client connect and `initialize()` are excluded from elapsed time; any wait to obtain the client is recorded separately as **Connect Time** on the sample (visible in listeners such as View Results Tree).
- Both modes keep the JMeter GUI responsive: the run timer and Stop button activate immediately instead of waiting for a slow STDIO `initialize()` on the engine thread.
- On `testEnded()` the Config Element triggers `closeGracefully()` via the registry and clears deferred settings for that name (unless **Keep server running after test ends** is enabled). When the managed HTTP/SSE server process is stopped, any remaining HTTP MCP clients are closed first so the long-lived SSE/stream is torn down cleanly before the subprocess is killed.

## Creating the test plan

### Option A: STDIO (local subprocess)

1. Add **bzm - MCP Client Config** to your test plan (for example under the Thread Group or at test-plan level).
2. Set **Transport** to **STDIO** and configure **Command**, **Args**, and optionally **Env** (for example `npx` with `-y @modelcontextprotocol/server-everything`).
3. Create a **Thread Group** and add one or more **bzm - MCP Sampler** elements.
4. Set **Client Config (Variable Name)** on each sampler to match the config element’s **Variable Name** (default `mcpClient`).
5. Pick an **Operation** and fill in any required fields (see **MCP Sampler** below).
6. Add a listener (for example *View Results Tree*) to inspect responses.

### Option B: SSE or Streamable HTTP (in-plan server)

1. Add **bzm - MCP Server Process** *above* **bzm - MCP Client Config** in the test plan. Configure it to run the HTTP/SSE MCP server (for example `npx -y @modelcontextprotocol/server-everything sse` or `streamableHttp`).
2. Add **bzm - MCP Client Config** with **Transport** set to **SSE** or **STREAMABLE_HTTP** and **Server URL** pointing at the listen address (for example `http://localhost:3001`).
3. Add a **Thread Group** with **bzm - MCP Sampler** elements as in Option A.

> [!NOTE]
> Do **not** use **bzm - MCP Client Config** with **STDIO** to launch HTTP/SSE servers — STDIO is an MCP client transport, not a generic process launcher; `server-everything sse` speaks HTTP, not stdin/stdout MCP.

### Option C: SSE or Streamable HTTP (external server)

Same as Option B, but run the MCP HTTP/SSE server yourself (outside JMeter or in another process) and point **Server URL** at it. Skip **bzm - MCP Server Process** if the server is already running.

After any option, add assertions to validate MCP responses, timers to pace calls, and listeners or result files as needed — whether the plan is a one-off check or an automated suite.

## MCP Client Config

Add with **Add → Config Element → bzm - MCP Client Config**.

| **Field** | **Description** | **Default** |
| --- | --- | --- |
| **Variable Name** | Registry key samplers use in **Client Config (Variable Name)**. Must match across config and sampler elements that share one client. | `mcpClient` |
| **Transport** | How the plugin reaches the MCP server: `STDIO`, `SSE`, or `STREAMABLE_HTTP`. | `STDIO` |
| **Connect on test start** | When enabled, connect + `initialize()` run on a background thread at `testStarted()`. When disabled, the first sampler that references this Variable Name performs connect + init. | off |
| **Command** | Executable to spawn (for example `npx`, `node`). Required for STDIO. | *(empty)* |
| **Args** | Arguments passed to the command, space-separated. Supports single- and double-quoted tokens (for example `-y @modelcontextprotocol/server-everything`). | *(empty)* |
| **Env** | Extra environment variables, one `KEY=value` per line. Lines starting with `#` are ignored. | *(empty)* |
| **Server URL** | Base URL of the MCP HTTP server (for example `http://localhost:3001`). Required for SSE and Streamable HTTP. | *(empty)* |
| **Endpoint** | Optional path override. For Streamable HTTP, leave blank to use the SDK default (`/mcp`). For SSE, set the server’s SSE path when it differs from the SDK default. | *(empty)* |
| **Client Name** | MCP client identity sent during `initialize`. | `jmeter-mcp-plugin` |
| **Client Version** | Version string paired with Client Name in `initialize`. | `0.1.0` |
| **Request Timeout (ms)** | Per-request timeout for MCP RPCs and HTTP connect timeout. | `30000` |
| **Init Timeout (ms)** | Maximum time allowed for the `initialize` handshake. | `30000` |

## MCP Server Process

Add with **Add → Config Element → bzm - MCP Server Process**. Place it **above** **bzm - MCP Client Config** when it should start the server before the client connects.

Use it to run commands such as:

- `npx -y @modelcontextprotocol/server-everything sse`
- `npx -y @modelcontextprotocol/server-everything streamableHttp`

The element waits for the listen port before the test proceeds. Set **Env** (for example `PORT=3001`) when the server should bind to a non-default port; the HTTP examples under `examples/` target `http://localhost:3001` by default.

## MCP Sampler

Add with **Add → Sampler → bzm - MCP Sampler**.

| **Field** | **Description** | **Default** |
| --- | --- | --- |
| **Client Config (Variable Name)** | Must match the **Variable Name** on a **bzm - MCP Client Config** element in the test plan. | `mcpClient` |
| **Operation** | MCP call to execute. See **Operations** below. | `PING` |
| **Tool Name** | Name of the tool to invoke. Required for `CALL_TOOL`. | *(empty)* |
| **Resource URI** | URI of the resource to read. Required for `READ_RESOURCE`. | *(empty)* |
| **Prompt Name** | Name of the prompt template. Required for `GET_PROMPT`. | *(empty)* |
| **Arguments** | JSON object of named parameters (for example `{"city": "London"}`). Used for `CALL_TOOL` and `GET_PROMPT`. Omit or leave blank for an empty object. Invalid JSON fails the sample. | *(empty)* |

### Operations

| **Operation** | **Extra fields** | **Description** |
| --- | --- | --- |
| `PING` | — | Health check; verifies the client session is alive. |
| `LIST_TOOLS` | — | Returns tools exposed by the server. |
| `CALL_TOOL` | Tool Name, Arguments | Invokes a named tool with the given JSON arguments. Sample is marked unsuccessful if the server returns `isError=true`. |
| `LIST_RESOURCES` | — | Lists available MCP resources. |
| `READ_RESOURCE` | Resource URI | Fetches content for a resource URI. |
| `LIST_PROMPTS` | — | Lists prompt templates exposed by the server. |
| `GET_PROMPT` | Prompt Name, Arguments | Retrieves a prompt with optional JSON arguments. |

## Results

You can set listeners to evaluate the results of your tests. The **View Results Tree** listener displays each MCP sampler sample so you can inspect requests and responses.

Each successful sample splits metadata and payload:

- **Response headers** — `X-MCP-Operation` (for example `PING`, `CALL_TOOL`) and `X-MCP-Session`, a JSON object with the MCP handshake from `initialize`: `protocolVersion`, `capabilities`, `serverInfo`, and full `instructions` text (the same content the SDK used to print at INFO under `LifecycleInitializer`; the plugin silences that logger to ERROR so it does not flood JMeter logs).
- **Response body** — pretty-printed JSON for the operation result only (for example ping payload, `listTools` result, or `callTool` content).

**Connect Time** on the sample reflects any wait to obtain or initialize the shared client; **Elapsed** reflects only the MCP operation itself.

## Examples

Working examples are provided under `examples/`:

| File | Transport | How to run the demo server |
| --- | --- | --- |
| [`mcp-example.jmx`](examples/mcp-example.jmx) | STDIO (default) | `npx -y @modelcontextprotocol/server-everything` |
| [`mcp-example-sse.jmx`](examples/mcp-example-sse.jmx) | SSE | Started in-plan by **bzm - MCP Server Process** (or run `npx … sse` manually) |
| [`mcp-example-streamable-http.jmx`](examples/mcp-example-streamable-http.jmx) | Streamable HTTP | Started in-plan by **bzm - MCP Server Process** (or run `npx … streamableHttp` manually) |

## Building from source

Uses **Maven 3.8+** from the repository root:

```bash
mvn clean package
```

This produces a shaded JAR at:

```
target/jmeter-mcp-plugin-${project.version}.jar
```

Artifacts land under **`target/`**. Compilation uses the **`jmeter.version`** declared in **`pom.xml`**; at runtime install the packaged JAR against the JMeter build you intend to run and validate with a short smoke plan.

## License

Distributed under the **Apache License 2.0**. See **`LICENSE`** in this repository.
