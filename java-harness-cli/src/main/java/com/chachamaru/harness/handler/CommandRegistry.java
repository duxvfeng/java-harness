package com.chachamaru.harness.handler;

import com.chachamaru.harness.hook.HookDispatcher;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all command handlers.
 * Maps command names to their handler implementations.
 */
public class CommandRegistry {
    private static final Map<String, CommandHandler> handlers = new HashMap<>();

    static {
        // Register plan handler
        handlers.put("plan", new PlanHandler());

        // Register work handler
        handlers.put("work", new WorkHandler());

        // Register hook dispatcher
        handlers.put("hook", new HookDispatcher());
    }

    /**
     * Get the handler for a given command name.
     *
     * @param command The command name (without prefix)
     * @return The handler, or null if not found
     */
    public static CommandHandler getHandler(String command) {
        return handlers.get(command);
    }

    /**
     * Register a new command handler.
     *
     * @param command The command name
     * @param handler The handler implementation
     */
    public static void register(String command, CommandHandler handler) {
        handlers.put(command, handler);
    }
}
