package com.chachamaru.harness.collaboration.config;

/**
 * Exception thrown when configuration parsing fails.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>Configuration file cannot be read</li>
 *   <li>Configuration syntax is invalid</li>
 *   <li>Required configuration fields are missing</li>
 *   <li>Configuration values are invalid</li>
 * </ul>
 *
 * @spec_reference Phase 7: Dual Platform Support
 */
public class ConfigParseException extends Exception {

    /**
     * Creates a new config parse exception.
     *
     * @param message the error message
     */
    public ConfigParseException(String message) {
        super(message);
    }

    /**
     * Creates a new config parse exception with cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new config parse exception.
     *
     * @param cause the underlying cause
     */
    public ConfigParseException(Throwable cause) {
        super(cause);
    }
}
