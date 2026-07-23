package com.blazemeter.jmeter.mcp.util;

import com.helger.commons.annotation.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Resolves a bare executable name the way Windows {@code cmd} does with {@code PATHEXT}, so
 * Java {@link ProcessBuilder} can spawn commands such as {@code npx} without a {@code cmd /c}
 * wrapper.
 *
 * <p>On Windows, {@code CreateProcess} (used by {@code ProcessBuilder}) only auto-appends
 * {@code .exe}. Shells additionally try each {@code PATHEXT} extension ({@code .CMD},
 * {@code .BAT}, …). This class performs that lookup and returns an absolute path when found.
 *
 * <p>Extensionless PATH hits (for example Node's bash shim named {@code npx}) are skipped on
 * purpose: they match {@code where} but are not valid Win32 images for {@code CreateProcess}.
 * Non-Windows platforms return the command unchanged (POSIX {@code exec} already resolves
 * PATH and shebangs).
 */
public final class CommandResolver {

  /**
   * Fallback extensions when {@code PATHEXT} is unset or empty. Matches the usual Windows
   * default (uppercase, as reported by {@code System.getenv("PATHEXT")}).
   */
  public static final List<String> DEFAULT_PATHEXT_EXTENSIONS =
      List.of(".COM", ".EXE", ".BAT", ".CMD");

  /**
   * {@link #DEFAULT_PATHEXT_EXTENSIONS} joined with {@code ;}, same shape as the {@code PATHEXT}
   * environment variable.
   */
  public static final String DEFAULT_PATHEXT = String.join(";", DEFAULT_PATHEXT_EXTENSIONS);

  /** Process {@code PATH}, read once (does not change for the JVM lifetime). */
  private static final String PATH_ENV = System.getenv("PATH");

  /** Process {@code PATHEXT}, read once (does not change for the JVM lifetime). */
  private static final String PATHEXT_ENV = System.getenv("PATHEXT");

  private CommandResolver() {
  }

  /**
   * Resolve {@code command} for the current OS and process environment.
   *
   * @param command executable name or path from MCP {@code command} (not a full shell line)
   * @return absolute path when Windows PATHEXT resolution succeeds; otherwise {@code command}
   */
  public static String resolve(String command) {
    return resolve(command, isWindows(), PATH_ENV, PATHEXT_ENV);
  }

  /**
   * Same as {@link #resolve(String)} with injectable OS/env for tests.
   *
   * @param command executable name or path
   * @param windows whether to apply Windows PATHEXT search
   * @param pathEnv {@code PATH} value (may be {@code null})
   * @param pathextEnv {@code PATHEXT} value (may be {@code null})
   * @return resolved executable path or the original {@code command}
   */
  @VisibleForTesting
  static String resolve(String command, boolean windows, String pathEnv, String pathextEnv) {
    if (command == null || command.isBlank()) {
      return command;
    }
    String trimmed = command.trim();
    if (!windows) {
      return trimmed;
    }
    List<String> extensions = pathextExtensions(pathextEnv);
    if (hasDirectorySeparator(trimmed) || hasPathextExtension(trimmed, extensions)) {
      return trimmed;
    }
    String resolved = findOnPath(trimmed, pathEnv, extensions);
    return resolved != null ? resolved : trimmed;
  }

  private static String findOnPath(
      String command, String pathEnv, List<String> extensions) {
    if (pathEnv == null || pathEnv.isBlank()) {
      return null;
    }
    for (String dir : pathEnv.split(File.pathSeparator, -1)) {
      if (dir.isBlank()) {
        continue;
      }
      Path directory = Path.of(dir);
      for (String ext : extensions) {
        Path found = findCandidate(directory, command, ext);
        if (found != null) {
          return toResolvedPath(found);
        }
      }
    }
    return null;
  }

  /**
   * Locate {@code command + ext} under {@code directory}. Tries the PATHEXT spelling first
   * (typically uppercase on Windows), then a lower-case extension so resolution also works on
   * case-sensitive filesystems when simulating Windows in tests.
   */
  private static Path findCandidate(Path directory, String command, String ext) {
    Path exact = directory.resolve(command + ext);
    if (Files.isRegularFile(exact)) {
      return exact;
    }
    String lowerExt = ext.toLowerCase(Locale.ROOT);
    if (!lowerExt.equals(ext)) {
      Path lower = directory.resolve(command + lowerExt);
      if (Files.isRegularFile(lower)) {
        return lower;
      }
    }
    return null;
  }

  private static String toResolvedPath(Path candidate) {
    try {
      return candidate.toRealPath().toString();
    } catch (IOException ex) {
      return candidate.toAbsolutePath().normalize().toString();
    }
  }

  private static List<String> pathextExtensions(String pathextEnv) {
    if (pathextEnv == null || pathextEnv.isBlank()) {
      return DEFAULT_PATHEXT_EXTENSIONS;
    }
    List<String> extensions = new ArrayList<>();
    for (String part : pathextEnv.split(";", -1)) {
      String ext = part.trim();
      if (ext.isEmpty()) {
        continue;
      }
      if (!ext.startsWith(".")) {
        ext = "." + ext;
      }
      extensions.add(ext);
    }
    return extensions.isEmpty() ? DEFAULT_PATHEXT_EXTENSIONS : extensions;
  }

  private static boolean hasPathextExtension(String command, List<String> extensions) {
    String lower = command.toLowerCase(Locale.ROOT);
    for (String ext : extensions) {
      if (lower.endsWith(ext.toLowerCase(Locale.ROOT))) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasDirectorySeparator(String command) {
    return command.indexOf('/') >= 0
        || command.indexOf('\\') >= 0
        || command.indexOf(File.separatorChar) >= 0;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
  }
}
