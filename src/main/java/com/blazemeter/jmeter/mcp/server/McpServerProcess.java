package com.blazemeter.jmeter.mcp.server;

import org.apache.jmeter.config.ConfigElement;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMeter Configuration Element that spawns an MCP server subprocess (for example {@code npx
 * -y @modelcontextprotocol/server-everything sse}) and waits until its listen port is open. Use a
 * separate {@link com.blazemeter.jmeter.mcp.config.McpClientConfig} with SSE or Streamable HTTP to
 * connect as a client.
 */
public class McpServerProcess extends ConfigTestElement
    implements ConfigElement, TestStateListener {

  public static final String COMMAND = "McpServerProcess.command";
  public static final String ARGS = "McpServerProcess.args";
  public static final String ENV = "McpServerProcess.env";
  public static final String READY_HOST = "McpServerProcess.readyHost";
  public static final String READY_PORT = "McpServerProcess.readyPort";
  public static final String STARTUP_WAIT_MS = "McpServerProcess.startupWaitMs";

  private static final long serialVersionUID = 1L;

    public static final String COMMAND = "McpServerProcess.command";
    public static final String ARGS = "McpServerProcess.args";
    public static final String ENV = "McpServerProcess.env";
    public static final String READY_HOST = "McpServerProcess.readyHost";
    public static final String READY_PORT = "McpServerProcess.readyPort";
    public static final String STARTUP_WAIT_MS = "McpServerProcess.startupWaitMs";
    public static final String KEEP_SERVER_RUNNING_AFTER_TEST =
            "McpServerProcess.keepServerRunningAfterTest";

  @Override
  public void addConfigElement(ConfigElement config) {
    // self-contained
  }

  @Override
  public boolean expectsModification() {
    return false;
  }

  @Override
  public void testStarted() {
    startServer();
  }

  @Override
  public void testStarted(String host) {
    startServer();
  }

  @Override
  public void testEnded() {
    stopServer();
  }

  @Override
  public void testEnded(String host) {
    stopServer();
  }

  private void startServer() {
    try {
      McpServerProcessManager.getInstance()
          .start(
              getPropertyAsString(COMMAND, ""),
              getPropertyAsString(ARGS, ""),
              getPropertyAsString(ENV, ""),
              getPropertyAsString(READY_HOST, "localhost"),
              (int) getPropertyAsLong(READY_PORT, 3001L),
              getPropertyAsLong(STARTUP_WAIT_MS, 60_000L));
    } catch (RuntimeException ex) {
      LOG.error("Failed to start MCP server process: {}", ex.getMessage(), ex);
      throw ex;
    }
  }

    @Override
    public boolean expectsModification() {
        return false;
    }

    @Override
    public void testStarted() {
        startServer();
    }

    @Override
    public void testStarted(String host) {
        startServer();
    }

    @Override
    public void testEnded() {
        stopServer();
    }

    @Override
    public void testEnded(String host) {
        stopServer();
    }

    private void startServer() {
        try {
            McpServerProcessManager.getInstance().start(
                    getPropertyAsString(COMMAND, ""),
                    getPropertyAsString(ARGS, ""),
                    getPropertyAsString(ENV, ""),
                    getPropertyAsString(READY_HOST, "localhost"),
                    (int) getPropertyAsLong(READY_PORT, 3001L),
                    getPropertyAsLong(STARTUP_WAIT_MS, 60_000L));
        } catch (RuntimeException ex) {
            LOG.error("Failed to start MCP server process: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Does not stop the subprocess immediately. {@link com.blazemeter.jmeter.mcp.config.McpClientConfig}
     * stops it after closing HTTP/SSE clients; a short deferred stop covers server-only plans.
     */
    private void stopServer() {
        McpServerProcessManager manager = McpServerProcessManager.getInstance();
        if (isKeepServerRunningAfterTest()) {
            manager.cancelDeferredStop();
            return;
        }
        manager.scheduleDeferredStop(McpServerProcessManager.DEFERRED_STOP_FALLBACK_MS);
    }

    private boolean isKeepServerRunningAfterTest() {
        return getPropertyAsBoolean(KEEP_SERVER_RUNNING_AFTER_TEST, true);
    }
}
