package com.chachamaru.harness.session.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Token 使用信息
 *
 * <p>记录当前会话的 token 使用情况，用于自动保存触发判断。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class TokenUsageInfo {

    @JsonProperty("currentUsage")
    private final int currentUsage;

    @JsonProperty("percentage")
    private final int percentage;

    @JsonProperty("estimatedRemaining")
    private final int estimatedRemaining;

    @JsonProperty("detectionMethod")
    private final String detectionMethod;

    @JsonProperty("lastUpdateTime")
    private final long lastUpdateTime;

    /**
     * 构造 TokenUsageInfo
     *
     * @param currentUsage 当前使用的 token 数量
     * @param percentage 使用百分比 (0-100)
     * @param estimatedRemaining 估计剩余 token 数量
     * @param detectionMethod 检测方法 (environment_variable, api, estimation, unknown)
     */
    public TokenUsageInfo(
            int currentUsage,
            int percentage,
            int estimatedRemaining,
            String detectionMethod) {
        this.currentUsage = currentUsage;
        this.percentage = percentage;
        this.estimatedRemaining = estimatedRemaining;
        this.detectionMethod = detectionMethod != null ? detectionMethod : "unknown";
        this.lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 创建检测失败的 TokenUsageInfo
     */
    public static TokenUsageInfo detectionFailed() {
        return new TokenUsageInfo(-1, -1, -1, "failed");
    }

    /**
     * 创建未知的 TokenUsageInfo
     */
    public static TokenUsageInfo unknown() {
        return new TokenUsageInfo(-1, -1, -1, "unknown");
    }

    // Getters
    public int getCurrentUsage() { return currentUsage; }
    public int getPercentage() { return percentage; }
    public int getEstimatedRemaining() { return estimatedRemaining; }
    public String getDetectionMethod() { return detectionMethod; }
    public long getLastUpdateTime() { return lastUpdateTime; }

    /**
     * 判断是否达到阈值
     */
    public boolean isThresholdReached(int threshold) {
        if (percentage < 0) {
            return false; // 无法确定时不触发
        }
        return percentage >= threshold;
    }

    /**
     * 判断是否需要立即保存（紧急阈值）
     */
    public boolean needsImmediateSave(int urgentThreshold, int normalThreshold) {
        if (percentage < 0) {
            return false;
        }
        return percentage >= urgentThreshold;
    }

    /**
     * 判断检测是否成功
     */
    public boolean isDetectionSuccessful() {
        return currentUsage >= 0 && percentage >= 0;
    }

    /**
     * 获取使用状态描述
     */
    public String getStatusDescription() {
        if (!isDetectionSuccessful()) {
            return "Unknown (detection failed)";
        }

        if (percentage >= 90) {
            return "Critical (" + percentage + "%)";
        } else if (percentage >= 80) {
            return "High (" + percentage + "%)";
        } else if (percentage >= 50) {
            return "Moderate (" + percentage + "%)";
        } else {
            return "Normal (" + percentage + "%)";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenUsageInfo that = (TokenUsageInfo) o;
        return currentUsage == that.currentUsage &&
               percentage == that.percentage &&
               estimatedRemaining == that.estimatedRemaining &&
               Objects.equals(detectionMethod, that.detectionMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentUsage, percentage, estimatedRemaining, detectionMethod);
    }

    @Override
    public String toString() {
        return "TokenUsageInfo{" +
                "currentUsage=" + currentUsage +
                ", percentage=" + percentage +
                ", estimatedRemaining=" + estimatedRemaining +
                ", detectionMethod='" + detectionMethod + '\'' +
                ", status='" + getStatusDescription() + '\'' +
                '}';
    }
}