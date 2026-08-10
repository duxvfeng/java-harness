package com.chachamaru.harness.model;

/**
 * 模型不可用异常
 * 当所有候选模型都不可用时抛出此异常
 */
public class ModelUnavailableException extends RuntimeException {

    private final ModelTier tier;

    /**
     * 创建模型不可用异常
     * @param message 异常消息
     */
    public ModelUnavailableException(String message) {
        super(message);
        this.tier = null;
    }

    /**
     * 创建模型不可用异常（带等级信息）
     * @param tier 模型等级
     * @param message 异常消息
     */
    public ModelUnavailableException(ModelTier tier, String message) {
        super(message);
        this.tier = tier;
    }

    /**
     * 创建模型不可用异常（带原因）
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ModelUnavailableException(String message, Throwable cause) {
        super(message, cause);
        this.tier = null;
    }

    /**
     * 创建模型不可用异常（完整信息）
     * @param tier 模型等级
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ModelUnavailableException(ModelTier tier, String message, Throwable cause) {
        super(message, cause);
        this.tier = tier;
    }

    /**
     * 获取模型等级
     * @return 模型等级，如果没有则为 null
     */
    public ModelTier getTier() {
        return tier;
    }

    /**
     * 检查是否有等级信息
     * @return 如果有等级信息返回 true
     */
    public boolean hasTier() {
        return tier != null;
    }
}