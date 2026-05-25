package com.blazemeter.jmeter.mcp.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies per-thread offsets so each JMeter worker connects to its own MCP server
 * instance (for example {@code basePort + threadNum} for HTTP transports).
 */
public final class McpThreadScopedSettings {

    private McpThreadScopedSettings() {
    }

    /**
     * Returns a copy of {@code base} with transport-specific fields adjusted for
     * {@code threadNum} (thread 0 keeps the configured port / URL).
     */
    public static McpClientSettings forThread(McpClientSettings base, int threadNum) {
        McpClientSettings copy = copyOf(base);
        if (threadNum <= 0) {
            return copy;
        }
        if (copy.getTransport() == TransportType.SSE
                || copy.getTransport() == TransportType.STREAMABLE_HTTP) {
            copy.setServerUrl(offsetHttpUrl(copy.getServerUrl(), threadNum));
        }
        return copy;
    }

    /**
     * Base listen port for an MCP server process plus the thread offset.
     */
    public static int serverPortForThread(int basePort, int threadNum) {
        return basePort + Math.max(0, threadNum);
    }

    /**
     * Ensures {@code PORT} is set in the server environment map for the given listen port.
     */
    public static Map<String, String> serverEnvForPort(String envRaw, int listenPort) {
        Map<String, String> env = new LinkedHashMap<>(McpClientFactory.parseEnv(envRaw));
        env.putIfAbsent("PORT", String.valueOf(listenPort));
        return env;
    }

    static String offsetHttpUrl(String url, int threadOffset) {
        if (url == null || url.isBlank() || threadOffset <= 0) {
            return url;
        }
        try {
            URI uri = new URI(url.trim());
            int port = uri.getPort();
            if (port < 0) {
                port = defaultPort(uri.getScheme());
            }
            int newPort = port + threadOffset;
            URI adjusted = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    newPort,
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());
            return adjusted.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid MCP server URL: " + url, ex);
        }
    }

    private static int defaultPort(String scheme) {
        if ("https".equalsIgnoreCase(scheme)) {
            return 443;
        }
        return 80;
    }

    private static McpClientSettings copyOf(McpClientSettings base) {
        McpClientSettings copy = new McpClientSettings();
        copy.setName(base.getName());
        copy.setTransport(base.getTransport());
        copy.setServerUrl(base.getServerUrl());
        copy.setEndpoint(base.getEndpoint());
        copy.setStdioCommand(base.getStdioCommand());
        copy.setStdioArgs(base.getStdioArgs());
        copy.setStdioEnv(base.getStdioEnv());
        copy.setClientName(base.getClientName());
        copy.setClientVersion(base.getClientVersion());
        copy.setRequestTimeoutMillis(base.getRequestTimeoutMillis());
        copy.setInitializationTimeoutMillis(base.getInitializationTimeoutMillis());
        copy.setConnectOnStartup(base.isConnectOnStartup());
        return copy;
    }
}
