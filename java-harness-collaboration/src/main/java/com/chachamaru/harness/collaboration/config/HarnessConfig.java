package com.chachamaru.harness.collaboration.config;

import java.util.Map;
import java.util.HashMap;

/**
 * Unified configuration for Java Harness across platforms.
 *
 * <p>Provides configuration compatibility between Claude Code and Codex platforms.
 * Supports TOML-based configuration with environment variable expansion and validation.</p>
 *
 * <h3>Configuration Structure:</h3>
 * <pre>{@code
 * [harness]
 * version = "4.1.1"
 * platform = "claude-code" | "codex"
 * multi-platform = true
 *
 * [backend]
 * default = "claude" | "codex" | "auto-detect"
 * timeout = 300000
 * max-retries = 3
 *
 * [claude-code]
 * model = "claude-sonnet-5"
 * max-tokens = 200000
 *
 * [codex]
 * model = "gpt-4"
 * api-key = "${CODEX_API_KEY}"
 * }</pre>
 *
 * @spec_reference Phase 7: Dual Platform Support
 */
public class HarnessConfig {

    /**
     * Default harness version.
     */
    private static final String DEFAULT_VERSION = "4.1.1";

    /**
     * Default platform.
     */
    private static final String DEFAULT_PLATFORM = "claude-code";

    // Core configuration
    private String version;
    private String platform;
    private boolean multiPlatform;

    // Backend configuration
    private String backendDefault;
    private long backendTimeout;
    private int backendMaxRetries;
    private Map<String, Object> backendFallback;

    // Platform-specific configurations
    private Map<String, Map<String, Object>> platformConfigs;

    // Environment variables for expansion
    private Map<String, String> environment;

    /**
     * Creates a new harness configuration with defaults.
     */
    public HarnessConfig() {
        this.version = DEFAULT_VERSION;
        this.platform = DEFAULT_PLATFORM;
        this.multiPlatform = false;
        this.backendDefault = "auto-detect";
        this.backendTimeout = 300000;
        this.backendMaxRetries = 3;
        this.backendFallback = new HashMap<>();
        this.platformConfigs = new HashMap<>();
        this.environment = new HashMap<>();
    }

    /**
     * Gets the harness version.
     *
     * @return the version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the target platform.
     *
     * @return the platform identifier
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * Checks if multi-platform mode is enabled.
     *
     * @return true if multi-platform support is enabled
     */
    public boolean isMultiPlatform() {
        return multiPlatform;
    }

    /**
     * Gets the default backend.
     *
     * @return the backend identifier
     */
    public String getBackendDefault() {
        return backendDefault;
    }

    /**
     * Gets the backend timeout in milliseconds.
     *
     * @return timeout value
     */
    public long getBackendTimeout() {
        return backendTimeout;
    }

    /**
     * Gets the maximum retry count for backend.
     *
     * @return max retries value
     */
    public int getBackendMaxRetries() {
        return backendMaxRetries;
    }

    /**
     * Gets the API key (with environment expansion).
     *
     * @return the expanded API key, or null if not set
     */
    public String getApiKey() {
        String apiKey = (String) getFromPlatformConfigs("api-key");
        return expandEnvVars(apiKey);
    }

    /**
     * Gets the data directory (with environment expansion).
     *
     * @return the expanded data directory path
     */
    public String getDataDir() {
        String dataDir = (String) getFromPlatformConfigs("data-dir");
        return expandEnvVars(dataDir);
    }

    /**
     * Checks if a platform-specific section exists.
     *
     * @param platformSection the platform section name (e.g., "claude-code", "codex")
     * @return true if the section exists
     */
    public boolean hasPlatformSection(String platformSection) {
        return platformConfigs.containsKey(platformSection);
    }

    /**
     * Checks if a section exists.
     *
     * @param section the section name (can be nested like "skills.claude")
     * @return true if the section exists
     */
    public boolean hasSection(String section) {
        // For simplicity, just check top-level sections
        return platformConfigs.containsKey(section);
    }

    /**
     * Gets a value from platform configurations.
     *
     * @param key the configuration key
     * @return the value, or null if not found
     */
    public Object getFromPlatformConfigs(String key) {
        // Simple implementation: check all platform configs
        for (Map<String, Object> config : platformConfigs.values()) {
            if (config.containsKey(key)) {
                return config.get(key);
            }
        }
        return null;
    }

    /**
     * Expands environment variables in a string.
     *
     * <p>Supports ${VAR} and ${VAR:-default} syntax.</p>
     *
     * @param value the string to expand
     * @return the expanded string
     */
    private String expandEnvVars(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // Simple ${VAR} expansion
        StringBuilder result = new StringBuilder();
        int start = 0;
        int dollarBrace;

        while ((dollarBrace = value.indexOf("${", start)) != -1) {
            result.append(value, start, dollarBrace);
            int endBrace = value.indexOf('}', dollarBrace + 2);
            if (endBrace == -1) {
                // Malformed, just append rest
                result.append(value.substring(dollarBrace));
                break;
            }

            String varName = value.substring(dollarBrace + 2, endBrace);
            String defaultValue = null;
            int colonIndex = varName.indexOf(":-");
            if (colonIndex > 0) {
                defaultValue = varName.substring(colonIndex + 2);
                varName = varName.substring(0, colonIndex);
            }

            String envValue = System.getenv(varName);
            if (envValue != null) {
                result.append(envValue);
            } else if (defaultValue != null) {
                result.append(defaultValue);
            } else {
                // Keep original if var not found and no default
                result.append(value.substring(dollarBrace, endBrace + 1));
            }

            start = endBrace + 1;
        }

        result.append(value.substring(start));
        return result.toString();
    }

    /**
     * Sets the harness version.
     */
    void setVersion(String version) {
        this.version = version;
    }

    /**
     * Sets the target platform.
     */
    void setPlatform(String platform) {
        this.platform = platform;
    }

    /**
     * Sets multi-platform mode.
     */
    void setMultiPlatform(boolean multiPlatform) {
        this.multiPlatform = multiPlatform;
    }

    /**
     * Sets the default backend.
     */
    void setBackendDefault(String backendDefault) {
        this.backendDefault = backendDefault;
    }

    /**
     * Sets the backend timeout.
     */
    void setBackendTimeout(long backendTimeout) {
        this.backendTimeout = backendTimeout;
    }

    /**
     * Sets the max retry count.
     */
    void setBackendMaxRetries(int backendMaxRetries) {
        this.backendMaxRetries = backendMaxRetries;
    }

    /**
     * Sets platform configuration.
     */
    void setPlatformConfig(String platform, Map<String, Object> config) {
        this.platformConfigs.put(platform, config);
    }

    /**
     * Gets platform configuration.
     */
    Map<String, Object> getPlatformConfig(String platform) {
        return platformConfigs.getOrDefault(platform, new HashMap<>());
    }
}
