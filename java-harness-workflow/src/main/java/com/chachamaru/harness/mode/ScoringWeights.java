package com.chachamaru.harness.mode;

/**
 * 评分权重配置
 * 用于配置不同任务特征在评分算法中的权重
 */
public record ScoringWeights(
    double taskCountWeight,           // 任务数量权重
    double complexityWeight,           // 复杂度权重
    double dependencyWeight,           // 依赖关系权重
    double reviewRequirementWeight     // 审查需求权重
) {
    /**
     * 默认权重配置
     * 基于经验和实际效果优化得出
     */
    public static final ScoringWeights DEFAULT = new ScoringWeights(
        0.35,  // 任务数量权重 35% - 任务数量是影响执行模式选择的首要因素
        0.35,  // 复杂度权重 35% - 复杂度决定了需要多少协调和审查
        0.20,  // 依赖关系权重 20% - 依赖关系影响并行效果
        0.10   // 审查需求权重 10% - 审查需求影响是否需要独立Reviewer
    );

    /**
     * 验证权重配置的有效性
     */
    public ScoringWeights {
        if (taskCountWeight < 0 || taskCountWeight > 1) {
            throw new IllegalArgumentException("任务数量权重必须在 [0, 1] 范围内");
        }
        if (complexityWeight < 0 || complexityWeight > 1) {
            throw new IllegalArgumentException("复杂度权重必须在 [0, 1] 范围内");
        }
        if (dependencyWeight < 0 || dependencyWeight > 1) {
            throw new IllegalArgumentException("依赖关系权重必须在 [0, 1] 范围内");
        }
        if (reviewRequirementWeight < 0 || reviewRequirementWeight > 1) {
            throw new IllegalArgumentException("审查需求权重必须在 [0, 1] 范围内");
        }

        double sum = taskCountWeight + complexityWeight + dependencyWeight + reviewRequirementWeight;
        if (Math.abs(sum - 1.0) > 0.001) {
            throw new IllegalArgumentException("权重总和必须等于1.0，当前为: " + sum);
        }
    }

    /**
     * 创建权重构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 权重构建器
     */
    public static class Builder {
        private double taskCountWeight = 0.35;
        private double complexityWeight = 0.35;
        private double dependencyWeight = 0.20;
        private double reviewRequirementWeight = 0.10;

        public Builder taskCountWeight(double weight) {
            this.taskCountWeight = weight;
            return this;
        }

        public Builder complexityWeight(double weight) {
            this.complexityWeight = weight;
            return this;
        }

        public Builder dependencyWeight(double weight) {
            this.dependencyWeight = weight;
            return this;
        }

        public Builder reviewRequirementWeight(double weight) {
            this.reviewRequirementWeight = weight;
            return this;
        }

        public ScoringWeights build() {
            return new ScoringWeights(
                taskCountWeight,
                complexityWeight,
                dependencyWeight,
                reviewRequirementWeight
            );
        }
    }
}