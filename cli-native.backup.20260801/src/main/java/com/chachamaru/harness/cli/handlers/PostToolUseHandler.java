package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * PostToolUse hook handler
 */
public class PostToolUseHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PostToolUseHandler.class);
    private static final String POST_TOOL_USE = "PostToolUse";

    @Override
    public String getEventName() {
        return POST_TOOL_USE;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.debug("PostToolUse triggered for tool: {}", input.toolName());

        // PostToolUse logic could include:
        // - Recording tool usage statistics
        // - Updating session state
        // - Triggering compaction if needed
        // - Quality checks on tool results

        return HookOutput.allow();
    }
}