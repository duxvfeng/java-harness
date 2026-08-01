package com.chachamaru.harness.foundation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Configurable interface.
 */
@DisplayName("Configurable Tests")
class ConfigurableTest {

    @Test
    @DisplayName("Should configure and retrieve configuration")
    void shouldConfigureAndRetrieveConfiguration() throws ConfigurationException {
        TestConfiguration config = new TestConfiguration("value1");
        Configurable<TestConfiguration> configurable = new Configurable<>() {
            private TestConfiguration configuration;

            @Override
            public void configure(TestConfiguration config) throws ConfigurationException {
                config.validate();
                this.configuration = config;
            }

            @Override
            public TestConfiguration getConfiguration() {
                return configuration;
            }
        };

        configurable.configure(config);
        assertEquals("value1", configurable.getConfiguration().value());
        assertTrue(configurable.isConfigured());
    }

    @Test
    @DisplayName("Should return false when not configured")
    void shouldReturnFalseWhenNotConfigured() {
        Configurable<TestConfiguration> configurable = new Configurable<>() {
            @Override
            public void configure(TestConfiguration config) throws ConfigurationException {
                // Do nothing
            }

            @Override
            public TestConfiguration getConfiguration() {
                return null;
            }
        };

        assertFalse(configurable.isConfigured());
    }

    @Test
    @DisplayName("Should throw exception when configuration is invalid")
    void shouldThrowExceptionWhenConfigurationIsInvalid() {
        TestConfiguration invalidConfig = new TestConfiguration(null);
        Configurable<TestConfiguration> configurable = new Configurable<>() {
            @Override
            public void configure(TestConfiguration config) throws ConfigurationException {
                config.validate();
            }

            @Override
            public TestConfiguration getConfiguration() {
                return null;
            }
        };

        assertThrows(ConfigurationException.class, () -> configurable.configure(invalidConfig));
    }

    @Test
    @DisplayName("Should support reconfiguration")
    void shouldSupportReconfiguration() throws ConfigurationException {
        TestConfiguration config1 = new TestConfiguration("value1");
        TestConfiguration config2 = new TestConfiguration("value2");

        Configurable<TestConfiguration> configurable = new Configurable<>() {
            private TestConfiguration configuration;

            @Override
            public void configure(TestConfiguration config) throws ConfigurationException {
                config.validate();
                this.configuration = config;
            }

            @Override
            public TestConfiguration getConfiguration() {
                return configuration;
            }
        };

        configurable.configure(config1);
        assertEquals("value1", configurable.getConfiguration().value());

        configurable.reconfigure(config2);
        assertEquals("value2", configurable.getConfiguration().value());
    }

    // Test configuration class
    private static class TestConfiguration implements Configuration {
        private final String value;

        TestConfiguration(String value) {
            this.value = value;
        }

        String value() {
            return value;
        }

        @Override
        public void validate() throws ConfigurationException {
            if (value == null || value.isBlank()) {
                throw new ConfigurationException("config.value", "Value cannot be null or blank");
            }
        }
    }
}
