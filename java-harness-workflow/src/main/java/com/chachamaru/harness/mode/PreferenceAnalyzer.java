package com.chachamaru.harness.mode;

import java.util.*;

/**
 * 用户偏好分析器
 * 分析用户模式选择偏好
 */
public class PreferenceAnalyzer {

    /**
     * 分析用户偏好
     * @param history 用户反馈历史
     * @return 用户偏好分析结果
     */
    public UserPreferences analyzePreferences(UserFeedbackHistory history) {
        if (history == null || history.isEmpty()) {
            return new UserPreferences(Map.of(), 0);
        }

        Map<ExecutionMode, Integer> modeCounts = new HashMap<>();
        modeCounts.put(ExecutionMode.SOLO, history.getModeSelectionCount(ExecutionMode.SOLO));
        modeCounts.put(ExecutionMode.PARALLEL, history.getModeSelectionCount(ExecutionMode.PARALLEL));
        modeCounts.put(ExecutionMode.BREEZING, history.getModeSelectionCount(ExecutionMode.BREEZING));

        int totalSelections = history.size();

        return new UserPreferences(modeCounts, totalSelections);
    }

    /**
     * 分析用户接受率
     * @param history 用户反馈历史
     * @return 接受率（0-1）
     */
    public double calculateAcceptanceRate(UserFeedbackHistory history) {
        if (history == null || history.isEmpty()) {
            return 0.0;
        }

        long acceptedCount = history.getAcceptedFeedbacks().size();
        return (double) acceptedCount / history.size();
    }

    /**
     * 分析模式转换模式
     * @param history 用户反馈历史
     * @return 模式转换统计
     */
    public Map<String, Integer> analyzeModeTransitions(UserFeedbackHistory history) {
        if (history == null || history.isEmpty()) {
            return Map.of();
        }

        Map<String, Integer> transitions = new HashMap<>();

        for (UserFeedback feedback : history.feedbacks()) {
            if (feedback.wasRejected()) {
                ExecutionMode from = feedback.recommendedMode();
                ExecutionMode to = feedback.selectedMode();
                String transition = from + "->" + to;
                transitions.put(transition, transitions.getOrDefault(transition, 0) + 1);
            }
        }

        return transitions;
    }

    /**
     * 识别用户的模式偏好模式
     * @param history 用户反馈历史
     * @return 偏好描述
     */
    public String identifyPreferencePattern(UserFeedbackHistory history) {
        UserPreferences preferences = analyzePreferences(history);

        if (preferences.totalSelections() < 5) {
            return "数据不足，无法确定偏好模式";
        }

        Optional<ExecutionMode> mostPreferred = preferences.getMostPreferredMode();
        if (mostPreferred.isEmpty()) {
            return "无明显偏好模式";
        }

        double acceptanceRate = calculateAcceptanceRate(history);
        ExecutionMode preferredMode = mostPreferred.get();

        int count = preferences.getModeSelectionCount(preferredMode);
        double percentage = (double) count / preferences.totalSelections() * 100;

        StringBuilder pattern = new StringBuilder();
        pattern.append("用户偏好 ").append(preferredMode).append(" 模式");
        pattern.append("（使用率: ").append(String.format("%.1f%%", percentage)).append("）");

        if (acceptanceRate > 0.8) {
            pattern.append("，推荐接受率较高（").append(String.format("%.1f%%", acceptanceRate * 100)).append("）");
        } else if (acceptanceRate < 0.5) {
            pattern.append("，推荐接受率较低（").append(String.format("%.1f%%", acceptanceRate * 100)).append("），可能需要调整推荐策略");
        }

        return pattern.toString();
    }
}