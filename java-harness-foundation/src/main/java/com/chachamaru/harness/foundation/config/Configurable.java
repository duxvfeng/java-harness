package com.chachamaru.harness.foundation.config;

/**
 * Configuration interface for configurable components.
 *
 * <p>This interface defines the contract for components that can be configured
 * with a configuration object. It provides type-safe configuration management.</p>
 *
 * @param <T> the configuration type
 * @spec_reference spec.md#Configuration Management
 */
public interface Configurable<T> {

    /**
     * Configures this component with the provided configuration.
     *
     * @param config the configuration to apply
     * @throws ConfigurationException if configuration is invalid
     */
    void configure(T config) throws ConfigurationException;

    /**
     * Gets the current configuration.
     *
     * @return the current configuration, or null if not configured
     */
    T getConfiguration();

    /**
     * Checks if this component is configured.
     *
     * @return true if configured, false otherwise
     */
    default boolean isConfigured() {
        return getConfiguration() != null;
    }

    /**
     * Reconfigures this component with a new configuration.
     *
     * @param config the new configuration to apply
     * @throws ConfigurationException if configuration is invalid
     */
    default void reconfigure(T config) throws ConfigurationException {
        configure(config);
    }
}
