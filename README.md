# JMeter MCP Plugin

An Apache JMeter plugin that integrates the official
[Model Context Protocol (MCP) Java SDK](https://java.sdk.modelcontextprotocol.io/)
so you can load-test MCP servers from JMeter.

The plugin ships two JMeter components:

| Component | JMeter category | Purpose |
| --- | --- | --- |
| **bzm - MCP Client Config** | Config Element | Builds and shares a single `McpSyncClient` for the test run. Supports STDIO, SSE, and Streamable HTTP transports. |
| **bzm - MCP Sampler** | Sampler | Invokes operations (`ping`, `listTools`, `callTool`, `listResources`, `readResource`, `listPrompts`, `getPrompt`) against the shared client and records JMeter sample results. |

## Requirements

- Java 17+ (required by the MCP Java SDK)
- Apache JMeter 5.6.3 (or any later 5.6.x / 6.x running on Java 17)
- Maven 3.8+

## Build

```bash
mvn clean package
```

This produces a shaded JAR at:

```
target/jmeter-mcp-plugin-${project.version}.jar
```

The shaded JAR bundles the MCP SDK and Jackson 3 so no extra dependencies are
required at runtime. JMeter core and SLF4J are intentionally excluded — they
are provided by the host JMeter installation.

## Install

Copy the shaded JAR into your JMeter installation:

```bash
cp target/jmeter-mcp-plugin-${project.version}.jar "$JMETER_HOME/lib/ext/"
```

Restart JMeter. You should see:

- **Config Element → bzm - MCP Client Config**
- **Config Element → bzm - MCP Server Process** (spawn HTTP/SSE servers from the test plan)
- **Sampler → bzm - MCP Sampler**

## Usage

1. Add a **bzm - MCP Client Config** to your test plan and configure it:
   - **Variable Name** — logical key used by samplers to look up this client.
   - **Transport** — defaults to `STDIO`; also supports `STREAMABLE_HTTP` and `SSE`.
   - For STDIO (default), fill in **Command**, **Args**, and (optionally) **Env**.
   - For HTTP transports, fill in **Server URL** (e.g. `http://localhost:8080`)
     and, if needed, the **Endpoint** (defaults to `/mcp` for Streamable HTTP).
   - **Connect on test start** — when enabled, begins connect + `initialize()` in
     a background thread as soon as the test starts (JMeter still shows
     "running" immediately). When disabled (default), the client connects on the
     first sampler that references the same Variable Name.
   - Tweak request / initialization timeouts and client identity as needed.

2. Add a **Thread Group** and inside it add one or more **bzm - MCP Sampler**
   elements.
   - Set **Client Config (Variable Name)** to the same value used above
     (e.g. `mcpClient`).
   - Pick an **Operation**:
     - `PING`, `LIST_TOOLS`, `LIST_RESOURCES`, `LIST_PROMPTS` — no parameters.
     - `CALL_TOOL` — fill in **Tool Name** and a JSON **Arguments** object.
     - `READ_RESOURCE` — fill in **Resource URI**.
     - `GET_PROMPT` — fill in **Prompt Name** and (optional) JSON **Arguments**.

3. Add a listener (e.g. *View Results Tree*) to inspect responses returned by
   the MCP server.

Each successful sample splits metadata and payload:

- **Response headers** — `X-MCP-Operation` (e.g. `PING`, `CALL_TOOL`) and
  `X-MCP-Session`, a JSON object with the MCP handshake from `initialize`:
  `protocolVersion`, `capabilities`, `serverInfo`, and full `instructions`
  text (the same content the SDK used to print at INFO under
  `LifecycleInitializer`; the plugin silences that logger to WARN so it does
  not flood JMeter logs).
- **Response body** — pretty-printed JSON for the operation result only (e.g.
  ping payload, `listTools` result, or `callTool` content).

Working examples are provided under `examples/`:

| File | Transport | How to run the demo server |
| --- | --- | --- |
| [`mcp-example.jmx`](examples/mcp-example.jmx) | STDIO (default) | `npx -y @modelcontextprotocol/server-everything` |
| [`mcp-example-sse.jmx`](examples/mcp-example-sse.jmx) | SSE | Started in-plan by **bzm - MCP Server Process** (or run `npx … sse` manually) |
| [`mcp-example-streamable-http.jmx`](examples/mcp-example-streamable-http.jmx) | Streamable HTTP | Started in-plan by **bzm - MCP Server Process** (or run `npx … streamableHttp` manually) |

The HTTP examples target `http://localhost:3001` (the default port for
`server-everything`; set `PORT` in **bzm - MCP Server Process → Env** if you use another).

For SSE or Streamable HTTP, add **bzm - MCP Server Process** *above* **bzm - MCP Client Config**
in the test plan. It runs `npx -y @modelcontextprotocol/server-everything sse` (or
`streamableHttp`) and waits for the listen port. Do **not** use **bzm - MCP Client Config**
with STDIO to launch those servers — STDIO is an MCP client transport, not a generic
process launcher; `server-everything sse` speaks HTTP, not stdin/stdout MCP.

## How the lifecycle works

- The Config Element implements `TestStateListener`. On `testStarted()` it
  registers connection settings in `McpClientRegistry`.
- **Connect on test start** (off by default): when enabled, the registry
  schedules connect + `initialize()` on a background thread during
  `testStarted()` so the engine thread is not blocked. Samplers wait for that
  connect to finish if it is still in progress. When disabled, the **first**
  **bzm - MCP Sampler** that references the same **Variable Name** performs connect +
  init. Later samples reuse the same client. The client is thread-safe across
  JMeter threads.
- **Sample elapsed time** measures only the MCP operation (e.g. `PING`,
  `CALL_TOOL`). Client connect and `initialize()` are excluded from elapsed
  time; any wait to obtain the client is recorded separately as **Connect
  Time** on the sample (visible in listeners such as View Results Tree).
- Both modes keep the JMeter GUI responsive: the run timer and Stop button
  activate immediately instead of waiting for a slow STDIO `initialize()` on
  the engine thread.
- On `testEnded()` the Config Element triggers `closeGracefully()` via the
  registry and clears deferred settings for that name. When **bzm - MCP Server Process**
  started the HTTP/SSE server, the client config stops that subprocess *after*
  the client closes (JMeter listener order is not guaranteed).

## bzm - MCP Client Config parameters

| Parameter | Default | Applies to | Description |
| --- | --- | --- | --- |
| **Variable Name** | `mcpClient` | All | Registry key samplers use in **Client Config (Variable Name)**. Must match across config and sampler elements that share one client. |
| **Transport** | `STDIO` | All | How the plugin reaches the MCP server: `STDIO`, `SSE`, or `STREAMABLE_HTTP`. |
| **Connect on test start** | off | All | When enabled, connect + `initialize()` run on a background thread at `testStarted()`. When disabled, the first sampler that references this Variable Name performs connect + init. See [How the lifecycle works](#how-the-lifecycle-works). |
| **Command** | *(empty)* | `STDIO` | Executable to spawn (e.g. `npx`, `node`). Required for STDIO. |
| **Args** | *(empty)* | `STDIO` | Arguments passed to the command, space-separated. Supports single- and double-quoted tokens (e.g. `-y @modelcontextprotocol/server-everything`). |
| **Env** | *(empty)* | `STDIO` | Extra environment variables, one `KEY=value` per line. Lines starting with `#` are ignored. |
| **Server URL** | *(empty)* | `SSE`, `STREAMABLE_HTTP` | Base URL of the MCP HTTP server (e.g. `http://localhost:3001`). Required for HTTP transports. |
| **Endpoint** | *(empty)* | `SSE`, `STREAMABLE_HTTP` | Optional path override. For Streamable HTTP, leave blank to use the SDK default (`/mcp`). For SSE, set the server's SSE path when it differs from the SDK default. |
| **Client Name** | `jmeter-mcp-plugin` | All | MCP client identity sent during `initialize`. |
| **Client Version** | `0.1.0` | All | Version string paired with Client Name in `initialize`. |
| **Request Timeout (ms)** | `30000` | All | Per-request timeout for MCP RPCs and HTTP connect timeout. |
| **Init Timeout (ms)** | `30000` | All | Maximum time allowed for the `initialize` handshake. |

## bzm - MCP Sampler parameters

| Parameter | Default | Used when | Description |
| --- | --- | --- | --- |
| **Client Config (Variable Name)** | `mcpClient` | All | Must match the **Variable Name** on a **bzm - MCP Client Config** element in the test plan. |
| **Operation** | `PING` | All | MCP call to execute. See table below. |
| **Tool Name** | *(empty)* | `CALL_TOOL` | Name of the tool to invoke. Required for `CALL_TOOL`. |
| **Resource URI** | *(empty)* | `READ_RESOURCE` | URI of the resource to read. Required for `READ_RESOURCE`. |
| **Prompt Name** | *(empty)* | `GET_PROMPT` | Name of the prompt template. Required for `GET_PROMPT`. |
| **Arguments** | *(empty)* | `CALL_TOOL`, `GET_PROMPT` | JSON object of named parameters (e.g. `{"city": "London"}`). Omit or leave blank for an empty object. Invalid JSON fails the sample. |

### Operations

| Operation | Extra parameters | Description |
| --- | --- | --- |
| `PING` | — | Health check; verifies the client session is alive. |
| `LIST_TOOLS` | — | Returns tools exposed by the server. |
| `CALL_TOOL` | Tool Name, Arguments | Invokes a named tool with the given JSON arguments. Sample is marked unsuccessful if the server returns `isError=true`. |
| `LIST_RESOURCES` | — | Lists available MCP resources. |
| `READ_RESOURCE` | Resource URI | Fetches content for a resource URI. |
| `LIST_PROMPTS` | — | Lists prompt templates exposed by the server. |
| `GET_PROMPT` | Prompt Name, Arguments | Retrieves a prompt with optional JSON arguments. |

## License

This plugin is distributed under the same MIT license as the upstream MCP
Java SDK.
