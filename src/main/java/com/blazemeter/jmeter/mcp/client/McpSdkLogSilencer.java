package com.blazemeter.jmeter.mcp.client;

/**
 * The MCP Java SDK logs the full {@code initialize} handshake (including long {@code instructions}
 * markdown) at INFO from {@code io.modelcontextprotocol.client.LifecycleInitializer}. That
 * duplicates what we surface in {@link com.blazemeter.jmeter.mcp.sampler.McpSampler} response
 * bodies and clutters JMeter's log. The same logger also emits WARN with full stack traces for
 * expected stream teardown (server process stopped while SSE / Streamable HTTP is open). When
 * Log4j2 Core is present (standard in JMeter), we raise that logger to ERROR once per JVM.
 */
final class McpSdkLogSilencer {

  private static final String LIFECYCLE_INITIALIZER_LOGGER =
      "io.modelcontextprotocol.client.LifecycleInitializer";

  private static volatile boolean applied;

  private McpSdkLogSilencer() {

  }

  static void ensureApplied() {
    if (applied) {
      return;
    }
    synchronized (McpSdkLogSilencer.class) {
      if (applied) {
        return;
      }
      try {
        Class<?> configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator");
        Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
        Object error = levelClass.getField("ERROR").get(null);
        configurator
            .getMethod("setLevel", String.class, levelClass)
            .invoke(null, LIFECYCLE_INITIALIZER_LOGGER, error);
      } catch (Throwable ignored) {
        // No Log4j2 Core, or security manager — sampler still returns full JSON.
      }
      applied = true;
    }
  }
}
