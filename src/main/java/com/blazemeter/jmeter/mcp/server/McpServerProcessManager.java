package com.blazemeter.jmeter.mcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.blazemeter.jmeter.mcp.McpRuntimeCleanup;
import com.blazemeter.jmeter.mcp.client.McpClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts and stops MCP server child processes (e.g. {@code npx server-everything sse})
 * without acting as an MCP client. HTTP/SSE servers must be launched this way, not via
 * {@link io.modelcontextprotocol.client.transport.StdioClientTransport}.
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

    private volatile Process process;
    private volatile ScheduledFuture<?> deferredStop;

    private McpServerProcessManager() {
    }

    public static McpServerProcessManager getInstance() {
        McpRuntimeCleanup.ensureRegistered();
        return INSTANCE;
    }

    /**
     * Start a subprocess and block until {@code host:port} accepts TCP connections or
     * {@code startupWaitMs} elapses. When the ready endpoint is already reachable, the
     * existing listener is reused and any managed process is left running.
     */
    public void start(String command, String args, String envRaw,
                      String readyHost, int readyPort, long startupWaitMs) {
        String trimmedCommand = command == null ? "" : command.trim();
        if (trimmedCommand.isEmpty()) {
            throw new IllegalArgumentException("MCP server command must not be empty");
        }

        String host = (readyHost == null || readyHost.isBlank()) ? "localhost" : readyHost.trim();
        if (isPortOpen(host, readyPort, 500)) {
            if (process != null && process.isAlive()) {
                LOG.info("Reusing managed MCP server at {}:{} (pid {})", host, readyPort, process.pid());
            } else {
                LOG.info("MCP server already reachable at {}:{}; reusing existing listener",
                        host, readyPort);
            }
            cancelDeferredStop();
            return;
        }

        stop();

        List<String> cmd = new ArrayList<>();
        cmd.add(trimmedCommand);
        cmd.addAll(McpClientFactory.splitArgs(args));

        ProcessBuilder builder = new ProcessBuilder(cmd);
        Map<String, String> env = McpClientFactory.parseEnv(envRaw);
        if (!env.isEmpty()) {
            builder.environment().putAll(env);
        }
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        LOG.info("Starting MCP server process: {}", cmd);
        try {
            process = builder.start();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to start MCP server process: " + cmd, ex);
        }

        LOG.info("Waiting for MCP server at {}:{} (timeout {} ms)", host, readyPort, startupWaitMs);
        if (!waitForPort(host, readyPort, startupWaitMs)) {
            stop();
            throw new RuntimeException(
                    "MCP server did not become reachable at " + host + ":" + readyPort
                            + " within " + startupWaitMs + " ms");
        }
        LOG.info("MCP server is reachable at {}:{}", host, readyPort);
    }

    /** Whether this manager currently owns a subprocess started via {@link #start}. */
    public boolean isManagedProcessRunning() {
        return process != null;
    }

    /** Whether the managed subprocess reference is still alive. */
    public boolean isManagedServerAlive() {
        Process p = process;
        return p != null && p.isAlive();
    }

    /** PID of the managed subprocess, or {@code null} if none is running. */
    public Long getManagedProcessPid() {
        Process p = process;
        return p != null && p.isAlive() ? p.pid() : null;
    }

    /**
     * Schedule process stop after {@code delayMs}. Used from {@code testEnded()} when listener
     * order may run before HTTP clients close; cancelled when {@link #stop()} runs.
     */
    public void scheduleDeferredStop(long delayMs) {
        if (process == null) {
            cancelDeferredStop();
            return;
        }
        cancelDeferredStop();
        deferredStop = DEFERRED_STOP_EXECUTOR.schedule(() -> {
            if (process != null) {
                LOG.info("Stopping MCP server process (deferred)");
                stop();
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public void cancelDeferredStop() {
        ScheduledFuture<?> pending = deferredStop;
        deferredStop = null;
        if (pending != null) {
            pending.cancel(false);
        }
    }

    public void stop() {
        cancelDeferredStop();
        Process p = process;
        process = null;
        if (p == null) {
            return;
        }
        LOG.info("Stopping MCP server process (pid {})", p.pid());
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

    /** Force-stop the managed server and cancel any deferred stop. */
    public void shutdownAll() {
        LOG.info("Shutting down managed MCP server process");
        stop();
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

    /** Whether {@code host:port} accepts a TCP connection (for GUI / Start Now checks). */
    public static boolean isPortOpen(String host, int port, int connectTimeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
