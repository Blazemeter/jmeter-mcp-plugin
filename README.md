# JMeter MCP Plugin

An Apache JMeter plugin that integrates the official
[Model Context Protocol (MCP) Java SDK](https://java.sdk.modelcontextprotocol.io/)
so you can load-test MCP servers from JMeter.

The plugin ships two JMeter components:

| Component | JMeter category | Purpose |
| --- | --- | --- |
| **MCP Client Config** | Config Element | Builds and shares a single `McpSyncClient` for the test run. Supports STDIO, SSE, and Streamable HTTP transports. |
| **MCP Sampler** | Sampler | Invokes operations (`ping`, `listTools`, `callTool`, `listResources`, `readResource`, `listPrompts`, `getPrompt`) against the shared client and records JMeter sample results. |

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

- **Config Element → MCP Client Config**
- **Sampler → MCP Sampler**

## Usage

1. Add an **MCP Client Config** to your test plan and configure it:
   - **Variable Name** — logical key used by samplers to look up this client.
   - **Transport** — defaults to `STDIO`; also supports `STREAMABLE_HTTP` and `SSE`.
   - For STDIO (default), fill in **Command**, **Args**, and (optionally) **Env**.
   - For HTTP transports, fill in **Server URL** (e.g. `http://localhost:8080`)
     and, if needed, the **Endpoint** (defaults to `/mcp` for Streamable HTTP).
   - Tweak request / initialization timeouts and client identity as needed.

2. Add a **Thread Group** and inside it add one or more **MCP Sampler**
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

A working example is provided at [`examples/mcp-example.jmx`](examples/mcp-example.jmx).

## How the lifecycle works

- The Config Element implements `TestStateListener`. On `testStarted()` it
  **registers only** connection settings in `McpClientRegistry` (no blocking
  network or subprocess handshake). This keeps the JMeter GUI responsive:
  the run timer and Stop button activate immediately instead of waiting for a
  slow STDIO `initialize()` on the engine thread.
- The **first** `MCP Sampler` that references the same **Variable Name** opens
  the transport, creates an `McpSyncClient`, calls `initialize()`, and caches
  the client. That first sample's elapsed time includes connect + init; later
  samples reuse the same client. The client is thread-safe across JMeter
  threads.
- On `testEnded()` the Config Element triggers `closeGracefully()` via the
  registry and clears deferred settings for that name.

## Project layout

```
.
├── pom.xml
├── README.md
├── examples/
│   └── mcp-example.jmx
└── src/
    ├── main/java/io/github/jmeter/mcp/
    │   ├── client/
    │   │   ├── JsonMappers.java
    │   │   ├── McpClientFactory.java
    │   │   ├── McpClientRegistry.java
    │   │   ├── McpClientSettings.java
    │   │   ├── McpSdkLogSilencer.java
    │   │   └── TransportType.java
    │   ├── config/
    │   │   ├── McpClientConfig.java
    │   │   └── gui/McpClientConfigGui.java
    │   └── sampler/
    │       ├── McpOperation.java
    │       ├── McpSampler.java
    │       └── gui/McpSamplerGui.java
    └── test/java/io/github/jmeter/mcp/client/
        └── McpClientFactoryTest.java
```

## License

This plugin is distributed under the same MIT license as the upstream MCP
Java SDK.
