package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PermissionDenied hook handler
 * <p>
 * Handles permission denied event logging.
 * </p>
 */
public class PermissionDeniedHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PermissionDeniedHandler.class);
    private static final String PERMISSION_DENIED = "PermissionDenied";

    @Override
    public String getEventName() {
        return PERMISSION_DENIED;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.warn("Permission denied for session: {}, tool: {}",
            input.sessionId(), input.toolName());

        // Log denied event to file
        logDeniedEventToFile(input);

        return HookOutput.allow();
    }

    /**
     * Log denied event to security log
     */
    private void logDeniedEventToFile(HookInput input) throws IOException {
        Path logDir = Paths.get(input.cwd())
            .resolve(".claude")
            .resolve("logs");

        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        Path logFile = logDir.resolve("denied-permissions.log");

        String logEntry = String.format(
            "[%s] Session: %s | Tool: %s | Path: %s | DENIED%n",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            input.sessionId(),
            input.toolName() != null ? input.toolName() : "N/A",
            input.cwd()
        );

        Files.writeString(logFile, logEntry,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }
}
