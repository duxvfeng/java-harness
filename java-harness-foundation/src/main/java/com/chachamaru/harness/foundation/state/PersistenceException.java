package com.chachamaru.harness.foundation.state;

/**
 * 状态持久化异常
 * 在状态保存、加载或删除过程中发生错误时抛出
 */
public class PersistenceException extends Exception {

    private final ErrorType errorType;

    public enum ErrorType {
        SERIALIZATION_ERROR,
        DESERIALIZATION_ERROR,
        FILE_WRITE_ERROR,
        FILE_READ_ERROR,
        FILE_NOT_FOUND,
        DIRECTORY_CREATE_ERROR,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }

    public PersistenceException(String message) {
        this(message, ErrorType.UNKNOWN_ERROR, null);
    }

    public PersistenceException(String message, Throwable cause) {
        this(message, ErrorType.UNKNOWN_ERROR, cause);
    }

    public PersistenceException(String message, ErrorType errorType) {
        this(message, errorType, null);
    }

    public PersistenceException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    @Override
    public String toString() {
        return "PersistenceException{" +
                "errorType=" + errorType +
                ", message='" + getMessage() + '\'' +
                '}';
    }
}
