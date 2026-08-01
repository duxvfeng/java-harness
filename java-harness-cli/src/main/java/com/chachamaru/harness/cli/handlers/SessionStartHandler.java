package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * SessionStart hook handler
 * <p>
 * Handles session initialization including Plans.md summary and environment setup.
 * </p>
 */
public class SessionStartHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(SessionStartHandler.class);
    private static final String SESSION_START = "SessionStart";

    @Override
    public String getEventName() {
        return SESSION_START;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("Session started: {}", input.sessionId());

        // Initialize session state
        initializeSession(input);

        // Generate Plans.md summary
        String plansSummary = generatePlansSummary(input);

        // Log session start with context
        logSessionStart(input, plansSummary);

        // Build additional context for the session
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("Session initialized at ").append(formatTimestamp()).append("\n");

        if (plansSummary != null && !plansSummary.isEmpty()) {
            contextBuilder.append("\nPlans.md Summary:\n").append(plansSummary);
        }

        return HookOutput.allow();
    }

    /**
     * Initialize session state
     */
    private void initializeSession(HookInput input) throws IOException {
        // Create session directory structure
        Path cwd = Paths.get(input.cwd());
        Path sessionDir = cwd.resolve(".claude").resolve("state").resolve(input.sessionId());

        if (!Files.exists(sessionDir)) {
            Files.createDirectories(sessionDir);
            log.debug("Created session directory: {}", sessionDir);
        }

        // Initialize session metadata file
        Path metadataFile = sessionDir.resolve("metadata.json");
        if (!Files.exists(metadataFile)) {
            String metadata = String.format(
                "{\"sessionId\":\"%s\",\"startTime\":\"%s\",\"cwd\":\"%s\"}",
                input.sessionId(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                input.cwd()
            );
            Files.writeString(metadataFile, metadata);
            log.debug("Created session metadata: {}", metadataFile);
        }
    }

    /**
     * Generate Plans.md summary
     */
    private String generatePlansSummary(HookInput input) {
        try {
            Path cwd = Paths.get(input.cwd());
            Path plansFile = cwd.resolve("Plans.md");

            if (!Files.exists(plansFile)) {
                return null;
            }

            String content = Files.readString(plansFile);

            // Extract key information from Plans.md
            StringBuilder summary = new StringBuilder();

            // Extract project title/phase
            if (content.contains("# ")) {
                String firstLine = content.lines()
                    .filter(line -> line.startsWith("# "))
                    .findFirst()
                    .orElse("");
                if (!firstLine.isEmpty()) {
                    summary.append("Project: ").append(firstLine.substring(2)).append("\n");
                }
            }

            // Count TODO and completed items
            long todoCount = content.lines()
                .filter(line -> line.contains("cc:TODO"))
                .count();

            long completedCount = content.lines()
                .filter(line -> line.contains("cc:"))
                .filter(line -> !line.contains("cc:TODO"))
                .count();

            summary.append("Progress: ").append(completedCount).append(" completed, ")
                  .append(todoCount).append(" pending\n");

            // Extract current phase if available
            if (content.contains("## Phase")) {
                String currentPhase = content.lines()
                    .filter(line -> line.startsWith("## Phase"))
                    .findFirst()
                    .map(line -> line.substring(3).trim())
                    .orElse("Unknown");
                summary.append("Current Phase: ").append(currentPhase).append("\n");
            }

            return summary.toString();

        } catch (IOException e) {
            log.warn("Failed to read Plans.md: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Log session start with context
     */
    private void logSessionStart(HookInput input, String plansSummary) {
        log.info("=== Session Start ===");
        log.info("Session ID: {}", input.sessionId());
        log.info("Working Directory: {}", input.cwd());
        log.info("Permission Mode: {}", input.permissionMode());

        if (plansSummary != null) {
            log.info("Plans Summary: {}", plansSummary.replace("\n", " | "));
        }

        log.info("==================");
    }

    /**
     * Format current timestamp
     */
    private String formatTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}