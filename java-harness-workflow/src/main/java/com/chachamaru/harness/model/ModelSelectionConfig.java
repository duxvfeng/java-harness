package com.chachamaru.harness.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型选择系统的总配置类
 * 管理所有等级配置和全局设置
 *
 * <p>主要职责：</p>
 * <ul>
 *   <li>管理四个模型等级的配置（FAST/BALANCED/QUALITY/POWERFUL）</li>
 *   <li>控制智能模型选择功能的启用/禁用</li>
 *   <li>配置降级策略的超时和重试参数</li>
 *   <li>配置模型选择策略（effortBased/costBased等）</li>
 * </ul>
 *
 * <p>配置示例：</p>
 * <pre>{@code
 * Map<ModelTier, TierConfig> tierConfigs = Map.of(
 *     ModelTier.FAST, new TierConfig(ModelTier.FAST, "ENV_VAR", new String[]{"model1", "model2"}),
 *     ModelTier.BALANCED, new TierConfig(ModelTier.BALANCED, "ENV_VAR", new String[]{"model1", "model2"})
 * );
 *
 * ModelSelectionConfig config = new ModelSelectionConfig(
 *     true,           // enabled
 *     "effortBased",  // strategy
 *     tierConfigs,    // tier configurations
 *     5000,           // timeout in milliseconds
 *     3,              // max attempts
 *     false           // validate API calls
 * );
 * }</pre>
 */
public class ModelSelectionConfig {

    private final boolean enabled;
    private final String strategy;
    private final Map<ModelTier, TierConfig> tierConfigs;
    private final int timeout;
    private final int maxAttempts;
    private final boolean validateApiCall;

    /**
     * 创建模型选择配置
     *
     * @param enabled 是否启用智能模型选择
     * @param strategy 选择策略（effortBased/costBased等）
     * @param tierConfigs 各等级配置映射
     * @param timeout 超时时间（毫秒）
     * @param maxAttempts 最大尝试次数
     * @param validateApiCall 是否验证API调用
     */
    public ModelSelectionConfig(
            boolean enabled,
            String strategy,
            Map<ModelTier, TierConfig> tierConfigs,
            int timeout,
            int maxAttempts,
            boolean validateApiCall) {
        this.enabled = enabled;
        this.strategy = strategy;
        // 创建不可变映射的防御性拷贝
        this.tierConfigs = tierConfigs != null
                ? Map.copyOf(tierConfigs)
                : Map.of();
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
        this.validateApiCall = validateApiCall;
    }

    /**
     * 检查是否启用智能模型选择
     * @return true 表示启用，false 表示禁用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取选择策略
     * @return 策略名称
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * 获取所有等级配置
     * @return 不可变的等级配置映射
     */
    public Map<ModelTier, TierConfig> getTierConfigs() {
        return tierConfigs;
    }

    /**
     * 根据等级获取配置
     * @param tier 模型等级
     * @return 包含配置的Optional，如果未找到返回空Optional
     */
    public Optional<TierConfig> getTierConfig(ModelTier tier) {
        return Optional.ofNullable(tierConfigs.get(tier));
    }

    /**
     * 获取所有已配置的等级
     * @return 已配置等级的集合
     */
    public Set<ModelTier> getAllTiers() {
        return tierConfigs.keySet();
    }

    /**
     * 获取超时时间
     * @return 超时时间（毫秒）
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * 获取最大尝试次数
     * @return 最大尝试次数
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * 检查是否验证API调用
     * @return true 表示验证，false 表示跳过验证
     */
    public boolean isValidateApiCall() {
        return validateApiCall;
    }

    /**
     * 验证配置的有效性
     *
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        if (tierConfigs == null || tierConfigs.isEmpty()) {
            throw new IllegalArgumentException("tier configs cannot be null or empty");
        }

        if (timeout < 0) {
            throw new IllegalArgumentException("timeout cannot be negative: " + timeout);
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("max attempts must be positive: " + maxAttempts);
        }

        // 验证所有等级配置
        for (Map.Entry<ModelTier, TierConfig> entry : tierConfigs.entrySet()) {
            try {
                entry.getValue().validate();
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid config for tier " + entry.getKey() + ": " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelSelectionConfig that = (ModelSelectionConfig) o;
        return enabled == that.enabled &&
               timeout == that.timeout &&
               maxAttempts == that.maxAttempts &&
               validateApiCall == that.validateApiCall &&
               Objects.equals(strategy, that.strategy) &&
               Objects.equals(tierConfigs, that.tierConfigs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, strategy, tierConfigs, timeout, maxAttempts, validateApiCall);
    }

    @Override
    public String toString() {
        return "ModelSelectionConfig{" +
               "enabled=" + enabled +
               ", strategy='" + strategy + '\'' +
               ", tierConfigsCount=" + tierConfigs.size() +
               ", timeout=" + timeout +
               ", maxAttempts=" + maxAttempts +
               ", validateApiCall=" + validateApiCall +
               '}';
    }
}