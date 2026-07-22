package com.blazemeter.jmeter.mcp;

import java.io.IOException;
import java.nio.file.Path;

/** Cross-platform helpers for spawning short-lived OS processes in tests. */
public final class TestProcesses {

  private TestProcesses() {
  }

  public static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }

  /** Starts a process that stays alive for about a minute (for shutdown/lifecycle tests). */
  public static Process startSleeper() throws IOException {
    if (isWindows()) {
      return new ProcessBuilder("cmd.exe", "/c", "ping", "-n", "61", "127.0.0.1").start();
    }
    return new ProcessBuilder("/bin/sleep", "60").start();
  }

  /**
   * Command + args for a process that starts successfully then exits immediately without
   * opening any TCP port (used to assert ready-port timeout behaviour).
   */
  public static String[] immediateExitCommandAndArgs() {
    String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    return new String[] {java, "-version"};
  }
}
