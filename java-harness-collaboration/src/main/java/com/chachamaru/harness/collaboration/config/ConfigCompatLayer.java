package com.chachamaru.harness.collaboration.config;

import com.chachamaru.harness.collaboration.platform.Platform;
import com.chachamaru.harness.collaboration.platform.PlatformDetector;
import com.chachamaru.harness.collaboration.config.parser.TomlParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration compatibility layer for dual platform support.
 *
 * <p>This class provides a unified configuration interface that works across
 * both Claude Code and Codex CLI platforms. It handles platform-specific
 * configuration paths, default values, and configuration merging.</p>
 *
 * <p>Configuration loading priority (highest to lowest):
 * <ol>
 *   <li>Platform-specific config file (e.g., .codex/config.toml for Codex)</li>
 *   <li>Standard harness.toml in project root</li>
 *   <li>Platform-specific default values</li>
 *   <li>Universal default values</li>
 * </ol>
 *
 * @spec_reference Phase 7: Dual Platform Support - Task 7.5
 */
public class ConfigCompatLayer {

    private final PlatformDetector platformDetector;
    private final Platform currentPlatform;
    private final Path basePath;
    private Map<String, Object> config;

    /**
     * Configuration file paths for different platforms.
     */
    private static final String HARNESS_TOML = "harness.toml";
    private static final String CODEX_CONFIG_DIR = ".codex";
    private static final String CODEX_CONFIG_FILE = "config.toml";
    private static final String CLAUDE_CONFIG_DIR = ".claude";
    private static final String CLAUDE_CONFIG_FILE = "config.toml";

    /**
     * Default configuration values for Claude Code platform.
     */
    private static final Map<String, Object> CLAUDE_DEFAULTS = new HashMap<>(Map.of(
        "harness.backend", "claude",
        "harness.version", "5.0.0-java",
        "plan.enable", true,
        "work.enable", true,
        "work.default_effort", "medium",
        "review.enable", true,
        "hooks.enable", true
    ));

    /**
     * Default configuration values for Codex platform.
     */
    private static final Map<String, Object> CODEX_DEFAULTS = new HashMap<>(Map.of(
        "harness.backend", "codex",
        "harness.version", "5.0.0-java",
        "plan.enable", true,
        "work.enable", true,
        "work.default_effort", "medium",
        "review.enable", true,
        "hooks.enable", true
    ));

    /**
     * Creates a new configuration compatibility layer.
     *
     * <p>Automatically detects the current platform and loads the appropriate
     * configuration files with proper fallback chain.</p>
     */
    public ConfigCompatLayer() {
        this(Paths.get("").toAbsolutePath());
    }

    /**
     * Creates a new configuration compatibility layer with explicit platform.
     *
     * @param platform The platform to use (for testing purposes)
     */
    public ConfigCompatLayer(Platform platform) {
        this(platform, Paths.get("").toAbsolutePath());
    }

    /**
     * Creates a new configuration compatibility layer with explicit base path.
     *
     * @param basePath The base directory for resolving config files (for testing)
     */
    public ConfigCompatLayer(Path basePath) {
        this(new PlatformDetector().detectCurrentPlatform(), basePath);
    }

    /**
     * Creates a new configuration compatibility layer with explicit platform and base path.
     *
     * @param platform The platform to use (for testing purposes)
     * @param basePath The base directory for resolving config files (for testing)
     */
    public ConfigCompatLayer(Platform platform, Path basePath) {
        this.platformDetector = new PlatformDetector();
        this.currentPlatform = platform;
        this.basePath = basePath;
        loadConfiguration();
    }

    /**
     * Loads configuration from available sources with fallback chain.
     *
     * <p>Loading order:
     * <ol>
     *   <li>Platform-specific config file</li>
     *   <li>Standard harness.toml</li>
     *   <li>Platform defaults</li>
     * </ol>
     */
    private void loadConfiguration() {
        // Initialize config as mutable HashMap
        config = new HashMap<>();

        // Try platform-specific config first
        Map<String, Object> platformConfig = loadPlatformSpecificConfig();
        if (!platformConfig.isEmpty()) {
            config.putAll(platformConfig);
        }

        // If not found, try standard harness.toml
        if (config.isEmpty()) {
            Map<String, Object> standardConfig = loadStandardConfig();
            if (!standardConfig.isEmpty()) {
                config.putAll(standardConfig);
            }
        }

        // Apply platform-specific defaults for any missing values
        applyPlatformDefaults();
    }

    /**
     * Loads platform-specific configuration file.
     *
     * @return Configuration map, or empty map if file not found
     */
    private Map<String, Object> loadPlatformSpecificConfig() {
        String configDir = currentPlatform == Platform.CODEX
            ? CODEX_CONFIG_DIR
            : CLAUDE_CONFIG_DIR;
        String configFile = currentPlatform == Platform.CODEX
            ? CODEX_CONFIG_FILE
            : CLAUDE_CONFIG_FILE;

        Path configPath = basePath.resolve(configDir).resolve(configFile);
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                return TomlParser.parse(content);
            } catch (IOException e) {
                // Log error but continue with fallback
                System.err.println("Warning: Failed to load platform config: " + e.getMessage());
            }
        }
        return Map.of();
    }

    /**
     * Loads standard harness.toml configuration file.
     *
     * @return Configuration map, or empty map if file not found
     */
    private Map<String, Object> loadStandardConfig() {
        Path configPath = basePath.resolve(HARNESS_TOML);
        if (Files.exists(configPath)) {
            try {
                String content = Files.readString(configPath);
                return TomlParser.parse(content);
            } catch (IOException e) {
                // Log error but continue with defaults
                System.err.println("Warning: Failed to load standard config: " + e.getMessage());
            }
        }
        return Map.of();
    }

    /**
     * Applies platform-specific default values for missing configuration keys.
     */
    private void applyPlatformDefaults() {
        Map<String, Object> defaults = currentPlatform == Platform.CODEX
            ? CODEX_DEFAULTS
            : CLAUDE_DEFAULTS;

        defaults.forEach((key, value) -> {
            String[] parts = key.split("\\.");
            if (getNestedValue(config, parts) == null) {
                setNestedValue(config, parts, value);
            }
        });
    }

    /**
     * Gets a nested value from a map using a key path.
     *
     * @param map The map to search
     * @param path The key path segments
     * @return The value, or null if not found
     */
    private Object getNestedValue(Map<String, Object> map, String[] path) {
        Object current = map;
        for (String part : path) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * Sets a nested value in a map using a key path.
     *
     * @param map The map to modify
     * @param path The key path segments
     * @param value The value to set
     */
    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> map, String[] path, Object value) {
        Map<String, Object> currentMap = map;
        for (int i = 0; i < path.length - 1; i++) {
            String part = path[i];
            Object current = currentMap.get(part);
            Map<String, Object> nestedMap;

            if (!(current instanceof Map)) {
                nestedMap = new HashMap<>();
                currentMap.put(part, nestedMap);
            } else {
                nestedMap = (Map<String, Object>) current;
                // Check if the nested map is immutable
                if (nestedMap.getClass().getName().contains("Immutable")) {
                    nestedMap = new HashMap<>(nestedMap);
                    currentMap.put(part, nestedMap);
                }
            }
            currentMap = nestedMap;
        }
        currentMap.put(path[path.length - 1], value);
    }

    /**
     * Gets the current platform.
     *
     * @return The detected platform
     */
    public Platform getCurrentPlatform() {
        return currentPlatform;
    }

    /**
     * Gets a string configuration value using dot notation.
     *
     * @param path Dot-separated configuration path (e.g., "harness.version")
     * @return Optional containing the value if found
     */
    public Optional<String> getString(String path) {
        String value = TomlParser.getString(config, path);
        return value != null ? Optional.of(value) : Optional.empty();
    }

    /**
     * Gets a boolean configuration value using dot notation.
     *
     * @param path Dot-separated configuration path (e.g., "plan.enable")
     * @return Boolean value, or false if not found
     */
    public boolean getBoolean(String path) {
        return TomlParser.getBoolean(config, path);
    }

    /**
     * Gets an integer configuration value using dot notation.
     *
     * @param path Dot-separated configuration path (e.g., "review.max_review_rounds")
     * @return Integer value, or 0 if not found
     */
    public int getInt(String path) {
        return TomlParser.getInt(config, path);
    }

    /**
     * Gets the complete configuration map.
     *
     * @return The entire configuration map
     */
    public Map<String, Object> getConfig() {
        return new java.util.HashMap<>(config);
    }

    /**
     * Reloads configuration from files.
     *
     * <p>Use this method when configuration files may have changed externally.</p>
     */
    public void reload() {
        loadConfiguration();
    }
}
