package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * PreCompact hook handler
 */
public class PreCompactHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PreCompactHandler.class);
    private static final String PRE_COMPACT = "PreCompact";

    @Override
    public String getEventName() {
        return PRE_COMPACT;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.debug("PreCompact triggered for session: {}", input.sessionId());

        // PreCompact logic could include:
        // - Checking if Plans.md is dirty
        // - Auto-committing changes if needed
        // - Validating session state before compaction

        // For now, we allow compaction
        return HookOutput.allow();
    }
}