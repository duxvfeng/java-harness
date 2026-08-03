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
        // Core command handlers
        handlers.put("plan", new PlanHandler());
        handlers.put("work", new WorkHandler());
        handlers.put("review", new ReviewHandler());
        handlers.put("release", new ReleaseHandler());
        handlers.put("sync", new SyncHandler());

        // Utility command handlers
        handlers.put("init", new InitHandler());
        handlers.put("doctor", new DoctorHandler());
        handlers.put("validate", new ValidateHandler());
        handlers.put("status", new StatusHandler());

        // Content generation and management
        handlers.put("gen", new GenHandler());
        handlers.put("sprint-contract", new SprintContractHandler());
        handlers.put("evidence", new EvidenceHandler());

        // CLI utilities
        handlers.put("version", new VersionHandler());
        handlers.put("help", new HelpHandler());
        handlers.put("completion", new CompletionHandler());

        // Hook dispatcher
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
