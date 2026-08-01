package com.chachamaru.harness.foundation.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigurationException.
 */
@DisplayName("ConfigurationException Tests")
class ConfigurationExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateExceptionWithMessage() {
        ConfigurationException exception = new ConfigurationException("Configuration error");
        assertEquals("Configuration error", exception.getMessage());
        assertNull(exception.getConfigKey());
    }

    @Test
    @DisplayName("Should create exception with config key and message")
    void shouldCreateExceptionWithConfigKeyAndMessage() {
        ConfigurationException exception = new ConfigurationException("timeout", "Invalid timeout value");
        assertEquals("[timeout] Invalid timeout value", exception.getMessage());
        assertEquals("timeout", exception.getConfigKey());
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateExceptionWithMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        ConfigurationException exception = new ConfigurationException("Configuration error", cause);

        assertEquals("Configuration error", exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertNull(exception.getConfigKey());
    }

    @Test
    @DisplayName("Should create exception with config key, message and cause")
    void shouldCreateExceptionWithConfigKeyMessageAndCause() {
        Throwable cause = new RuntimeException("Root cause");
        ConfigurationException exception = new ConfigurationException("port", "Invalid port", cause);

        assertEquals("[port] Invalid port", exception.getMessage());
        assertEquals("port", exception.getConfigKey());
        assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("Should maintain exception type")
    void shouldMaintainExceptionType() {
        ConfigurationException exception = new ConfigurationException("Test error");
        assertTrue(exception instanceof ConfigurationException);
        assertTrue(exception instanceof Exception);
    }

    @Test
    @DisplayName("Should handle null config key")
    void shouldHandleNullConfigKey() {
        ConfigurationException exception = new ConfigurationException("Test error");
        assertNull(exception.getConfigKey());
    }

    @Test
    @DisplayName("Should format message correctly with config key")
    void shouldFormatMessageCorrectlyWithConfigKey() {
        ConfigurationException exception = new ConfigurationException("server.url", "Invalid URL format");
        assertTrue(exception.getMessage().contains("[server.url]"));
        assertTrue(exception.getMessage().contains("Invalid URL format"));
    }
}
