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

/**
 * SubagentStop hook handler
 * <p>
 * Handles agent stop tracking.
 * </p>
 */
public class SubagentStopHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(SubagentStopHandler.class);
    private static final String SUBAGENT_STOP = "SubagentStop";

    @Override
    public String getEventName() {
        return SUBAGENT_STOP;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("Subagent stopped for session: {}", input.sessionId());

        // Track agent stop
        trackAgentStop(input);

        return HookOutput.allow();
    }

    /**
     * Track agent stop in agent log
     */
    private void trackAgentStop(HookInput input) throws IOException {
        Path agentLogFile = Paths.get(input.cwd())
            .resolve(".claude")
            .resolve("logs")
            .resolve("agents.log");

        Path logDir = agentLogFile.getParent();
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        String logEntry = String.format(
            "[%s] AGENT_STOP | Session: %s | Agent: %s%n",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            input.sessionId(),
            input.toolName() != null ? input.toolName() : "unknown"
        );

        Files.writeString(agentLogFile, logEntry,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND);
    }
}
