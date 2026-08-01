package com.chachamaru.harness.foundation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Configuration interface.
 */
@DisplayName("Configuration Tests")
class ConfigurationTest {

    @Test
    @DisplayName("Should validate configuration by default")
    void shouldValidateConfigurationByDefault() throws ConfigurationException {
        Configuration config = new Configuration() {};
        assertDoesNotThrow(config::validate);
    }

    @Test
    @DisplayName("Should return true for valid configuration")
    void shouldReturnTrueForValidConfiguration() {
        Configuration config = new Configuration() {};
        assertTrue(config.isValid());
    }

    @Test
    @DisplayName("Should return false when validation fails")
    void shouldReturnFalseWhenValidationFails() {
        Configuration config = new Configuration() {
            @Override
            public void validate() throws ConfigurationException {
                throw new ConfigurationException("Invalid configuration");
            }
        };
        assertFalse(config.isValid());
    }

    @Test
    @DisplayName("Should return false when validation throws exception")
    void shouldReturnFalseWhenValidationThrowsException() {
        Configuration config = new Configuration() {
            @Override
            public void validate() throws ConfigurationException {
                throw new ConfigurationException("validation.error", "Test error");
            }
        };
        assertFalse(config.isValid());
    }
}
