package com.blazemeter.jmeter.mcp;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

/**
 * Registers one JVM-wide cleanup hook with the JUnit root store so MCP clients and
 * managed servers are torn down when the Surefire fork session ends.
 */
public final class McpTestCleanupExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    context
        .getRoot()
        .getStore(Namespace.GLOBAL)
        .getOrComputeIfAbsent(
            McpTestCleanupExtension.class,
            type ->
                (ExtensionContext.Store.CloseableResource) () -> McpRuntimeCleanup.shutdownAll());
  }
}
