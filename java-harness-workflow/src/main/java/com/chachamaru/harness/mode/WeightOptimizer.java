package com.chachamaru.harness.mode;

/**
 * 权重优化器
 * 基于用户反馈优化评分权重配置
 */
public class WeightOptimizer {

    /**
     * 基于用户反馈历史优化权重
     * @param history 用户反馈历史
     * @return 优化后的权重配置
     */
    public ScoringWeights optimizeWeights(UserFeedbackHistory history) {
        if (history == null || history.isEmpty()) {
            return ScoringWeights.DEFAULT;
        }

        // 分析用户偏好模式
        int soloCount = history.getModeSelectionCount(ExecutionMode.SOLO);
        int parallelCount = history.getModeSelectionCount(ExecutionMode.PARALLEL);
        int breezingCount = history.getModeSelectionCount(ExecutionMode.BREEZING);

        int totalCount = soloCount + parallelCount + breezingCount;
        if (totalCount == 0) {
            return ScoringWeights.DEFAULT;
        }

        // 计算模式选择频率
        double soloFreq = (double) soloCount / totalCount;
        double parallelFreq = (double) parallelCount / totalCount;
        double breezingFreq = (double) breezingCount / totalCount;

        // 基于用户偏好调整权重
        double taskCountWeight = ScoringWeights.DEFAULT.taskCountWeight();
        double complexityWeight = ScoringWeights.DEFAULT.complexityWeight();
        double dependencyWeight = ScoringWeights.DEFAULT.dependencyWeight();
        double reviewRequirementWeight = ScoringWeights.DEFAULT.reviewRequirementWeight();

        // 如果用户倾向于SOLO，提高简单任务因素的权重
        if (soloFreq > 0.5) {
            taskCountWeight = Math.min(0.5, taskCountWeight + 0.1);
            reviewRequirementWeight = Math.max(0.05, reviewRequirementWeight - 0.05);
        }

        // 如果用户倾向于PARALLEL，平衡任务数量和复杂度权重
        if (parallelFreq > 0.4) {
            taskCountWeight = Math.min(0.45, taskCountWeight + 0.05);
            complexityWeight = Math.max(0.25, complexityWeight - 0.05);
        }

        // 如果用户倾向于BREEZING，提高复杂度权重
        if (breezingFreq > 0.3) {
            complexityWeight = Math.min(0.5, complexityWeight + 0.1);
            dependencyWeight = Math.min(0.3, dependencyWeight + 0.05);
        }

        // 确保权重总和为1.0
        double sum = taskCountWeight + complexityWeight + dependencyWeight + reviewRequirementWeight;
        if (sum != 1.0) {
            taskCountWeight /= sum;
            complexityWeight /= sum;
            dependencyWeight /= sum;
            reviewRequirementWeight /= sum;
        }

        return new ScoringWeights(taskCountWeight, complexityWeight, dependencyWeight, reviewRequirementWeight);
    }

    /**
     * 渐进式优化权重
     * @param currentWeights 当前权重
     * @param history 用户反馈历史
     * @param learningRate 学习率（0-1）
     * @return 更新后的权重
     */
    public ScoringWeights progressiveOptimize(
        ScoringWeights currentWeights,
        UserFeedbackHistory history,
        double learningRate
    ) {
        if (currentWeights == null) {
            currentWeights = ScoringWeights.DEFAULT;
        }

        ScoringWeights targetWeights = optimizeWeights(history);

        // 渐进式更新权重
        double newTaskCountWeight = linearInterpolation(
            currentWeights.taskCountWeight(),
            targetWeights.taskCountWeight(),
            learningRate
        );

        double newComplexityWeight = linearInterpolation(
            currentWeights.complexityWeight(),
            targetWeights.complexityWeight(),
            learningRate
        );

        double newDependencyWeight = linearInterpolation(
            currentWeights.dependencyWeight(),
            targetWeights.dependencyWeight(),
            learningRate
        );

        double newReviewRequirementWeight = linearInterpolation(
            currentWeights.reviewRequirementWeight(),
            targetWeights.reviewRequirementWeight(),
            learningRate
        );

        // 归一化确保总和为1.0
        double sum = newTaskCountWeight + newComplexityWeight + newDependencyWeight + newReviewRequirementWeight;
        return new ScoringWeights(
            newTaskCountWeight / sum,
            newComplexityWeight / sum,
            newDependencyWeight / sum,
            newReviewRequirementWeight / sum
        );
    }

    /**
     * 线性插值
     */
    private double linearInterpolation(double start, double end, double t) {
        return start + t * (end - start);
    }
}