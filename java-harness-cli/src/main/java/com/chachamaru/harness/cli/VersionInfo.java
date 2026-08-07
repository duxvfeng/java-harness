package com.chachamaru.harness.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Single source of truth for the CLI version.
 *
 * <p>Reads the version from the {@code VERSION} resource file bundled in the
 * classpath. This avoids hard-coding the version in multiple Java source files
 * and keeps it synchronized with the project {@code VERSION} file.</p>
 */
public final class VersionInfo {

    private static final String VERSION_RESOURCE = "/VERSION";
    private static final String FALLBACK_VERSION = "unknown";

    private VersionInfo() {
        // utility class
    }

    /**
     * Returns the application version, read from the bundled VERSION resource.
     *
     * @return the version string (e.g. {@code "4.1.1-java"}), or
     *         {@code "unknown"} if the resource cannot be read.
     */
    public static String getVersion() {
        try (InputStream is = VersionInfo.class.getResourceAsStream(VERSION_RESOURCE)) {
            if (is == null) {
                return FALLBACK_VERSION;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                return line != null ? line.trim() : FALLBACK_VERSION;
            }
        } catch (IOException e) {
            return FALLBACK_VERSION;
        }
    }
}
