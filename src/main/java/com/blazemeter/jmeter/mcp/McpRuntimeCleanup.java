package com.blazemeter.jmeter.mcp;

import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.TestPlanListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops MCP clients and managed server subprocesses when the JMeter GUI clears or
 * replaces the test plan, or when the JVM exits. Needed because
 * {@link com.blazemeter.jmeter.mcp.config.McpClientConfig} can intentionally keep
 * servers running after {@code testEnded()}.
 */
public final class McpRuntimeCleanup implements TestPlanListener {

    private static final Logger LOG = LoggerFactory.getLogger(McpRuntimeCleanup.class);

    private static final McpRuntimeCleanup LISTENER = new McpRuntimeCleanup();

    private static volatile boolean registered;

    private McpRuntimeCleanup() {
    }

    public static void ensureRegistered() {
        if (registered) {
            return;
        }
        synchronized (McpRuntimeCleanup.class) {
            if (registered) {
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(McpRuntimeCleanup::shutdownAll,
                    "mcp-plugin-shutdown"));
            GuiPackage gui = GuiPackage.getInstance();
            if (gui != null) {
                gui.addTestPlanListener(LISTENER);
            }
            registered = true;
        }
    }

    public static void shutdownAll() {
        LOG.info("Shutting down MCP plugin runtime (clients and managed server)");
        McpClientRegistry.getInstance().shutdownAll();
        McpServerProcessManager.getInstance().shutdownAll();
    }

    @Override
    public void beforeTestPlanCleared() {
        shutdownAll();
    }

    @Override
    public void afterTestPlanCleared() {
        // no-op
    }

    @Override
    public void testPlanLoaded() {
        // beforeTestPlanCleared runs when replacing a plan
    }
}
