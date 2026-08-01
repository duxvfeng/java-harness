package com.chachamaru.harness.foundation.sync;

/**
 * 同步异常
 * 在同步过程中发生错误时抛出
 */
public class SyncException extends Exception {

    private final ErrorType errorType;

    public enum ErrorType {
        PARSE_ERROR,
        WRITE_ERROR,
        READ_ERROR,
        CONFLICT_ERROR,
        VALIDATION_ERROR,
        STATE_ERROR,
        UNKNOWN_ERROR
    }

    public SyncException(String message) {
        this(message, ErrorType.UNKNOWN_ERROR, null);
    }

    public SyncException(String message, Throwable cause) {
        this(message, ErrorType.UNKNOWN_ERROR, cause);
    }

    public SyncException(String message, ErrorType errorType) {
        this(message, errorType, null);
    }

    public SyncException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return "SyncException{" +
                "errorType=" + errorType +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
