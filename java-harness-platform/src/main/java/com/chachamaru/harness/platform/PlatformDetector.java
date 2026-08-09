package com.chachamaru.harness.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Platform detector for identifying the current runtime environment.
 * <p>
 * Determines whether the code is running in Claude Code or Codex CLI,
 * and provides platform-specific configuration and behaviors.</p>
 *
 * @since 5.0.0
 */
public class PlatformDetector {

    private static final Logger logger = LoggerFactory.getLogger(PlatformDetector.class);

    /**
     * Supported platforms.
     */
    public enum Platform {
        /**
         * Claude Code environment
         */
        CLAUDE_CODE("claude-code", "Claude Code"),

        /**
         * OpenAI Codex environment
         */
        CODEX("codex", "OpenAI Codex"),

        /**
         * Unknown environment
         */
        UNKNOWN("unknown", "Unknown Platform");

        private final String id;
        private final String displayName;

        Platform(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Detects the current platform by examining environment indicators.
     *
     * @return the detected platform
     */
    public static Platform detectCurrentPlatform() {
        // Check Claude Code indicators
        if (isClaudeEnvironment()) {
            logger.debug("Detected Claude Code environment");
            return Platform.CLAUDE_CODE;
        }

        // Check Codex indicators
        if (isCodexEnvironment()) {
            logger.debug("Detected Codex environment");
            return Platform.CODEX;
        }

        logger.debug("Unable to detect specific platform, using UNKNOWN");
        return Platform.UNKNOWN;
    }

    /**
     * Checks if the current environment is Claude Code.
     *
     * @return true if running in Claude Code
     */
    private static boolean isClaudeEnvironment() {
        try {
            // Check for Claude-specific environment variables
            String claudeApiKey = System.getenv("ANTHROPIC_API_KEY");
            if (claudeApiKey != null && !claudeApiKey.isEmpty()) {
                return true;
            }

            // Check for Claude-specific directory structure
            String userHome = System.getProperty("user.home");
            Path claudeDir = Path.of(userHome, ".claude");
            if (Files.exists(claudeDir)) {
                // Check for Claude-specific files
                Path pluginsDir = claudeDir.resolve("plugins");
                return Files.exists(pluginsDir);
            }

            // Check for Claude-specific system properties
            String claudeVersion = System.getProperty("claude.code.version");
            if (claudeVersion != null) {
                return true;
            }

        } catch (Exception e) {
            logger.warn("Error checking Claude environment: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Checks if the current environment is Codex CLI.
     *
     * @return true if running in Codex CLI
     */
    private static boolean isCodexEnvironment() {
        try {
            String userHome = System.getProperty("user.home");
            Path codexDir = Path.of(userHome, ".codex");

            if (!Files.exists(codexDir)) {
                return false;
            }

            // Check for Codex-specific files
            Path codexConfig = codexDir.resolve("config.toml");
            if (Files.exists(codexConfig)) {
                return true;
            }

            // Check for Codex plugins directory
            Path codexPlugins = codexDir.resolve("plugins");
            if (Files.exists(codexPlugins)) {
                return true;
            }

            // Check for Codex-specific environment variables
            String openaiApiKey = System.getenv("OPENAI_API_KEY");
            if (openaiApiKey != null && !openaiApiKey.isEmpty()) {
                // Further validation needed, but this is a strong indicator
                return true;
            }

        } catch (Exception e) {
            logger.warn("Error checking Codex environment: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Gets platform-specific configuration.
     *
     * @param platform the platform to get configuration for
     * @return platform-specific configuration properties
     */
    public static Properties getPlatformConfig(Platform platform) {
        Properties config = new Properties();

        switch (platform) {
            case CLAUDE_CODE:
                config.setProperty("default.backend", "claude");
                config.setProperty("native.agents", "true");
                config.setProperty("realtime.review", "true");
                config.setProperty("hook.system", "native");
                config.setProperty("skill.format", "native");
                break;

            case CODEX:
                config.setProperty("default.backend", "codex");
                config.setProperty("native.agents", "false");
                config.setProperty("realtime.review", "false");
                config.setProperty("hook.system", "bridged");
                config.setProperty("skill.format", "bridged");
                config.setProperty("config.file", ".codex/config.toml");
                break;

            case UNKNOWN:
            default:
                config.setProperty("default.backend", "claude");
                config.setProperty("auto.detect", "true");
                break;
        }

        return config;
    }

    /**
     * Gets the appropriate backend for the current platform.
     *
     * @return the recommended backend identifier
     */
    public static String getRecommendedBackend() {
        Platform platform = detectCurrentPlatform();
        return switch (platform) {
            case CLAUDE_CODE -> "claude";
            case CODEX -> "codex";
            case UNKNOWN -> "claude"; // Default to Claude
        };
    }

    /**
     * Checks if a specific feature is supported on the current platform.
     *
     * @param feature the feature to check
     * @return true if the feature is supported
     */
    public static boolean isFeatureSupported(String feature) {
        Platform platform = detectCurrentPlatform();

        return switch (feature.toLowerCase()) {
            case "native_agents" -> platform == Platform.CLAUDE_CODE;
            case "codex_adapter" -> platform == Platform.CODEX;
            case "unified_commands" -> true; // Both platforms support
            case "skill_bridge" -> platform == Platform.CODEX;
            case "plan" -> true;
            case "work" -> true;
            case "review" -> true;
            case "release" -> true;
            default -> false;
        };
    }

    /**
     * Gets platform-specific user agent string.
     *
     * @return user agent identifier
     */
    public static String getUserAgent() {
        Platform platform = detectCurrentPlatform();
        String version = getVersion();

        return String.format("java-harness/%s (%s)", version, platform.getDisplayName());
    }

    /**
     * Gets the current version.
     *
     * @return version string
     */
    private static String getVersion() {
        try {
            Properties props = new Properties();
            props.load(PlatformDetector.class.getResourceAsStream("/version.properties"));
            return props.getProperty("version", "5.0.0");
        } catch (IOException e) {
            return "5.0.0";
        }
    }

    /**
     * Creates a platform-adapted configuration.
     *
     * @param baseConfig the base configuration
     * @return platform-adapted configuration
     */
    public static Map<String, Object> adaptConfigForPlatform(Map<String, Object> baseConfig) {
        Platform platform = detectCurrentPlatform();

        // Add platform-specific settings
        baseConfig.put("detected_platform", platform.getId());
        baseConfig.put("recommended_backend", getRecommendedBackend());
        baseConfig.put("version", getVersion());

        // Add platform-specific optimizations
        switch (platform) {
            case CLAUDE_CODE:
                baseConfig.put("optimization", "quality");
                baseConfig.put("effort", "high");
                break;

            case CODEX:
                baseConfig.put("optimization", "balance");
                baseConfig.put("effort", "medium");
                break;

            case UNKNOWN:
                baseConfig.put("optimization", "auto");
                baseConfig.put("effort", "medium");
                break;
        }

        return baseConfig;
    }
}