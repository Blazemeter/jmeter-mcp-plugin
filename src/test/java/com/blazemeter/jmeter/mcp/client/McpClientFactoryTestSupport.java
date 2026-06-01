package com.blazemeter.jmeter.mcp.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.modelcontextprotocol.spec.McpClientTransport;

/**
 * Invokes package-private {@link McpClientFactory} transport wiring without
 * opening a live MCP session.
 */
final class McpClientFactoryTestSupport {

    private McpClientFactoryTestSupport() {
    }

    static McpClientTransport buildTransport(McpClientSettings settings) {
        try {
            Method method = McpClientFactory.class.getDeclaredMethod(
                    "buildTransport", McpClientSettings.class);
            method.setAccessible(true);
            return (McpClientTransport) method.invoke(null, settings);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
