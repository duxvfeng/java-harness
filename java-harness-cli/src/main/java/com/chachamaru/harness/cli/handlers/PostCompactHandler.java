package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * PostCompact hook handler
 * <p>
 * Handles WIP context re-injection after context compaction.
 * </p>
 */
public class PostCompactHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PostCompactHandler.class);
    private static final String POST_COMPACT = "PostCompact";

    @Override
    public String getEventName() {
        return POST_COMPACT;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("PostCompact triggered for session: {}", input.sessionId());

        // Re-inject WIP context
        String wipContext = restoreWipContext(input);

        if (wipContext != null && !wipContext.isEmpty()) {
            log.debug("Restored WIP context for session: {}", input.sessionId());
            return new HookOutput(
                POST_COMPACT,
                "allow",
                null,
                "WIP context restored: " + wipContext.substring(0, Math.min(100, wipContext.length())) + "..."
            );
        }

        return HookOutput.allow();
    }

    /**
     * Restore WIP context from session storage
     */
    private String restoreWipContext(HookInput input) {
        try {
            var contextFile = java.nio.file.Paths.get(input.cwd())
                .resolve(".claude")
                .resolve("state")
                .resolve(input.sessionId())
                .resolve("wip-context.txt");

            if (java.nio.file.Files.exists(contextFile)) {
                return java.nio.file.Files.readString(contextFile);
            }
        } catch (IOException e) {
            log.warn("Failed to restore WIP context: {}", e.getMessage());
        }

        return null;
    }
}
