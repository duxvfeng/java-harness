package com.chachamaru.harness.handler.session;

import com.chachamaru.harness.handler.CommandHandler;
import com.chachamaru.harness.session.manager.SessionSaveManager;
import com.chachamaru.harness.session.restore.SessionRestoreManager;
import com.chachamaru.harness.session.storage.SessionStorage;
import com.chachamaru.harness.session.storage.FileSystemStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Base class for session management commands.
 *
 * <p>Provides common functionality and initialization for all session-related commands.</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public abstract class BaseSessionCommand implements CommandHandler {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final SessionSaveManager saveManager;
    protected final SessionRestoreManager restoreManager;
    protected final SessionStorage storage;

    public BaseSessionCommand() {
        // Initialize storage
        this.storage = createDefaultStorage();

        // Initialize managers
        this.saveManager = createSaveManager(storage);
        this.restoreManager = createRestoreManager(storage);

        logger.debug("Session command initialized: {}", getClass().getSimpleName());
    }

    /**
     * Execute the session command with argument parsing
     */
    @Override
    public void execute(String[] args) {
        logger.debug("Executing session command: {} with args: {}",
                getClass().getSimpleName(), Arrays.toString(args));

        try {
            executeCommand(args);
        } catch (Exception e) {
            logger.error("Session command execution failed", e);
            handleError("命令执行失败: " + e.getMessage(), e);
        }
    }

    /**
     * Execute the specific command logic
     */
    protected abstract void executeCommand(String[] args) throws Exception;

    /**
     * Handle command execution errors
     */
    protected void handleError(String message, Exception e) {
        System.err.println("❌ " + message);
        if (logger.isDebugEnabled()) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Parse boolean flag from arguments
     */
    protected boolean parseFlag(String[] args, String flagName, boolean defaultValue) {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(flagName)) {
                return true;
            }
        }
        return defaultValue;
    }

    /**
     * Parse string value from arguments
     */
    protected String parseValue(String[] args, String flagName) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase(flagName) && i + 1 < args.length) {
                return args[i + 1];
            }
        }
        return null;
    }

    /**
     * Parse integer value from arguments
     */
    protected int parseIntValue(String[] args, String flagName, int defaultValue) {
        String value = parseValue(args, flagName);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for {}: {}", flagName, value);
            }
        }
        return defaultValue;
    }

    /**
     * Create default storage implementation
     */
    private SessionStorage createDefaultStorage() {
        return new FileSystemStorage(
                Paths.get(System.getProperty("user.dir"))
                        .resolve(".claude")
                        .resolve("state")
                        .resolve("session-saves"),
                100 * 1024 * 1024 // 100MB
        );
    }

    /**
     * Create session save manager
     */
    private SessionSaveManager createSaveManager(SessionStorage storage) {
        return new SessionSaveManager(
                storage,
                SessionSaveManager.SessionSaveConfig.getDefault()
        );
    }

    /**
     * Create session restore manager
     */
    private SessionRestoreManager createRestoreManager(SessionStorage storage) {
        return new SessionRestoreManager(
                storage,
                SessionRestoreManager.RestoreConfig.getDefault()
        );
    }
}