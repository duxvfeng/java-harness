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
 * Notification hook handler
 * <p>
 * Handles notification event logging.
 * </p>
 */
public class NotificationHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);
    private static final String NOTIFICATION = "Notification";

    @Override
    public String getEventName() {
        return NOTIFICATION;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.debug("Notification event: {}", input.sessionId());

        // Log notification to file
        logNotificationToFile(input);

        return HookOutput.allow();
    }

    /**
     * Log notification to event log file
     */
    private void logNotificationToFile(HookInput input) throws IOException {
        Path logDir = Paths.get(input.cwd())
            .resolve(".claude")
            .resolve("logs");

        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        Path logFile = logDir.resolve("notifications.log");

        String logEntry = String.format(
            "[%s] Session: %s | Tool: %s | Event: Notification%n",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            input.sessionId(),
            input.toolName() != null ? input.toolName() : "N/A"
        );

        Files.writeString(logFile, logEntry,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);
    }
}
