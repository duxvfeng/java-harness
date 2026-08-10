package com.chachamaru.harness.model;

import java.util.*;

/**
 * 单个模型等级的配置类
 * 包含降级链列表和验证逻辑
 */
public class TierConfig {

    private final ModelTier tier;
    private final String modelEnv;
    private final String[] fallbackModels;

    /**
     * 创建等级配置
     */
    public TierConfig(ModelTier tier, String modelEnv, String[] fallbackModels) {
        this.tier = tier;
        this.modelEnv = modelEnv;
        this.fallbackModels = fallbackModels != null ? Arrays.copyOf(fallbackModels, fallbackModels.length) : new String[0];
    }

    /**
     * 获取模型等级
     */
    public ModelTier getTier() {
        return tier;
    }

    /**
     * 获取环境变量名称
     */
    public String getModelEnv() {
        return modelEnv;
    }

    /**
     * 获取降级模型列表
     */
    public String[] getFallbackModels() {
        return Arrays.copyOf(fallbackModels, fallbackModels.length);
    }

    /**
     * 验证配置有效性
     */
    public void validate() {
        if (tier == null) {
            throw new IllegalArgumentException("Model tier cannot be null");
        }
        if (modelEnv == null || modelEnv.trim().isEmpty()) {
            throw new IllegalArgumentException("Model environment variable cannot be null or empty");
        }
        if (fallbackModels == null || fallbackModels.length == 0) {
            throw new IllegalArgumentException("Fallback models cannot be null or empty");
        }
    }
}