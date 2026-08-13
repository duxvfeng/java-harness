package com.chachamaru.harness.isolation;

/**
 * Exception thrown when state file operations fail.
 */
public class StateException extends Exception {

    private final String operation;
    private final String filePath;

    public StateException(String message) {
        super(message);
        this.operation = "unknown";
        this.filePath = "unknown";
    }

    public StateException(String message, Throwable cause) {
        super(message, cause);
        this.operation = "unknown";
        this.filePath = "unknown";
    }

    public StateException(String operation, String filePath, String message, Throwable cause) {
        super(message + " (operation: " + operation + ", file: " + filePath + ")", cause);
        this.operation = operation;
        this.filePath = filePath;
    }

    public String getOperation() {
        return operation;
    }

    public String getFilePath() {
        return filePath;
    }
}