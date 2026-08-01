package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * SessionStart hook handler
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

        // Session start logic could include:
        // - Initializing session state
        // - Loading session-specific configuration
        // - Setting up monitoring

        return HookOutput.allow();
    }
}