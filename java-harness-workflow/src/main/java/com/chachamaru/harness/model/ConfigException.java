package com.chachamaru.harness.model;

/**
 * 模型选择配置异常
 * 当配置无效、环境变量缺失或格式错误时抛出
 */
public class ConfigException extends RuntimeException {

    private final String configKey;

    /**
     * 创建配置异常（简单消息）
     * @param message 异常消息
     */
    public ConfigException(String message) {
        super(message);
        this.configKey = null;
    }

    /**
     * 创建配置异常（带原因）
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
        this.configKey = null;
    }

    /**
     * 创建配置异常（带配置键）
     * @param configKey 配置键
     * @param message 异常消息
     */
    public ConfigException(String configKey, String message) {
        super("[" + configKey + "] " + message);
        this.configKey = configKey;
    }

    /**
     * 创建配置异常（带配置键和原因）
     * @param configKey 配置键
     * @param message 异常消息
     * @param cause 原因异常
     */
    public ConfigException(String configKey, String message, Throwable cause) {
        super("[" + configKey + "] " + message, cause);
        this.configKey = configKey;
    }

    /**
     * 获取配置键
     * @return 配置键，如果没有则为 null
     */
    public String getConfigKey() {
        return configKey;
    }

    /**
     * 检查是否有配置键
     * @return 如果有配置键返回 true
     */
    public boolean hasConfigKey() {
        return configKey != null && !configKey.isEmpty();
    }
}