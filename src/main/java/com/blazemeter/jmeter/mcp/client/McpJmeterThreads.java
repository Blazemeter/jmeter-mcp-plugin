package com.blazemeter.jmeter.mcp.client;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

/**
 * Resolves the active JMeter worker thread for per-thread MCP client and server
 * lifecycle. Falls back to a single {@value #MAIN_THREAD_KEY} slot when no thread
 * context exists (for example during GUI edits or unit tests).
 */
public final class McpJmeterThreads {

    /** Registry / server slot used outside a running thread group. */
    public static final String MAIN_THREAD_KEY = "jmeter-main";

    private McpJmeterThreads() {
    }

    public static String currentThreadKey() {
        JMeterContext ctx = JMeterContextService.getContext();
        if (ctx != null && ctx.getThread() != null) {
            String name = ctx.getThread().getThreadName();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return MAIN_THREAD_KEY;
    }

    /**
     * Zero-based thread number within the thread group, or {@code 0} when unknown.
     */
    public static int currentThreadNum() {
        JMeterContext ctx = JMeterContextService.getContext();
        if (ctx != null && ctx.getThread() != null) {
            return ctx.getThread().getThreadNum();
        }
        return 0;
    }
}
