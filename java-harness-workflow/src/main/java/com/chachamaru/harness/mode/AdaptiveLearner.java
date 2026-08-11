package com.chachamaru.harness.mode;

/**
 * 自适应学习器
 * 基于用户反馈渐进式优化推荐算法
 */
public class AdaptiveLearner {

    private ScoringWeights currentWeights;
    private final WeightOptimizer optimizer;
    private int feedbackCount;

    /**
     * 创建自适应学习器
     */
    public AdaptiveLearner() {
        this(ScoringWeights.DEFAULT);
    }

    /**
     * 创建自适应学习器
     * @param initialWeights 初始权重
     */
    public AdaptiveLearner(ScoringWeights initialWeights) {
        this.currentWeights = initialWeights != null ? initialWeights : ScoringWeights.DEFAULT;
        this.optimizer = new WeightOptimizer();
        this.feedbackCount = 0;
    }

    /**
     * 设置初始权重
     * @param weights 初始权重
     */
    public void setInitialWeights(ScoringWeights weights) {
        this.currentWeights = weights != null ? weights : ScoringWeights.DEFAULT;
        this.feedbackCount = 0;
    }

    /**
     * 从用户反馈中学习
     * @param history 用户反馈历史
     * @return 更新后的权重
     */
    public ScoringWeights learnFromFeedback(UserFeedbackHistory history) {
        if (history == null || history.isEmpty()) {
            return currentWeights;
        }

        feedbackCount += history.size();

        // 计算学习率（随着反馈增加而降低）
        double learningRate = calculateLearningRate();

        // 渐进式优化权重
        currentWeights = optimizer.progressiveOptimize(currentWeights, history, learningRate);

        return currentWeights;
    }

    /**
     * 获取当前权重
     * @return 当前权重配置
     */
    public ScoringWeights getCurrentWeights() {
        return currentWeights;
    }

    /**
     * 获取已处理的反馈数量
     * @return 反馈数量
     */
    public int getFeedbackCount() {
        return feedbackCount;
    }

    /**
     * 重置学习器
     */
    public void reset() {
        this.currentWeights = ScoringWeights.DEFAULT;
        this.feedbackCount = 0;
    }

    /**
     * 计算学习率
     * @return 学习率（0-1）
     */
    private double calculateLearningRate() {
        // 随着反馈数量增加，学习率逐渐降低
        // 初始学习率较高，逐步降低到最小值
        if (feedbackCount < 10) {
            return 0.3; // 早期快速学习
        } else if (feedbackCount < 50) {
            return 0.1; // 中期稳步调整
        } else {
            return 0.05; // 后期微调
        }
    }

    /**
     * 判断是否已经充分学习
     * @return 是否充分学习
     */
    public boolean isWellLearned() {
        return feedbackCount >= 50;
    }
}