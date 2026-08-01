package com.chachamaru.harness.protocol;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;

/**
 * Handler interface for processing hook events in the Claude Code Harness lifecycle.
 *
 * <p>Implementations of this interface can be registered to handle specific types
 * of hook events, allowing customization of the development workflow.</p>
 *
 * @param <T> the type of input context for this hook handler
 * @since 4.1.0
 */
@FunctionalInterface
public interface HookHandler<T extends HookInput> {

    /**
     * Handles a hook event.
     *
     * <p>This method is called when the associated hook event type is triggered
     * during the execution lifecycle. Implementations should process the input
     * and return an appropriate output.</p>
     *
     * @param eventType the type of hook event being handled
     * @param input the input context for this hook
     * @return the output result from processing this hook
     * @throws HookHandlerException if the hook handler fails
     */
    HookOutput handle(HookEventType eventType, T input) throws HookHandlerException;

    /**
     * Returns the priority of this hook handler.
     *
     * <p>Higher priority handlers are executed first. Default priority is 0.</p>
     *
     * @return the priority value (higher values = higher priority)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Determines if this handler should process the given event type.
     *
     * @param eventType the event type to check
     * @return true if this handler should process the event, false otherwise
     */
    default boolean canHandle(HookEventType eventType) {
        return true;
    }
}
