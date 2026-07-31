package com.chachamaru.harness.cli.router;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.handlers.HookHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hook event router
 */
public class HookRouter {
    private static final Logger log = LoggerFactory.getLogger(HookRouter.class);
    private final HandlerRegistry registry;

    public HookRouter() {
        this.registry = new HandlerRegistry();
        // Handlers will be registered during initialization
    }

    public void registerHandler(HookHandler handler) {
        registry.register(handler);
    }

    public HookHandler route(HookInput input) {
        String eventName = input.hookEventName();
        log.debug("Routing event: {}", eventName);

        if (!registry.hasHandler(eventName)) {
            throw new IllegalStateException("No handler registered for event: " + eventName);
        }

        return registry.get(eventName);
    }

    public HandlerRegistry getRegistry() {
        return registry;
    }
}
