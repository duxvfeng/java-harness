package com.chachamaru.harness.protocol;

/**
 * Exception thrown when hook codec operations fail.
 *
 * @since 4.1.0
 */
public class HookCodecException extends Exception {

    /**
     * Constructs a new hook codec exception with the specified detail message.
     *
     * @param message the detail message
     */
    public HookCodecException(String message) {
        super(message);
    }

    /**
     * Constructs a new hook codec exception with the specified cause.
     *
     * @param cause the cause
     */
    public HookCodecException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new hook codec exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public HookCodecException(String message, Throwable cause) {
        super(message, cause);
    }
}
