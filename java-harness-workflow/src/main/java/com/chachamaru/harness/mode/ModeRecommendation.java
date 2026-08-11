package com.chachamaru.harness.mode;

import java.util.List;
import java.util.Objects;

/**
 * 执行模式推荐结果数据类
 * 包含推荐的执行模式、置信度、推荐理由和备选方案
 *
 * @param recommendedMode 推荐的执行模式
 * @param confidence 推荐置信度 (0.0-1.0)
 * @param reason 推荐理由
 * @param alternativeModes 备选执行模式列表
 */
public record ModeRecommendation(
    ExecutionMode recommendedMode,
    double confidence,
    String reason,
    List<ExecutionMode> alternativeModes
) {
    /**
     * 验证推荐结果的有效性
     * @return true 如果推荐结果有效，否则 false
     */
    public boolean isValid() {
        return recommendedMode != null &&
               confidence >= 0.0 && confidence <= 1.0 &&
               reason != null && !reason.isEmpty() &&
               alternativeModes != null;
    }

    /**
     * 判断是否为高置信度推荐 (>80%)
     * @return true 如果置信度 > 0.8
     */
    public boolean isHighConfidence() {
        return confidence > 0.8;
    }

    /**
     * 判断是否为中等置信度推荐 (60%-80%)
     * @return true 如果置信度在 0.6-0.8 之间
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.6 && confidence <= 0.8;
    }

    /**
     * 判断是否为低置信度推荐 (<60%)
     * @return true 如果置信度 < 0.6
     */
    public boolean isLowConfidence() {
        return confidence < 0.6;
    }

    /**
     * 判断推荐是否应该自动应用
     * 高置信度推荐可以自动应用，无需用户确认
     * @return true 如果应该自动应用
     */
    public boolean shouldAutoApply() {
        return isHighConfidence();
    }

    /**
     * 判断是否需要用户确认
     * 中等置信度推荐需要用户确认
     * @return true 如果需要用户确认
     */
    public boolean requiresUserConfirmation() {
        return isMediumConfidence();
    }

    /**
     * 判断是否需要提供多个选项
     * 低置信度推荐应该提供多个选项供用户选择
     * @return true 如果需要提供多个选项
     */
    public boolean requiresMultipleOptions() {
        return isLowConfidence();
    }

    /**
     * 获取置信度百分比表示
     * @return 置信度百分比 (0-100)
     */
    public int getConfidencePercentage() {
        return (int) Math.round(confidence * 100);
    }

    /**
     * 获取推荐的文本描述
     * @return 推荐模式的文本描述
     */
    public String getRecommendedModeDescription() {
        return switch (recommendedMode) {
            case SOLO -> "单独执行 (开销最小，执行最快)";
            case PARALLEL -> "并行执行 (2-3个任务并行处理)";
            case BREEZING -> "团队协作 (Lead/Worker/Reviewer 协调执行)";
        };
    }

    /**
     * 获取备选方案的文本描述
     * @return 备选方案的文本描述
     */
    public String getAlternativesDescription() {
        if (alternativeModes == null || alternativeModes.isEmpty()) {
            return "无备选方案";
        }

        StringBuilder sb = new StringBuilder("备选方案: ");
        for (int i = 0; i < alternativeModes.size(); i++) {
            ExecutionMode mode = alternativeModes.get(i);
            sb.append(getModeDescription(mode));
            if (i < alternativeModes.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    private String getModeDescription(ExecutionMode mode) {
        return switch (mode) {
            case SOLO -> "Solo";
            case PARALLEL -> "Parallel";
            case BREEZING -> "Breezing";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModeRecommendation that = (ModeRecommendation) o;
        return Double.compare(that.confidence, confidence) == 0 &&
               Objects.equals(recommendedMode, that.recommendedMode) &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(alternativeModes, that.alternativeModes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recommendedMode, confidence, reason, alternativeModes);
    }

    @Override
    public String toString() {
        return String.format("ModeRecommendation[recommendedMode=%s, confidence=%.2f, reason='%s', alternatives=%s]",
            recommendedMode, confidence, reason, alternativeModes);
    }
}