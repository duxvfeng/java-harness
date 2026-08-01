package com.chachamaru.harness.foundation.recovery;

/**
 * 状态恢复异常
 */
public class RecoveryException extends Exception {

    private final ErrorType errorType;

    public enum ErrorType {
        FILE_NOT_FOUND,
        CORRUPTED_STATE,
        READ_ERROR,
        WRITE_ERROR,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }

    public RecoveryException(String message) {
        this(message, ErrorType.UNKNOWN_ERROR, null);
    }

    public RecoveryException(String message, Throwable cause) {
        this(message, ErrorType.UNKNOWN_ERROR, cause);
    }

    public RecoveryException(String message, ErrorType errorType) {
        this(message, errorType, null);
    }

    public RecoveryException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return "RecoveryException{" +
                "errorType=" + errorType +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
