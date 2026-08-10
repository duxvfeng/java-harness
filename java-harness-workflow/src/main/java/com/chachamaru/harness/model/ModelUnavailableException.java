package com.chachamaru.harness.model;

/**
 * 模型不可用异常
 * 当降级链中的所有模型都不可用时抛出
 */
public class ModelUnavailableException extends Exception {

    /**
     * 创建异常
     */
    public ModelUnavailableException(String message) {
        super(message);
    }

    /**
     * 创建异常（带原因）
     */
    public ModelUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}