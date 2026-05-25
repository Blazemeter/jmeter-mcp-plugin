package com.blazemeter.jmeter.mcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.blazemeter.jmeter.mcp.client.McpClientFactory;
import com.blazemeter.jmeter.mcp.client.McpJmeterThreads;
import com.blazemeter.jmeter.mcp.client.McpThreadScopedSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops MCP server child processes per JMeter worker thread (e.g.
 * {@code npx server-everything sse} on {@code basePort + threadNum}).
 */
public final class McpServerProcessManager {

    private static final Logger LOG = LoggerFactory.getLogger(McpServerProcessManager.class);

    private static final McpServerProcessManager INSTANCE = new McpServerProcessManager();

    private static final ScheduledExecutorService DEFERRED_STOP_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "mcp-server-deferred-stop");
                t.setDaemon(true);
                return t;
            });

    /** Fallback delay when no MCP Client Config stops the managed process. */
    public static final long DEFERRED_STOP_FALLBACK_MS = 500;

    private final Map<String, Process> processes = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> deferredStops = new ConcurrentHashMap<>();

    private McpServerProcessManager() {
    }

    public static McpServerProcessManager getInstance() {
        return INSTANCE;
    }

    /**
     * Start a subprocess for the current thread and block until {@code host:port}
     * accepts TCP connections or {@code startupWaitMs} elapses.
     */
    public void start(String command, String args, String envRaw,
                      String readyHost, int readyPort, long startupWaitMs) {
        int threadNum = McpJmeterThreads.currentThreadNum();
        int listenPort = McpThreadScopedSettings.serverPortForThread(readyPort, threadNum);
        Map<String, String> env = McpThreadScopedSettings.serverEnvForPort(envRaw, listenPort);
        startForThread(
                McpJmeterThreads.currentThreadKey(),
                command,
                args,
                env,
                readyHost,
                listenPort,
                startupWaitMs);
    }

    void startForThread(String threadKey, String command, String args,
                        Map<String, String> env, String readyHost, int listenPort,
                        long startupWaitMs) {
        stop(threadKey);
        String trimmedCommand = command == null ? "" : command.trim();
        if (trimmedCommand.isEmpty()) {
            throw new IllegalArgumentException("MCP server command must not be empty");
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(trimmedCommand);
        cmd.addAll(McpClientFactory.splitArgs(args));

        ProcessBuilder builder = new ProcessBuilder(cmd);
        if (env != null && !env.isEmpty()) {
            builder.environment().putAll(env);
        }
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        LOG.info("Starting MCP server process on thread '{}': {} (PORT={})",
                threadKey, cmd, listenPort);
        try {
            Process p = builder.start();
            processes.put(threadKey, p);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to start MCP server process: " + cmd, ex);
        }

        String host = (readyHost == null || readyHost.isBlank()) ? "localhost" : readyHost.trim();
        LOG.info("Waiting for MCP server at {}:{} on thread '{}' (timeout {} ms)",
                host, listenPort, threadKey, startupWaitMs);
        if (!waitForPort(host, listenPort, startupWaitMs)) {
            stop(threadKey);
            throw new RuntimeException(
                    "MCP server did not become reachable at " + host + ":" + listenPort
                            + " within " + startupWaitMs + " ms (thread '" + threadKey + "')");
        }
        LOG.info("MCP server is reachable at {}:{} on thread '{}'", host, listenPort, threadKey);
    }

    /** Whether the current thread owns a running subprocess. */
    public boolean isManagedProcessRunning() {
        return processes.containsKey(McpJmeterThreads.currentThreadKey());
    }

    /**
     * Schedule stopping every thread-owned subprocess after {@code delayMs}.
     */
    public void scheduleDeferredStopAll(long delayMs) {
        DEFERRED_STOP_EXECUTOR.schedule(this::stopAll, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedule process stop for the current thread after {@code delayMs}.
     */
    public void scheduleDeferredStop(long delayMs) {
        scheduleDeferredStop(McpJmeterThreads.currentThreadKey(), delayMs);
    }

    void scheduleDeferredStop(String threadKey, long delayMs) {
        if (!processes.containsKey(threadKey)) {
            return;
        }
        cancelDeferredStop(threadKey);
        deferredStops.put(threadKey, DEFERRED_STOP_EXECUTOR.schedule(() -> {
            if (processes.containsKey(threadKey)) {
                LOG.info("Stopping MCP server process on thread '{}' (deferred)", threadKey);
                stop(threadKey);
            }
        }, delayMs, TimeUnit.MILLISECONDS));
    }

    private void cancelDeferredStop(String threadKey) {
        ScheduledFuture<?> pending = deferredStops.remove(threadKey);
        if (pending != null) {
            pending.cancel(false);
        }
    }

    /** Stop the subprocess owned by the current thread. */
    public void stop() {
        stop(McpJmeterThreads.currentThreadKey());
    }

    public void stop(String threadKey) {
        cancelDeferredStop(threadKey);
        Process p = processes.remove(threadKey);
        if (p == null) {
            return;
        }
        LOG.info("Stopping MCP server process on thread '{}' (pid {})", threadKey, p.pid());
        p.destroy();
        try {
            if (!p.waitFor(5, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                p.waitFor(5, TimeUnit.SECONDS);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        }
    }

    /** Stop every thread-owned subprocess (safety net on {@code testEnded()}). */
    public void stopAll() {
        for (String threadKey : new ArrayList<>(processes.keySet())) {
            stop(threadKey);
        }
    }

    private static boolean waitForPort(String host, int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isPortOpen(host, port, 500)) {
                return true;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return isPortOpen(host, port, 500);
    }

    private static boolean isPortOpen(String host, int port, int connectTimeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
