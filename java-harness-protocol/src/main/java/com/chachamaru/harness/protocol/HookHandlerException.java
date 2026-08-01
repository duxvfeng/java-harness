package com.chachamaru.harness.protocol;

/**
 * Exception thrown when a hook handler fails during execution.
 *
 * @since 4.1.0
 */
public class HookHandlerException extends Exception {

    /**
     * Constructs a new hook handler exception with the specified detail message.
     *
     * @param message the detail message
     */
    public HookHandlerException(String message) {
        super(message);
    }

    /**
     * Constructs a new hook handler exception with the specified cause.
     *
     * @param cause the cause
     */
    public HookHandlerException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new hook handler exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public HookHandlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
