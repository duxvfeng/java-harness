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
 * SubagentStart hook handler
 * <p>
 * Handles agent startup tracking.
 * </p>
 */
public class SubagentStartHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(SubagentStartHandler.class);
    private static final String SUBAGENT_START = "SubagentStart";

    @Override
    public String getEventName() {
        return SUBAGENT_START;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("Subagent started for session: {}", input.sessionId());

        // Track agent startup
        trackAgentStartup(input);

        return HookOutput.allow();
    }

    /**
     * Track agent startup in agent log
     */
    private void trackAgentStartup(HookInput input) throws IOException {
        Path agentLogFile = Paths.get(input.cwd())
            .resolve(".claude")
            .resolve("logs")
            .resolve("agents.log");

        Path logDir = agentLogFile.getParent();
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        String logEntry = String.format(
            "[%s] AGENT_START | Session: %s | Agent: %s%n",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            input.sessionId(),
            input.toolName() != null ? input.toolName() : "unknown"
        );

        Files.writeString(agentLogFile, logEntry,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND);
    }
}
