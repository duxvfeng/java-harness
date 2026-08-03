package com.chachamaru.harness.handler;

import com.chachamaru.harness.config.ConfigSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Init command handler.
 * Initializes a new project with harness configuration.
 */
public class InitHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(InitHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            String projectRoot = ".";
            boolean force = false;

            // Parse arguments
            for (String arg : args) {
                if (arg.equals("--force")) {
                    force = true;
                } else if (!arg.startsWith("--")) {
                    projectRoot = arg;
                }
            }

            logger.info("Initializing harness project in: {}", projectRoot);

            // Create default configuration
            String configPath = projectRoot + "/harness.toml";

            // Check if config already exists
            if (!force) {
                try {
                    if (ConfigSync.loadConfigFromFile(configPath) != null &&
                            !ConfigSync.loadConfigFromFile(configPath).isEmpty()) {
                        logger.warn("Configuration file already exists: {}", configPath);
                        logger.info("Use --force to overwrite");
                        return;
                    }
                } catch (IOException e) {
                    // File doesn't exist, continue
                }
            }

            // Create configuration
            ConfigSync.createDefaultConfig(configPath);
            logger.info("Created configuration file: {}", configPath);

            // Create state directory
            String stateDir = projectRoot + "/.claude/state";
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(stateDir));
            logger.info("Created state directory: {}", stateDir);

            // Create log directory
            String logDir = projectRoot + "/.claude/logs";
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(logDir));
            logger.info("Created log directory: {}", logDir);

            System.out.println("✓ Harness project initialized successfully!");
            System.out.println("  Configuration: " + configPath);
            System.out.println("  State directory: " + stateDir);

        } catch (IOException e) {
            logger.error("Failed to initialize project", e);
            System.err.println("Error: Failed to initialize project: " + e.getMessage());
            System.exit(1);
        }
    }
}
