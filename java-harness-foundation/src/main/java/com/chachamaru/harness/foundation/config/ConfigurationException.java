package com.chachamaru.harness.foundation.config;

/**
 * Exception thrown when configuration is invalid or cannot be applied.
 *
 * <p>This exception is used to indicate configuration-related errors,
 * such as missing required fields, invalid values, or incompatible settings.</p>
 */
public class ConfigurationException extends Exception {

    private final String configKey;

    /**
     * Creates a new configuration exception.
     *
     * @param message the error message
     */
    public ConfigurationException(String message) {
        super(message);
        this.configKey = null;
    }

    /**
     * Creates a new configuration exception with a specific config key.
     *
     * @param configKey the configuration key that caused the error
     * @param message   the error message
     */
    public ConfigurationException(String configKey, String message) {
        super(String.format("[%s] %s", configKey, message));
        this.configKey = configKey;
    }

    /**
     * Creates a new configuration exception with a cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
        this.configKey = null;
    }

    /**
     * Creates a new configuration exception with a specific config key and cause.
     *
     * @param configKey the configuration key that caused the error
     * @param message   the error message
     * @param cause     the underlying cause
     */
    public ConfigurationException(String configKey, String message, Throwable cause) {
        super(String.format("[%s] %s", configKey, message), cause);
        this.configKey = configKey;
    }

    /**
     * Gets the configuration key that caused the error.
     *
     * @return the configuration key, or null if not specified
     */
    public String getConfigKey() {
        return configKey;
    }
}
