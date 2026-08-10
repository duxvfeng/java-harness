package com.chachamaru.harness.model;

/**
 * 模型等级枚举
 * 定义四个复杂度等级和对应的模型映射
 */
public enum ModelTier {

    /**
     * FAST 等级 - 低复杂度任务 (0-2分)
     * 使用快速模型如 FABLE
     */
    FAST(0, 2, "ANTHROPIC_DEFAULT_FABLE_MODEL", "FABLE"),

    /**
     * BALANCED 等级 - 中等复杂度任务 (3-4分)
     * 使用平衡模型如 HAIKU
     */
    BALANCED(3, 4, "ANTHROPIC_DEFAULT_HAIKU_MODEL", "HAIKU"),

    /**
     * QUALITY 等级 - 高复杂度任务 (5-6分)
     * 使用质量模型如 SONNET
     */
    QUALITY(5, 6, "ANTHROPIC_DEFAULT_SONNET_MODEL", "SONNET"),

    /**
     * POWERFUL 等级 - 超高复杂度任务 (7+分)
     * 使用强大模型如 OPUS
     */
    POWERFUL(7, Integer.MAX_VALUE, "ANTHROPIC_DEFAULT_OPUS_MODEL", "OPUS");

    private final int minScore;
    private final int maxScore;
    private final String modelEnv;
    private final String defaultModel;

    ModelTier(int minScore, int maxScore, String modelEnv, String defaultModel) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.modelEnv = modelEnv;
        this.defaultModel = defaultModel;
    }

    /**
     * 获取最低分数
     */
    public int getMinScore() {
        return minScore;
    }

    /**
     * 获取最高分数
     */
    public int getMaxScore() {
        return maxScore;
    }

    /**
     * 获取环境变量名称
     */
    public String getModelEnv() {
        return modelEnv;
    }

    /**
     * 获取默认模型名称
     */
    public String getDefaultModel() {
        return defaultModel;
    }

    /**
     * 根据分数获取对应的模型等级
     */
    public static ModelTier fromScore(int score) {
        for (ModelTier tier : values()) {
            if (score >= tier.minScore && score <= tier.maxScore) {
                return tier;
            }
        }
        return POWERFUL; // 默认返回最高等级
    }
}