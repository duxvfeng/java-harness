package com.chachamaru.harness.mode;

import java.util.Map;
import java.util.HashMap;

/**
 * 执行模式评分结果
 * 包含三种执行模式的评分和推荐信息
 */
public record ModeScores(
    double soloScore,         // SOLO 模式评分 [0, 1]
    double parallelScore,    // PARALLEL 模式评分 [0, 1]
    double breezingScore,    // BREEZING 模式评分 [0, 1]
    Map<ExecutionMode, Double> scoreBreakdown  // 评分详细分解
) {
    /**
     * 创建评分结果
     */
    public ModeScores {
        // 验证评分范围
        if (soloScore < 0 || soloScore > 1) {
            throw new IllegalArgumentException("SOLO 评分必须在 [0, 1] 范围内: " + soloScore);
        }
        if (parallelScore < 0 || parallelScore > 1) {
            throw new IllegalArgumentException("PARALLEL 评分必须在 [0, 1] 范围内: " + parallelScore);
        }
        if (breezingScore < 0 || breezingScore > 1) {
            throw new IllegalArgumentException("BREEZING 评分必须在 [0, 1] 范围内: " + breezingScore);
        }

        // 确保分解信息不为null
        if (scoreBreakdown == null) {
            scoreBreakdown = createDefaultBreakdown(soloScore, parallelScore, breezingScore);
        }
    }

    /**
     * 创建默认的评分分解
     */
    private static Map<ExecutionMode, Double> createDefaultBreakdown(
        double soloScore, double parallelScore, double breezingScore
    ) {
        Map<ExecutionMode, Double> breakdown = new HashMap<>();
        breakdown.put(ExecutionMode.SOLO, soloScore);
        breakdown.put(ExecutionMode.PARALLEL, parallelScore);
        breakdown.put(ExecutionMode.BREEZING, breezingScore);
        return breakdown;
    }

    /**
     * 获取推荐的最佳执行模式
     * @return 评分最高的执行模式
     */
    public ExecutionMode getRecommendedMode() {
        if (soloScore >= parallelScore && soloScore >= breezingScore) {
            return ExecutionMode.SOLO;
        } else if (parallelScore >= breezingScore) {
            return ExecutionMode.PARALLEL;
        } else {
            return ExecutionMode.BREEZING;
        }
    }

    /**
     * 获取最高评分
     * @return 三种模式中的最高评分
     */
    public double getHighestScore() {
        return Math.max(soloScore, Math.max(parallelScore, breezingScore));
    }

    /**
     * 获取指定模式的评分
     * @param mode 执行模式
     * @return 该模式的评分
     */
    public double getScore(ExecutionMode mode) {
        return switch (mode) {
            case SOLO -> soloScore;
            case PARALLEL -> parallelScore;
            case BREEZING -> breezingScore;
        };
    }

    /**
     * 检查评分是否明确（最高评分明显高于其他模式）
     * @param threshold 差异阈值（默认0.2）
     * @return 如果最高评分与其他模式差异超过阈值则返回true
     */
    public boolean isClearRecommendation(double threshold) {
        double highest = getHighestScore();
        double secondHighest = getSecondHighestScore();
        return (highest - secondHighest) >= threshold;
    }

    /**
     * 获取第二高的评分
     */
    private double getSecondHighestScore() {
        if (soloScore >= parallelScore && soloScore >= breezingScore) {
            // SOLO 最高，返回 PARALLEL 和 BREEZING 中的较高者
            return Math.max(parallelScore, breezingScore);
        } else if (parallelScore >= soloScore && parallelScore >= breezingScore) {
            // PARALLEL 最高，返回 SOLO 和 BREEZING 中的较高者
            return Math.max(soloScore, breezingScore);
        } else {
            // BREEZING 最高，返回 SOLO 和 PARALLEL 中的较高者
            return Math.max(soloScore, parallelScore);
        }
    }

    /**
     * 判断默认是否是明确推荐（使用0.2阈值）
     */
    public boolean isClearRecommendation() {
        return isClearRecommendation(0.2);
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 评分结果构建器
     */
    public static class Builder {
        private double soloScore = 0.0;
        private double parallelScore = 0.0;
        private double breezingScore = 0.0;
        private Map<ExecutionMode, Double> scoreBreakdown = new HashMap<>();

        public Builder soloScore(double score) {
            this.soloScore = score;
            return this;
        }

        public Builder parallelScore(double score) {
            this.parallelScore = score;
            return this;
        }

        public Builder breezingScore(double score) {
            this.breezingScore = score;
            return this;
        }

        public Builder scoreBreakdown(Map<ExecutionMode, Double> breakdown) {
            this.scoreBreakdown = new HashMap<>(breakdown);
            return this;
        }

        public ModeScores build() {
            // 如果分解信息不完整，自动填充
            if (!scoreBreakdown.containsKey(ExecutionMode.SOLO)) {
                scoreBreakdown.put(ExecutionMode.SOLO, soloScore);
            }
            if (!scoreBreakdown.containsKey(ExecutionMode.PARALLEL)) {
                scoreBreakdown.put(ExecutionMode.PARALLEL, parallelScore);
            }
            if (!scoreBreakdown.containsKey(ExecutionMode.BREEZING)) {
                scoreBreakdown.put(ExecutionMode.BREEZING, breezingScore);
            }

            return new ModeScores(soloScore, parallelScore, breezingScore, scoreBreakdown);
        }
    }
}