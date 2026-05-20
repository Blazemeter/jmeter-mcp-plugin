package io.github.jmeter.mcp.client;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Builds {@link McpSyncClient} instances from a declarative
 * {@link McpClientSettings} value. The factory hides the transport-specific
 * wiring so the JMeter components only need to know about settings.
 */
public final class McpClientFactory {

    private McpClientFactory() {
        // utility
    }

    /**
     * Build, initialize and return a connected sync MCP client.
     *
     * @param settings the resolved client settings
     * @return an initialized {@link McpSyncClient} that is ready to use
     */
    public static McpSyncClient buildAndInitialize(McpClientSettings settings) {
        Objects.requireNonNull(settings, "settings must not be null");

        McpClientTransport transport = buildTransport(settings);

        McpSchema.Implementation clientInfo = new McpSchema.Implementation(
                blankToDefault(settings.getClientName(), "jmeter-mcp-plugin"),
                blankToDefault(settings.getClientVersion(), "0.1.0"));

        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(clientInfo)
                .requestTimeout(Duration.ofMillis(settings.getRequestTimeoutMillis()))
                .initializationTimeout(Duration.ofMillis(settings.getInitializationTimeoutMillis()))
                .capabilities(McpSchema.ClientCapabilities.builder().build())
                .build();

        client.initialize();
        return client;
    }

    private static McpClientTransport buildTransport(McpClientSettings settings) {
        switch (settings.getTransport()) {
            case STDIO:
                return buildStdio(settings);
            case SSE:
                return buildSse(settings);
            case STREAMABLE_HTTP:
                return buildStreamableHttp(settings);
            default:
                throw new IllegalStateException("Unhandled transport: " + settings.getTransport());
        }
    }

    private static McpClientTransport buildStdio(McpClientSettings s) {
        String command = s.getStdioCommand();
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException(
                    "MCP STDIO transport requires a command (e.g. 'npx' or '/usr/local/bin/node')");
        }
        ServerParameters.Builder builder = ServerParameters.builder(command);

        List<String> args = splitArgs(s.getStdioArgs());
        if (!args.isEmpty()) {
            builder.args(args.toArray(new String[0]));
        }

        Map<String, String> env = parseEnv(s.getStdioEnv());
        if (!env.isEmpty()) {
            builder.env(env);
        }

        McpJsonMapper jsonMapper = JsonMappers.getDefault();
        return new StdioClientTransport(builder.build(), jsonMapper);
    }

    private static McpClientTransport buildSse(McpClientSettings s) {
        String url = requireUrl(s, "SSE");
        HttpClientSseClientTransport.Builder builder = HttpClientSseClientTransport.builder(url)
                .jsonMapper(JsonMappers.getDefault())
                .connectTimeout(Duration.ofMillis(s.getRequestTimeoutMillis()));
        if (s.getEndpoint() != null && !s.getEndpoint().isBlank()) {
            builder.sseEndpoint(s.getEndpoint());
        }
        return builder.build();
    }

    private static McpClientTransport buildStreamableHttp(McpClientSettings s) {
        String url = requireUrl(s, "Streamable HTTP");
        HttpClientStreamableHttpTransport.Builder builder =
                HttpClientStreamableHttpTransport.builder(url)
                        .jsonMapper(JsonMappers.getDefault())
                        .connectTimeout(Duration.ofMillis(s.getRequestTimeoutMillis()));
        if (s.getEndpoint() != null && !s.getEndpoint().isBlank()) {
            builder.endpoint(s.getEndpoint());
        }
        return builder.build();
    }

    private static String requireUrl(McpClientSettings s, String label) {
        String url = s.getServerUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException(
                    "MCP " + label + " transport requires a server URL");
        }
        return url;
    }

    static List<String> splitArgs(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        // Simple shell-like split that respects double and single quotes.
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (Character.isWhitespace(c) && !inSingle && !inDouble) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    static Map<String, String> parseEnv(String raw) {
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        Map<String, String> env = new HashMap<>();
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = trimmed.substring(0, eq).trim();
            String value = trimmed.substring(eq + 1).trim();
            if (!key.isEmpty()) {
                env.put(key, value);
            }
        }
        return env;
    }

    private static String blankToDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
