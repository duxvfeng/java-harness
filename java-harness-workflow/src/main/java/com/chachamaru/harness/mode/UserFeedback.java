package com.chachamaru.harness.mode;

import java.io.Serializable;
import java.util.List;
import java.time.Instant;

/**
 * 用户反馈记录
 * 记录用户对推荐结果的接受/拒绝行为
 */
public record UserFeedback(
    List<String> tasks,                    // 任务列表
    List<String> files,                    // 文件列表
    ModeRecommendation originalRecommendation, // 原始推荐
    ExecutionMode selectedMode,            // 用户选择的模式
    boolean wasAccepted,                   // 是否接受推荐
    Instant timestamp                      // 反馈时间戳
) implements Serializable {

    /**
     * 序列化版本UID
     */
    private static final long serialVersionUID = 1L;
    /**
     * 创建用户反馈
     */
    public UserFeedback {
        // 确保不可变
        tasks = List.copyOf(tasks != null ? tasks : List.of());
        files = List.copyOf(files != null ? files : List.of());

        if (originalRecommendation == null) {
            throw new IllegalArgumentException("原始推荐不能为null");
        }
        if (selectedMode == null) {
            throw new IllegalArgumentException("选择的模式不能为null");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }

        // 如果接受了推荐，selectedMode应该与推荐模式一致
        if (wasAccepted && !selectedMode.equals(originalRecommendation.recommendedMode())) {
            throw new IllegalArgumentException("接受推荐时，选择模式应该与推荐模式一致");
        }
    }

    /**
     * 创建接受反馈
     */
    public static UserFeedback accepted(
        List<String> tasks,
        List<String> files,
        ModeRecommendation recommendation
    ) {
        return new UserFeedback(
            tasks,
            files,
            recommendation,
            recommendation.recommendedMode(),
            true,
            Instant.now()
        );
    }

    /**
     * 创建拒绝反馈
     */
    public static UserFeedback rejected(
        List<String> tasks,
        List<String> files,
        ModeRecommendation recommendation,
        ExecutionMode selectedMode
    ) {
        return new UserFeedback(
            tasks,
            files,
            recommendation,
            selectedMode,
            false,
            Instant.now()
        );
    }

    /**
     * 判断是否为拒绝反馈
     */
    public boolean wasRejected() {
        return !wasAccepted;
    }

    /**
     * 获取推荐置信度
     */
    public double originalConfidence() {
        return originalRecommendation.confidence();
    }

    /**
     * 获取推荐的执行模式
     */
    public ExecutionMode recommendedMode() {
        return originalRecommendation.recommendedMode();
    }
}