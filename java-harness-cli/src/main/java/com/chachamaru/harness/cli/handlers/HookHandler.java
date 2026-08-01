package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;

import java.io.IOException;

/**
 * Hook handler interface
 */
public interface HookHandler {

    /**
     * Get the hook event name this handler handles
     */
    String getEventName();

    /**
     * Handle the hook event
     */
    HookOutput handle(HookInput input) throws IOException;
}
