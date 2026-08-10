package com.chachamaru.harness.model;

/**
 * 模型等级枚举
 * 基于复杂度分数的模型等级映射，支持环境变量映射
 *
 * <p>映射策略：</p>
 * <ul>
 *   <li>FAST (0-2分): 使用快速/便宜的模型，适用于简单任务</li>
 *   <li>BALANCED (3-4分): 使用平衡模型，适用于中等复杂度任务</li>
 *   <li>QUALITY (5-6分): 使用高质量模型，适用于复杂任务</li>
 *   <li>POWERFUL (≥7分): 使用强大模型，适用于超高复杂度任务</li>
 * </ul>
 *
 * <p>环境变量映射：</p>
 * <ul>
 *   <li>FAST → ANTHROPIC_DEFAULT_FABLE_MODEL</li>
 *   <li>BALANCED → ANTHROPIC_DEFAULT_HAIKU_MODEL</li>
 *   <li>QUALITY → ANTHROPIC_DEFAULT_SONNET_MODEL</li>
 *   <li>POWERFUL → ANTHROPIC_DEFAULT_OPUS_MODEL</li>
 * </ul>
 */
public enum ModelTier {

    /**
     * 快速模型等级（0-2分）
     * 用于低复杂度任务，如简单文本生成、格式转换等
     */
    FAST(0, 2, "ANTHROPIC_DEFAULT_FABLE_MODEL"),

    /**
     * 平衡模型等级（3-4分）
     * 用于中等复杂度任务，如代码审查、文档编写等
     */
    BALANCED(3, 4, "ANTHROPIC_DEFAULT_HAIKU_MODEL"),

    /**
     * 高质量模型等级（5-6分）
     * 用于高复杂度任务，如架构设计、复杂问题分析等
     */
    QUALITY(5, 6, "ANTHROPIC_DEFAULT_SONNET_MODEL"),

    /**
     * 强大模型等级（≥7分）
     * 用于超高复杂度任务，如系统重构、多组件协调等
     */
    POWERFUL(7, 999, "ANTHROPIC_DEFAULT_OPUS_MODEL");

    private final int minScore;
    private final int maxScore;
    private final String envVariable;

    ModelTier(int minScore, int maxScore, String envVariable) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.envVariable = envVariable;
    }

    /**
     * 获取该等级的最小分数
     * @return 最小分数
     */
    public int getMinScore() {
        return minScore;
    }

    /**
     * 获取该等级的最大分数
     * @return 最大分数
     */
    public int getMaxScore() {
        return maxScore;
    }

    /**
     * 获取该等级对应的环境变量名称
     * @return 环境变量名称
     */
    public String getEnvVariable() {
        return envVariable;
    }

    /**
     * 根据复杂度分数确定对应的模型等级
     *
     * @param score 复杂度分数（0-999）
     * @return 对应的模型等级
     * @throws IllegalArgumentException 如果分数为负数（内部处理，返回FAST）
     */
    public static ModelTier fromScore(int score) {
        if (score < 0) {
            // 负数分数默认使用 FAST
            return FAST;
        }
        if (score <= 2) {
            return FAST;
        } else if (score <= 4) {
            return BALANCED;
        } else if (score <= 6) {
            return QUALITY;
        } else {
            return POWERFUL;
        }
    }

    /**
     * 检查给定分数是否在该等级的范围内
     *
     * @param score 要检查的分数
     * @return 如果分数在该等级范围内返回 true，否则返回 false
     */
    public boolean containsScore(int score) {
        return score >= minScore && score <= maxScore;
    }

    /**
     * 获取该等级的显示名称（用于日志和调试）
     * @return 显示名称
     */
    public String getDisplayName() {
        return name() + " (" + minScore + "-" + (maxScore == 999 ? "∞" : maxScore) + ")";
    }
}