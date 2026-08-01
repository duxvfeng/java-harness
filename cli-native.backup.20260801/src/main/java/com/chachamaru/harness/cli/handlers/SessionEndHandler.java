package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * SessionEnd hook handler
 */
public class SessionEndHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(SessionEndHandler.class);
    private static final String SESSION_END = "SessionEnd";

    @Override
    public String getEventName() {
        return SESSION_END;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.info("Session ended: {}", input.sessionId());

        // Session end logic could include:
        // - Cleaning up temporary files
        // - Saving final session state
        // - Updating session statistics
        // - Notifying external systems

        return HookOutput.allow();
    }
}