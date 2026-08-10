package com.chachamaru.harness.model;

import java.util.*;

/**
 * 模型选择系统的总配置类
 * 管理所有等级配置和全局设置
 */
public class ModelSelectionConfig {

    private final boolean enabled;
    private final String strategy;
    private final Map<ModelTier, TierConfig> tierConfigs;

    /**
     * 创建默认配置
     */
    public ModelSelectionConfig() {
        this(true, "effortBased", createDefaultTierConfigs());
    }

    /**
     * 创建自定义配置
     */
    public ModelSelectionConfig(boolean enabled, String strategy, Map<ModelTier, TierConfig> tierConfigs) {
        this.enabled = enabled;
        this.strategy = strategy;
        this.tierConfigs = tierConfigs != null ? new HashMap<>(tierConfigs) : new HashMap<>();
    }

    /**
     * 创建默认等级配置
     */
    private static Map<ModelTier, TierConfig> createDefaultTierConfigs() {
        Map<ModelTier, TierConfig> configs = new HashMap<>();

        for (ModelTier tier : ModelTier.values()) {
            String[] fallbackModels = {
                "env:" + tier.getModelEnv(),
                "env:ANTHROPIC_MODEL",
                "glm-4.7"
            };
            configs.put(tier, new TierConfig(tier, tier.getModelEnv(), fallbackModels));
        }

        return configs;
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 获取策略名称
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * 获取所有等级配置
     */
    public Map<ModelTier, TierConfig> getTierConfigs() {
        return new HashMap<>(tierConfigs);
    }

    /**
     * 获取特定等级配置
     */
    public TierConfig getTierConfig(ModelTier tier) {
        return tierConfigs.get(tier);
    }

    /**
     * 验证配置有效性
     */
    public void validate() {
        if (tierConfigs.isEmpty()) {
            throw new IllegalArgumentException("Tier configs cannot be empty");
        }
        for (TierConfig config : tierConfigs.values()) {
            config.validate();
        }
    }
}