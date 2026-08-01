package com.chachamaru.harness.cli.router;

import com.chachamaru.harness.cli.handlers.HookHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for hook handlers
 */
public class HandlerRegistry {
    private static final Logger log = LoggerFactory.getLogger(HandlerRegistry.class);
    private final Map<String, HookHandler> handlers = new ConcurrentHashMap<>();

    public void register(HookHandler handler) {
        String eventName = handler.getEventName();
        handlers.put(eventName, handler);
        log.debug("Registered handler for event: {}", eventName);
    }

    public HookHandler get(String eventName) {
        HookHandler handler = handlers.get(eventName);
        if (handler == null) {
            throw new IllegalStateException("No handler registered for event: " + eventName);
        }
        return handler;
    }

    public boolean hasHandler(String eventName) {
        return handlers.containsKey(eventName);
    }

    public int getHandlerCount() {
        return handlers.size();
    }
}
