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
 * SessionSummary hook handler
 * <p>
 * Handles session summary generation to session-log.md.
 * </p>
 */
public class SessionSummaryHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(SessionSummaryHandler.class);
    private static final String SESSION_SUMMARY = "SessionSummary";

    @Override
    public String getEventName() {
        return SESSION_SUMMARY;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("Session summary for: {}", input.sessionId());

        // Append to session log
        appendToSessionLog(input);

        return HookOutput.allow();
    }

    /**
     * Append session summary to session-log.md
     */
    private void appendToSessionLog(HookInput input) throws IOException {
        Path logFile = Paths.get(input.cwd()).resolve("session-log.md");

        String summary = String.format("""

            ## Session: %s
            **Time**: %s
            **Directory**: %s
            **Permission Mode**: %s

            Session ended successfully.

            ---
            """,
            input.sessionId(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            input.cwd(),
            input.permissionMode()
        );

        Files.writeString(logFile, summary,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND);

        log.debug("Appended session summary to: {}", logFile);
    }
}
