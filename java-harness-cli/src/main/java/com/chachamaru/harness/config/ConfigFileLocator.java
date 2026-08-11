package com.chachamaru.harness.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration file locator
 *
 * Searches for configuration files in various locations with priority:
 * 1. Project root (current working directory)
 * 2. User home directory
 * 3. Installation directory
 * 4. Default bundled configuration
 *
 * @since 2.2.0
 */
public class ConfigFileLocator {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFileLocator.class);

    private static final String HARNESS_TOML = "harness.toml";
    private static final String HARNESS_TOML_EXAMPLE = "harness.toml.example";
    private static final String E2E_CONFIG_JSON = "e2e-detection.config.json";

    /**
     * Locate harness.toml configuration file
     *
     * Search order:
     * 1. Current working directory (project root)
     * 2. User home directory (~/.config/harness/)
     * 3. Installation directory relative to JAR
     * 4. Return null if not found (will use defaults)
     */
    public static Path locateHarnessToml() {
        List<Path> searchPaths = new ArrayList<>();

        // 1. Current working directory
        searchPaths.add(Paths.get(System.getProperty("user.dir"), HARNESS_TOML));

        // 2. Project root detection (find .git directory)
        Path projectRoot = detectProjectRoot();
        if (projectRoot != null) {
            searchPaths.add(projectRoot.resolve(HARNESS_TOML));
        }

        // 3. User config directory
        Path userConfigDir = Paths.get(System.getProperty("user.home"), ".config", "harness");
        searchPaths.add(userConfigDir.resolve(HARNESS_TOML));

        // 4. Installation directory
        Path installDir = detectInstallationDirectory();
        if (installDir != null) {
            searchPaths.add(installDir.resolve(HARNESS_TOML));
        }

        // Search and return first found
        for (Path searchPath : searchPaths) {
            logger.debug("Searching for config in: {}", searchPath);
            if (Files.exists(searchPath)) {
                logger.info("Found harness.toml at: {}", searchPath);
                return searchPath;
            }
        }

        logger.info("No harness.toml found, will use default configuration");
        return null;
    }

    /**
     * Locate E2E detection configuration file
     */
    public static Path locateE2EConfig(Path projectRoot) {
        List<Path> searchPaths = new ArrayList<>();

        // 1. Project .claude/config directory
        searchPaths.add(projectRoot.resolve(".claude/config").resolve(E2E_CONFIG_JSON));

        // 2. User config directory
        Path userConfigDir = Paths.get(System.getProperty("user.home"), ".config", "harness");
        searchPaths.add(userConfigDir.resolve(E2E_CONFIG_JSON));

        for (Path searchPath : searchPaths) {
            logger.debug("Searching for E2E config in: {}", searchPath);
            if (Files.exists(searchPath)) {
                logger.info("Found E2E config at: {}", searchPath);
                return searchPath;
            }
        }

        logger.info("No E2E config found, will use default configuration");
        return null;
    }

    /**
     * Detect project root by searching for .git directory
     */
    private static Path detectProjectRoot() {
        Path currentDir = Paths.get(System.getProperty("user.dir"));

        try {
            // Search up to 5 levels for .git directory
            Path searchDir = currentDir;
            for (int i = 0; i < 5; i++) {
                if (Files.exists(searchDir.resolve(".git"))) {
                    logger.debug("Found .git directory at: {}", searchDir);
                    return searchDir;
                }

                Path parent = searchDir.getParent();
                if (parent == null) break;
                searchDir = parent;
            }
        } catch (Exception e) {
            logger.debug("Error detecting project root", e);
        }

        return null;
    }

    /**
     * Detect installation directory (where JAR is located)
     */
    private static Path detectInstallationDirectory() {
        try {
            // Get the path to the running JAR
            String jarPath = ConfigFileLocator.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI().getPath();

            if (jarPath != null && jarPath.endsWith(".jar")) {
                Path jarLocation = Paths.get(jarPath);
                Path installDir = jarLocation.getParent();

                logger.debug("Detected installation directory: {}", installDir);
                return installDir;
            }
        } catch (Exception e) {
            logger.debug("Error detecting installation directory", e);
        }

        return null;
    }

    /**
     * Create default configuration file for user
     */
    public static Path createDefaultConfig(Path targetDir) {
        try {
            Path targetConfig = targetDir.resolve(HARNESS_TOML);

            if (Files.exists(targetConfig)) {
                logger.info("Config file already exists: {}", targetConfig);
                return targetConfig;
            }

            // Try to copy from example
            Path exampleConfig = detectInstallationDirectory();
            if (exampleConfig != null) {
                Path example = exampleConfig.resolve(HARNESS_TOML_EXAMPLE);
                if (Files.exists(example)) {
                    Files.copy(example, targetConfig);
                    logger.info("Created config from example: {}", targetConfig);
                    return targetConfig;
                }
            }

            logger.info("Could not create default config");
            return null;

        } catch (Exception e) {
            logger.error("Error creating default config", e);
            return null;
        }
    }

    /**
     * Initialize configuration for new project
     */
    public static Path initializeProjectConfig(Path projectRoot) {
        logger.info("Initializing harness configuration for project: {}", projectRoot);

        Path configDir = projectRoot.resolve(".claude/config");
        Path configFile = projectRoot.resolve("harness.toml");

        try {
            // Create directories
            Files.createDirectories(configDir);

            // Check if config already exists
            if (Files.exists(configFile)) {
                logger.info("Configuration already exists: {}", configFile);
                return configFile;
            }

            // Try to copy from example or create minimal config
            Path exampleConfig = detectInstallationDirectory();
            if (exampleConfig != null) {
                Path example = exampleConfig.resolve(HARNESS_TOML_EXAMPLE);
                if (Files.exists(example)) {
                    Files.copy(example, configFile);
                    logger.info("Created harness.toml from example");
                    return configFile;
                }
            }

            // Create minimal default configuration
            String minimalConfig = """
# Harness Configuration File
# Generated by Java Harness

[harness]
version = "5.0.0-java"
backend = "auto"
project_root = "."

[e2e_detection]
enabled = true
mode = "strict"
timeout = 120

[e2e_detection.test_types.frontend]
enabled = true
framework = "playwright"

[e2e_detection.test_types.backend]
enabled = true
framework = "auto"
""";
            Files.writeString(configFile, minimalConfig);
            logger.info("Created minimal harness.toml");

            return configFile;

        } catch (Exception e) {
            logger.error("Error initializing project config", e);
            return null;
        }
    }
}