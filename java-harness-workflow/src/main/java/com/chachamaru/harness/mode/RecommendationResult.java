package com.chachamaru.harness.mode;

/**
 * 推荐结果（包含调试信息）
 * 包含推荐结果和中间分析过程，用于调试和学习
 */
public record RecommendationResult(
    ModeRecommendation recommendation,    // 最终推荐结果
    TaskCharacteristics characteristics,  // 任务特征分析结果
    ModeScores scores                   // 模式评分结果
) {
    /**
     * 创建推荐结果
     */
    public RecommendationResult {
        if (recommendation == null) {
            throw new IllegalArgumentException("推荐结果不能为null");
        }
        if (characteristics == null) {
            throw new IllegalArgumentException("任务特征不能为null");
        }
        if (scores == null) {
            throw new IllegalArgumentException("评分结果不能为null");
        }
    }

    /**
     * 获取推荐的执行模式
     */
    public ExecutionMode getRecommendedMode() {
        return recommendation.recommendedMode();
    }

    /**
     * 获取推荐置信度
     */
    public double getConfidence() {
        return recommendation.confidence();
    }

    /**
     * 获取推荐理由
     */
    public String getReason() {
        return recommendation.reason();
    }

    /**
     * 获取备选方案
     */
    public java.util.List<ExecutionMode> getAlternativeModes() {
        return recommendation.alternativeModes();
    }

    /**
     * 判断是否为高置信度推荐
     */
    public boolean isHighConfidence() {
        return getConfidence() >= 0.8;
    }

    /**
     * 判断是否为低置信度推荐
     */
    public boolean isLowConfidence() {
        return getConfidence() < 0.6;
    }

    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 推荐结果构建器
     */
    public static class Builder {
        private ModeRecommendation recommendation;
        private TaskCharacteristics characteristics;
        private ModeScores scores;

        public Builder recommendation(ModeRecommendation recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public Builder characteristics(TaskCharacteristics characteristics) {
            this.characteristics = characteristics;
            return this;
        }

        public Builder scores(ModeScores scores) {
            this.scores = scores;
            return this;
        }

        public RecommendationResult build() {
            return new RecommendationResult(recommendation, characteristics, scores);
        }
    }
}