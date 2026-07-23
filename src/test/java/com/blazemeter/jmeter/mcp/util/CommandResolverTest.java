package com.blazemeter.jmeter.mcp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandResolverTest {

  @TempDir
  Path tempDir;

  @Test
  void shouldReturnBlankUnchangedWhenCommandIsBlank() {
    assertEquals("", CommandResolver.resolve("", true, "/bin", CommandResolver.DEFAULT_PATHEXT));
    assertEquals(
        "  ", CommandResolver.resolve("  ", true, "/bin", CommandResolver.DEFAULT_PATHEXT));
  }

  @Test
  void shouldReturnCommandUnchangedWhenNotWindows() throws Exception {
    Path cmd = tempDir.resolve("npx.cmd");
    Files.writeString(cmd, "@echo off\r\n");
    String path = tempDir.toString();

    assertEquals(
        "npx",
        CommandResolver.resolve("npx", false, path, CommandResolver.DEFAULT_PATHEXT));
  }

  @Test
  void shouldPreferPathextCmdOverExtensionlessShimWhenBothExist() throws Exception {
    Path shim = tempDir.resolve("npx");
    Files.writeString(shim, "#!/usr/bin/env bash\n");
    Path cmd = tempDir.resolve("npx.cmd");
    Files.writeString(cmd, "@echo off\r\n");

    String resolved =
        CommandResolver.resolve("npx", true, tempDir.toString(), CommandResolver.DEFAULT_PATHEXT);

    assertEquals(cmd.toRealPath().toString(), resolved);
  }

  @Test
  void shouldFollowPathextOrderWhenMultipleExtensionsExist() throws Exception {
    Path bat = tempDir.resolve("tool.bat");
    Path cmd = tempDir.resolve("tool.cmd");
    Files.writeString(bat, "@echo off\r\n");
    Files.writeString(cmd, "@echo off\r\n");

    String resolved =
        CommandResolver.resolve(
            "tool", true, tempDir.toString(), CommandResolver.DEFAULT_PATHEXT);

    assertEquals(bat.toRealPath().toString(), resolved);
  }

  @Test
  void shouldReturnOriginalWhenCommandNotFoundOnPath() {
    String resolved =
        CommandResolver.resolve(
            "definitely-missing-xyz",
            true,
            tempDir.toString(),
            CommandResolver.DEFAULT_PATHEXT);
    assertEquals("definitely-missing-xyz", resolved);
  }

  @Test
  void shouldLeavePathOrExtensionCommandsUnchanged() throws Exception {
    Path cmd = tempDir.resolve("npx.cmd");
    Files.writeString(cmd, "@echo off\r\n");
    String absolute = cmd.toAbsolutePath().toString();

    assertEquals(
        absolute,
        CommandResolver.resolve(absolute, true, tempDir.toString(), CommandResolver.DEFAULT_PATHEXT));
    assertEquals(
        "npx.cmd",
        CommandResolver.resolve(
            "npx.cmd", true, tempDir.toString(), CommandResolver.DEFAULT_PATHEXT));
    assertEquals(
        "subdir/npx",
        CommandResolver.resolve(
            "subdir/npx", true, tempDir.toString(), CommandResolver.DEFAULT_PATHEXT));
    assertEquals(
        "subdir\\npx",
        CommandResolver.resolve(
            "subdir\\npx", true, tempDir.toString(), CommandResolver.DEFAULT_PATHEXT));
  }

  @Test
  void shouldTrimCommandBeforeResolving() throws Exception {
    Path cmd = tempDir.resolve("npx.cmd");
    Files.writeString(cmd, "@echo off\r\n");

    String resolved =
        CommandResolver.resolve(
            "  npx  ", true, tempDir.toString(), CommandResolver.DEFAULT_PATHEXT);

    assertEquals(cmd.toRealPath().toString(), resolved);
  }

  @Test
  void shouldUseDefaultPathextWhenPathextEnvIsNull() throws Exception {
    Path cmd = tempDir.resolve("npx.cmd");
    Files.writeString(cmd, "@echo off\r\n");

    String resolved = CommandResolver.resolve("npx", true, tempDir.toString(), null);

    assertEquals(cmd.toRealPath().toString(), resolved);
  }

  @Test
  void shouldResolveRealNpxOnWindowsWhenPresentOnPath() {
    String os = System.getProperty("os.name", "").toLowerCase();
    if (!os.contains("win")) {
      return;
    }
    String resolved = CommandResolver.resolve("npx");
    assertTrue(
        resolved.toLowerCase().endsWith("npx.cmd") || resolved.toLowerCase().endsWith("npx.exe"),
        "expected PATHEXT resolution to npx.cmd/exe but was: " + resolved);
  }
}
