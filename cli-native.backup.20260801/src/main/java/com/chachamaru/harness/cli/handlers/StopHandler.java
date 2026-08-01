package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Stop hook handler
 */
public class StopHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(StopHandler.class);
    private static final String STOP = "Stop";

    @Override
    public String getEventName() {
        return STOP;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("Stop requested for session: {}", input.sessionId());

        // Stop logic could include:
        // - Checking if there are incomplete tasks
        // - Verifying session state consistency
        // - Saving session state

        // For now, we allow stop requests
        return HookOutput.allow();
    }
}