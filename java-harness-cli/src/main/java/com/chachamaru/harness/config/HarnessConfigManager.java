package com.chachamaru.harness.config;

import com.chachamaru.harness.e2e.E2EDetectionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Harness Configuration Manager (simplified for compilation)
 *
 * Manages loading configuration from various sources:
 * - harness.toml (TOML format)
 * - JSON configs
 * - Environment variables
 * - Default values
 *
 * @since 2.2.0
 */
public class HarnessConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(HarnessConfigManager.class);

    /**
     * Load E2E detection configuration from TOML file (stub implementation)
     */
    public static E2EDetectionConfig loadFromToml(Path tomlPath) {
        try {
            logger.info("Loading E2E detection config from TOML: {}", tomlPath);

            if (!Files.exists(tomlPath)) {
                logger.warn("TOML file not found, using default configuration");
                return E2EDetectionConfig.getDefault();
            }

            // TODO: Implement actual TOML parsing when dependencies are resolved
            logger.info("TOML parsing to be implemented - using defaults for now");
            return E2EDetectionConfig.getDefault();

        } catch (Exception e) {
            logger.error("Failed to load TOML configuration, using defaults", e);
            return E2EDetectionConfig.getDefault();
        }
    }

    /**
     * Load E2E detection configuration from JSON file
     */
    public static E2EDetectionConfig loadFromJson(Path jsonPath) {
        try {
            logger.info("Loading E2E detection config from JSON: {}", jsonPath);

            if (!Files.exists(jsonPath)) {
                logger.warn("JSON file not found, using default configuration");
                return E2EDetectionConfig.getDefault();
            }

            String jsonContent = Files.readString(jsonPath);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(jsonContent, E2EDetectionConfig.class);

        } catch (Exception e) {
            logger.error("Failed to load JSON configuration, using defaults", e);
            return E2EDetectionConfig.getDefault();
        }
    }

    /**
     * Override configuration with environment variables
     */
    public static void applyEnvironmentOverrides(E2EDetectionConfig config) {
        logger.debug("Applying environment variable overrides...");

        // Check for E2E detection enabled override
        String enabledOverride = System.getenv("HARNESS_E2E_ENABLED");
        if (enabledOverride != null) {
            config.setEnabled(Boolean.parseBoolean(enabledOverride));
            logger.info("E2E detection enabled overridden by env: {}", enabledOverride);
        }

        // Check for mode override
        String modeOverride = System.getenv("HARNESS_E2E_MODE");
        if (modeOverride != null) {
            config.setMode(modeOverride);
            logger.info("E2E detection mode overridden by env: {}", modeOverride);
        }

        // Check for timeout override
        String timeoutOverride = System.getenv("HARNESS_E2E_TIMEOUT");
        if (timeoutOverride != null) {
            try {
                config.setTimeout(Integer.parseInt(timeoutOverride));
                logger.info("E2E detection timeout overridden by env: {}", timeoutOverride);
            } catch (NumberFormatException e) {
                logger.warn("Invalid HARNESS_E2E_TIMEOUT value: {}", timeoutOverride);
            }
        }

        // Check for frontend enabled override
        String frontendOverride = System.getenv("HARNESS_E2E_FRONTEND");
        if (frontendOverride != null) {
            boolean enabled = Boolean.parseBoolean(frontendOverride);
            if (config.getTestTypes().getFrontend() != null) {
                config.getTestTypes().getFrontend().setEnabled(enabled);
                logger.info("E2E frontend detection overridden by env: {}", enabled);
            }
        }

        // Check for backend enabled override
        String backendOverride = System.getenv("HARNESS_E2E_BACKEND");
        if (backendOverride != null) {
            boolean enabled = Boolean.parseBoolean(backendOverride);
            if (config.getTestTypes().getBackend() != null) {
                config.getTestTypes().getBackend().setEnabled(enabled);
                logger.info("E2E backend detection overridden by env: {}", enabled);
            }
        }
    }

    /**
     * Load configuration with full priority chain:
     * TOML > JSON > Environment > Defaults
     */
    public static E2EDetectionConfig loadFullConfig(Path projectRoot, Path workDir) {
        logger.debug("Loading E2E detection configuration with full priority chain...");

        E2EDetectionConfig config;

        // Try TOML first (highest priority)
        Path tomlPath = projectRoot.resolve("java-harness-cli/harness.toml");
        if (Files.exists(tomlPath)) {
            config = loadFromToml(tomlPath);
        } else {
            // Try JSON next
            Path jsonPath = workDir.resolve(".claude/config/e2e-detection.config.json");
            if (Files.exists(jsonPath)) {
                config = loadFromJson(jsonPath);
            } else {
                // Use defaults
                config = E2EDetectionConfig.getDefault();
            }
        }

        // Apply environment overrides (lowest priority but can override)
        applyEnvironmentOverrides(config);

        logger.info("Final E2E detection config - enabled: {}, mode: {}, timeout: {}",
                    config.isEnabled(), config.getMode(), config.getTimeout());

        return config;
    }
}