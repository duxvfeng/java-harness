package com.chachamaru.harness.mode;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能执行模式推荐引擎
 * 整合任务分析、评分计算、推荐生成的完整流程
 */
public class ModeRecommender {

    private final TaskAnalyzer analyzer;
    private final ModeScorer scorer;
    private final RecommendationGenerator generator;

    /**
     * 使用默认配置构造
     */
    public ModeRecommender() {
        this(ScoringWeights.DEFAULT);
    }

    /**
     * 使用自定义权重配置构造
     * @param weights 评分权重配置
     */
    public ModeRecommender(ScoringWeights weights) {
        this.analyzer = new TaskAnalyzer();
        this.scorer = new ModeScorer(weights != null ? weights : ScoringWeights.DEFAULT);
        this.generator = new RecommendationGenerator();
    }

    /**
     * 为任务生成推荐（最简单的API）
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @return 推荐结果
     */
    public ModeRecommendation recommend(List<String> tasks, List<String> files) {
        return recommend(tasks, files, false, null);
    }

    /**
     * 为任务生成推荐（带失败历史）
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @param hasFailureHistory 是否有失败历史
     * @return 推荐结果
     */
    public ModeRecommendation recommend(List<String> tasks, List<String> files, boolean hasFailureHistory) {
        return recommend(tasks, files, hasFailureHistory, null);
    }

    /**
     * 为任务生成推荐（完整API）
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @param hasFailureHistory 是否有失败历史
     * @param explicitEffort 显式指定的 effort 等级
     * @return 推荐结果
     */
    public ModeRecommendation recommend(
        List<String> tasks,
        List<String> files,
        boolean hasFailureHistory,
        String explicitEffort
    ) {
        // 1. 分析任务特征
        TaskCharacteristics characteristics = analyzer.analyzeTask(tasks, files, hasFailureHistory, explicitEffort);

        // 2. 计算模式评分
        ModeScores scores = scorer.scoreModes(characteristics);

        // 3. 生成推荐结果
        ModeRecommendation recommendation = generator.generate(characteristics, scores);

        return recommendation;
    }

    /**
     * 为任务生成推荐（带调试信息的API）
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @return 包含调试信息的推荐结果
     */
    public RecommendationResult recommendWithDebugInfo(List<String> tasks, List<String> files) {
        return recommendWithDebugInfo(tasks, files, false, null);
    }

    /**
     * 为任务生成推荐（带调试信息和失败历史）
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @param hasFailureHistory 是否有失败历史
     * @return 包含调试信息的推荐结果
     */
    public RecommendationResult recommendWithDebugInfo(List<String> tasks, List<String> files, boolean hasFailureHistory) {
        return recommendWithDebugInfo(tasks, files, hasFailureHistory, null);
    }

    /**
     * 为任务生成推荐（带调试信息的完整API）
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @param hasFailureHistory 是否有失败历史
     * @param explicitEffort 显式指定的 effort 等级
     * @return 包含调试信息的推荐结果
     */
    public RecommendationResult recommendWithDebugInfo(
        List<String> tasks,
        List<String> files,
        boolean hasFailureHistory,
        String explicitEffort
    ) {
        // 1. 分析任务特征
        TaskCharacteristics characteristics = analyzer.analyzeTask(tasks, files, hasFailureHistory, explicitEffort);

        // 2. 计算模式评分
        ModeScores scores = scorer.scoreModes(characteristics);

        // 3. 生成推荐结果
        ModeRecommendation recommendation = generator.generate(characteristics, scores);

        // 4. 返回包含调试信息的完整结果
        return new RecommendationResult(recommendation, characteristics, scores);
    }

    /**
     * 获取当前的评分权重配置
     * @return 评分权重配置
     */
    public ScoringWeights getWeights() {
        return scorer.getWeights();
    }

    /**
     * 更新评分权重配置
     * @param weights 新的评分权重配置
     */
    public void updateWeights(ScoringWeights weights) {
        // 注意：由于 ModeScorer 是不可变的，这个方法可能需要重新创建 scorer
        // 或者我们可以让 ModeScorer 支持权重更新
        // 当前实现中，我们选择创建新的 scorer 实例
        throw new UnsupportedOperationException("当前实现不支持权重更新。请创建新的 ModeRecommender 实例。");
    }

    /**
     * 批量为多个任务生成推荐
     * @param taskGroups 任务组列表，每个元素包含 [tasks, files]
     * @return 推荐结果列表
     */
    public List<ModeRecommendation> batchRecommend(List<TaskGroup> taskGroups) {
        List<ModeRecommendation> recommendations = new ArrayList<>();

        for (TaskGroup group : taskGroups) {
            ModeRecommendation recommendation = recommend(group.tasks(), group.files());
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    /**
     * 任务组记录（内部使用）
     */
    public record TaskGroup(List<String> tasks, List<String> files) {
        public TaskGroup {
            if (tasks == null) {
                tasks = List.of();
            }
            if (files == null) {
                files = List.of();
            }
        }
    }

    /**
     * 快速推荐：只返回推荐模式，不生成详细理由
     * @param tasks 任务描述列表
     * @param files 变更文件列表
     * @return 推荐的执行模式
     */
    public ExecutionMode quickRecommend(List<String> tasks, List<String> files) {
        ModeRecommendation recommendation = recommend(tasks, files);
        return recommendation.recommendedMode();
    }

    /**
     * 判断是否应该自动应用推荐
     * 基于置信度决定是否可以自动应用推荐
     * @param recommendation 推荐结果
     * @return 如果置信度 >= 0.8 返回 true
     */
    public boolean shouldAutoApply(ModeRecommendation recommendation) {
        return recommendation.confidence() >= 0.8;
    }

    /**
     * 判断是否需要用户确认
     * @param recommendation 推荐结果
     * @return 如果置信度 < 0.7 返回 true
     */
    public boolean requiresUserConfirmation(ModeRecommendation recommendation) {
        return recommendation.confidence() < 0.7;
    }

    /**
     * 获取推荐的文本描述
     * @param recommendation 推荐结果
     * @return 格式化的推荐描述
     */
    public String getRecommendationSummary(ModeRecommendation recommendation) {
        StringBuilder summary = new StringBuilder();

        summary.append("推荐模式: ").append(recommendation.recommendedMode()).append("\n");
        summary.append("置信度: ").append(String.format("%.1f%%", recommendation.confidence() * 100)).append("\n");
        summary.append("推荐理由: ").append(recommendation.reason()).append("\n");

        if (!recommendation.alternativeModes().isEmpty()) {
            summary.append("备选方案: ").append(recommendation.alternativeModes()).append("\n");
        }

        return summary.toString();
    }
}