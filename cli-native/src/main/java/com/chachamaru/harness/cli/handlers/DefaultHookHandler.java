package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Default hook handler - allows all events
 */
public class DefaultHookHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultHookHandler.class);
    private final String eventName;

    public DefaultHookHandler(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.debug("Handling {} event (default allow)", eventName);
        return HookOutput.allow();
    }
}
