package com.blazemeter.jmeter.mcp;

/**
 * Tears down MCP clients, managed servers, and any leftover {@code npx}/{@code node}
 * children spawned by integration tests so the Surefire fork JVM can exit.
 */
final class McpTestProcessCleanup {

  private McpTestProcessCleanup() {
  }

  static void run() {
    McpRuntimeCleanup.shutdownAll();
    destroyOrphanMcpProcesses();
  }

  private static void destroyOrphanMcpProcesses() {
    try {
      ProcessHandle.current()
          .descendants()
          .forEach(
              ph ->
                  ph.info()
                      .commandLine()
                      .ifPresent(
                          cmd -> {
                            if (isMcpExternalProcess(cmd)) {
                              ph.destroyForcibly();
                            }
                          }));
    } catch (RuntimeException ignored) {
      // best effort
    }
  }

  private static boolean isMcpExternalProcess(String commandLine) {
    return commandLine.contains("server-everything")
        || (commandLine.contains("modelcontextprotocol")
            && (commandLine.contains("node") || commandLine.contains("npx")));
  }
}
