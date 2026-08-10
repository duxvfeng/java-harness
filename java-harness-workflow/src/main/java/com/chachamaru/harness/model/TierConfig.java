package com.chachamaru.harness.model;

import java.util.Arrays;
import java.util.Objects;

/**
 * 单个模型等级的配置类
 * 包含降级链列表和验证逻辑
 *
 * <p>每个模型等级（FAST/BALANCED/QUALITY/POWERFUL）都有一个对应的配置实例，
 * 包含：</p>
 * <ul>
 *   <li>等级标识（ModelTier）</li>
 *   <li>主要环境变量名称</li>
 *   <li>降级链（fallback chain）- 按优先级排列的候选模型列表</li>
 * </ul>
 *
 * <p>降级机制：</p>
 * <ol>
 *   <li>首先尝试主要环境变量指定的模型</li>
 *   <li>如果不可用，依次尝试降级链中的候选模型</li>
 *   <li>支持环境变量引用（env:VAR_NAME）和直接模型名称</li>
 * </ol>
 */
public class TierConfig {

    private final ModelTier tier;
    private final String modelEnv;
    private final String[] fallbackChain;

    /**
     * 创建等级配置
     *
     * @param tier 模型等级
     * @param modelEnv 主要环境变量名称
     * @param fallbackChain 降级链，按优先级排列的候选模型列表
     */
    public TierConfig(ModelTier tier, String modelEnv, String[] fallbackChain) {
        this.tier = tier;
        this.modelEnv = modelEnv;
        // 防御性拷贝，确保不可变性
        this.fallbackChain = fallbackChain != null ? Arrays.copyOf(fallbackChain, fallbackChain.length) : null;
    }

    /**
     * 获取模型等级
     * @return 模型等级枚举
     */
    public ModelTier getTier() {
        return tier;
    }

    /**
     * 获取等级名称
     * @return 等级名称字符串
     */
    public String getTierName() {
        return tier != null ? tier.name() : "UNKNOWN";
    }

    /**
     * 获取主要环境变量名称
     * @return 环境变量名称
     */
    public String getModelEnv() {
        return modelEnv;
    }

    /**
     * 获取降级链
     * @return 降级链数组（防御性拷贝）
     */
    public String[] getFallbackChain() {
        return fallbackChain != null ? Arrays.copyOf(fallbackChain, fallbackChain.length) : null;
    }

    /**
     * 检查是否有有效的降级链
     * @return 如果降级链非空且包含至少一个模型返回 true，否则返回 false
     */
    public boolean hasValidFallbackChain() {
        return fallbackChain != null && fallbackChain.length > 0;
    }

    /**
     * 验证配置的有效性
     *
     * @throws IllegalArgumentException 如果配置无效
     */
    public void validate() {
        if (tier == null) {
            throw new IllegalArgumentException("tier cannot be null");
        }

        if (modelEnv == null || modelEnv.trim().isEmpty()) {
            throw new IllegalArgumentException("modelEnv cannot be null or empty");
        }

        if (!hasValidFallbackChain()) {
            throw new IllegalArgumentException("fallback chain cannot be null or empty");
        }

        // 验证降级链中没有空字符串
        if (fallbackChain != null) {
            for (int i = 0; i < fallbackChain.length; i++) {
                if (fallbackChain[i] == null || fallbackChain[i].trim().isEmpty()) {
                    throw new IllegalArgumentException("fallback chain contains null or empty model at index " + i);
                }
            }
        }
    }

    /**
     * 获取显示名称（用于日志和调试）
     * @return 显示名称
     */
    public String getDisplayName() {
        return String.format("%s[%s]", tier != null ? tier.name() : "UNKNOWN", modelEnv);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TierConfig that = (TierConfig) o;
        return tier == that.tier &&
               Objects.equals(modelEnv, that.modelEnv) &&
               Arrays.equals(fallbackChain, that.fallbackChain);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(tier, modelEnv);
        result = 31 * result + Arrays.hashCode(fallbackChain);
        return result;
    }

    @Override
    public String toString() {
        return "TierConfig{" +
               "tier=" + tier +
               ", modelEnv='" + modelEnv + '\'' +
               ", fallbackChainLength=" + (fallbackChain != null ? fallbackChain.length : 0) +
               '}';
    }
}