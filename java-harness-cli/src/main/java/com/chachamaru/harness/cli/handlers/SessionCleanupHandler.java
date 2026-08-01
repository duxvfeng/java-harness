package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * SessionCleanup hook handler
 * <p>
 * Handles temporary file cleanup on session end.
 * </p>
 */
public class SessionCleanupHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(SessionCleanupHandler.class);
    private static final String SESSION_CLEANUP = "SessionCleanup";

    @Override
    public String getEventName() {
        return SESSION_CLEANUP;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("Session cleanup for: {}", input.sessionId());

        int filesCleaned = cleanupTempFiles(input);

        log.debug("Cleaned {} temporary files for session: {}", filesCleaned, input.sessionId());

        return new HookOutput(
            SESSION_CLEANUP,
            "allow",
            null,
            "Cleaned " + filesCleaned + " temporary files"
        );
    }

    /**
     * Cleanup temporary files for session
     */
    private int cleanupTempFiles(HookInput input) throws IOException {
        Path tempDir = Paths.get(input.cwd())
            .resolve(".claude")
            .resolve("temp")
            .resolve(input.sessionId());

        if (!Files.exists(tempDir)) {
            return 0;
        }

        try (Stream<Path> paths = Files.walk(tempDir)) {
            return (int) paths
                .filter(Files::isRegularFile)
                .peek(file -> {
                    try {
                        Files.deleteIfExists(file);
                        log.debug("Deleted temp file: {}", file);
                    } catch (IOException e) {
                        log.warn("Failed to delete temp file: {}", file);
                    }
                })
                .count();
        }
    }
}
