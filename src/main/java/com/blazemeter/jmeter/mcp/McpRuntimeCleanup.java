package com.blazemeter.jmeter.mcp;

import com.blazemeter.jmeter.mcp.client.McpClientRegistry;
import com.blazemeter.jmeter.mcp.server.McpServerProcessManager;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.TestPlanListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stops MCP clients and managed server subprocesses when the JMeter GUI clears or
 * replaces the test plan, or when the JVM exits. Needed because config elements can
 * intentionally keep clients or servers running after {@code testEnded()}.
 */
public final class McpRuntimeCleanup implements TestPlanListener {

    private static final Logger LOG = LoggerFactory.getLogger(McpRuntimeCleanup.class);

    private static final McpRuntimeCleanup LISTENER = new McpRuntimeCleanup();

    private static volatile boolean shutdownHookRegistered;
    private static volatile boolean testPlanListenerRegistered;

    private McpRuntimeCleanup() {
    }

    /**
     * Registers JVM shutdown hook and, when the JMeter GUI is available, a
     * {@link TestPlanListener}. Safe to call repeatedly (e.g. from GUI constructors).
     */
    public static void ensureRegistered() {
        ensureShutdownHookRegistered();
        ensureTestPlanListenerRegistered();
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
        shutdownAll();
    }

    private static void ensureShutdownHookRegistered() {
        if (shutdownHookRegistered) {
            return;
        }
        synchronized (McpRuntimeCleanup.class) {
            if (shutdownHookRegistered) {
                return;
            }
            Runtime.getRuntime().addShutdownHook(new Thread(McpRuntimeCleanup::shutdownAll,
                    "mcp-plugin-shutdown"));
            shutdownHookRegistered = true;
        }
    }

    private static void ensureTestPlanListenerRegistered() {
        if (testPlanListenerRegistered) {
            return;
        }
        synchronized (McpRuntimeCleanup.class) {
            if (testPlanListenerRegistered) {
                return;
            }
            GuiPackage gui = GuiPackage.getInstance();
            if (gui == null) {
                return;
            }
            gui.addTestPlanListener(LISTENER);
            testPlanListenerRegistered = true;
        }
    }
}
