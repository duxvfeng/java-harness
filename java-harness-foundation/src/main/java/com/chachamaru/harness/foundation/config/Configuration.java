package com.chachamaru.harness.foundation.config;

/**
 * Configuration marker interface.
 *
 * <p>All configuration objects should implement this interface.
 * This provides type safety and enables configuration validation.</p>
 *
 * @spec_reference spec.md#Configuration Management
 */
public interface Configuration {

    /**
     * Validates this configuration.
     *
     * @throws ConfigurationException if configuration is invalid
     */
    default void validate() throws ConfigurationException {
        // Default implementation: configuration is valid
        // Implementations can override for specific validation logic
    }

    /**
     * Checks if this configuration is valid.
     *
     * @return true if valid, false otherwise
     */
    default boolean isValid() {
        try {
            validate();
            return true;
        } catch (ConfigurationException e) {
            return false;
        }
    }
}
