package com.chachamaru.harness.handler;

import com.chachamaru.harness.hook.HookDispatcher;
import com.chachamaru.harness.handler.session.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for all command handlers.
 * Maps command names to their handler implementations.
 */
public class CommandRegistry {
    private static final Map<String, CommandHandler> handlers = new HashMap<>();

    static {
        // Core workflow commands should route to skills, not handlers
        // handlers.put("plan", new PlanHandler());
        // handlers.put("work", new WorkHandler());
        // handlers.put("review", new ReviewHandler());
        // handlers.put("release", new ReleaseHandler());
        // handlers.put("sync", new SyncHandler());

        // Utility command handlers (these remain as handlers)
        handlers.put("init", new InitHandler());
        handlers.put("doctor", new DoctorHandler());
        handlers.put("validate", new ValidateHandler());
        handlers.put("status", new StatusHandler());

        // Content generation and management (these remain as handlers for now)
        handlers.put("gen", new GenHandler());
        handlers.put("sprint-contract", new SprintContractHandler());
        handlers.put("evidence", new EvidenceHandler());

        // CLI utilities (these remain as handlers)
        handlers.put("version", new VersionHandler());
        handlers.put("help", new HelpHandler());
        handlers.put("completion", new CompletionHandler());

        // Hook dispatcher (essential)
        handlers.put("hook", new HookDispatcher());

        // Session management commands (unified format: /harness-session <subcommand>)
        handlers.put("harness-session", new SessionCommand());
        handlers.put("harness-session save", new SaveSessionCommand());
        handlers.put("harness-session restore", new RestoreSessionCommand());
        handlers.put("harness-session list", new ListSessionsCommand());
        handlers.put("harness-session show", new ShowSessionCommand());
        handlers.put("harness-session cleanup", new CleanupSessionsCommand());
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
