package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * PostToolFailure hook handler
 * <p>
 * Handles tool failure tracking and automatic escalation after 3 failures.
 * </p>
 */
public class PostToolFailureHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PostToolFailureHandler.class);
    private static final String POST_TOOL_FAILURE = "PostToolFailure";
    private static final int MAX_FAILURES = 3;

    // Track failure counts per session
    private static final Properties failureCounts = new Properties();

    @Override
    public String getEventName() {
        return POST_TOOL_FAILURE;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.warn("Tool failure detected in session: {}", input.sessionId());

        // Increment failure count
        int count = incrementFailureCount(input.sessionId());

        // Check if escalation is needed
        if (count >= MAX_FAILURES) {
            log.error("Tool failure count reached {} - escalating for session: {}", count, input.sessionId());

            // Create escalation marker file
            createEscalationMarker(input);

            // Reset count after escalation
            failureCounts.setProperty(input.sessionId(), "0");

            return new HookOutput(
                POST_TOOL_FAILURE,
                "allow",
                null,
                "CRITICAL: " + count + " tool failures detected - escalated for review"
            );
        }

        log.debug("Tool failure count: {} for session: {}", count, input.sessionId());

        return new HookOutput(
            POST_TOOL_FAILURE,
            "allow",
            null,
            "Tool failure count: " + count + "/" + MAX_FAILURES
        );
    }

    /**
     * Increment failure count for session
     */
    private synchronized int incrementFailureCount(String sessionId) {
        String current = failureCounts.getProperty(sessionId, "0");
        int count = Integer.parseInt(current) + 1;
        failureCounts.setProperty(sessionId, String.valueOf(count));
        return count;
    }

    /**
     * Create escalation marker file
     */
    private void createEscalationMarker(HookInput input) throws IOException {
        Path cwd = Paths.get(input.cwd());
        Path escalationFile = cwd.resolve(".claude").resolve("escalation-" + input.sessionId() + ".txt");

        String escalationMessage = String.format(
            "ESCALATION REQUIRED\n" +
            "Session: %s\n" +
            "Time: %s\n" +
            "Reason: %d consecutive tool failures\n" +
            "Action: Manual review required\n",
            input.sessionId(),
            java.time.LocalDateTime.now().toString(),
            MAX_FAILURES
        );

        Files.writeString(escalationFile, escalationMessage);
        log.info("Created escalation marker: {}", escalationFile);
    }

    /**
     * Reset failure count (for testing or session reset)
     */
    public static void resetFailureCount(String sessionId) {
        failureCounts.remove(sessionId);
    }

    /**
     * Get current failure count
     */
    public static int getFailureCount(String sessionId) {
        String current = failureCounts.getProperty(sessionId, "0");
        return Integer.parseInt(current);
    }
}
