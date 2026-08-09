package com.chachamaru.harness.session.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * 会话摘要信息
 *
 * <p>用于会话恢复提示时显示的精简摘要，包含工作进度、AI决策和建议。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SessionSummary {

    @JsonProperty("saveId")
    private final String saveId;

    @JsonProperty("quickOverview")
    private final String quickOverview;

    @JsonProperty("currentWork")
    private final String currentWork;

    @JsonProperty("recentProgress")
    private final List<String> recentProgress;

    @JsonProperty("recommendation")
    private final String recommendation;

    @JsonProperty("aiDecision")
    private final AIDecision aiDecision;

    /**
     * 构造 SessionSummary
     *
     * @param saveId 保存ID
     * @param quickOverview 快速概览（1-2句话）
     * @param currentWork 当前工作内容
     * @param recentProgress 最近进度列表
     * @param recommendation AI 建议
     * @param aiDecision AI 决策信息
     */
    @JsonCreator
    public SessionSummary(
            @JsonProperty("saveId") String saveId,
            @JsonProperty("quickOverview") String quickOverview,
            @JsonProperty("currentWork") String currentWork,
            @JsonProperty("recentProgress") List<String> recentProgress,
            @JsonProperty("recommendation") String recommendation,
            @JsonProperty("aiDecision") AIDecision aiDecision) {
        this.saveId = saveId;
        this.quickOverview = quickOverview;
        this.currentWork = currentWork;
        this.recentProgress = recentProgress;
        this.recommendation = recommendation;
        this.aiDecision = aiDecision;
    }

    // Getters
    public String getSaveId() { return saveId; }
    public String getQuickOverview() { return quickOverview; }
    public String getCurrentWork() { return currentWork; }
    public List<String> getRecentProgress() { return recentProgress; }
    public String getRecommendation() { return recommendation; }
    public AIDecision getAiDecision() { return aiDecision; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionSummary that = (SessionSummary) o;
        return Objects.equals(saveId, that.saveId) &&
               Objects.equals(quickOverview, that.quickOverview);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveId, quickOverview);
    }

    @Override
    public String toString() {
        return "SessionSummary{" +
                "saveId='" + saveId + '\'' +
                ", quickOverview='" + quickOverview + '\'' +
                ", currentWork='" + currentWork + '\'' +
                ", aiDecision=" + aiDecision +
                '}';
    }

    /**
     * AI 决策信息
     */
    public static class AIDecision {
        @JsonProperty("needsDetailedContext")
        private final boolean needsDetailedContext;

        @JsonProperty("reason")
        private final String reason;

        @JsonProperty("confidence")
        private final double confidence;

        @JsonCreator
        public AIDecision(
                @JsonProperty("needsDetailedContext") boolean needsDetailedContext,
                @JsonProperty("reason") String reason,
                @JsonProperty("confidence") double confidence) {
            this.needsDetailedContext = needsDetailedContext;
            this.reason = reason;
            this.confidence = confidence;
        }

        public boolean needsDetailedContext() { return needsDetailedContext; }
        public String getReason() { return reason; }
        public double getConfidence() { return confidence; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AIDecision aiDecision = (AIDecision) o;
            return needsDetailedContext == aiDecision.needsDetailedContext &&
                   Double.compare(aiDecision.confidence, confidence) == 0 &&
                   Objects.equals(reason, aiDecision.reason);
        }

        @Override
        public int hashCode() {
            return Objects.hash(needsDetailedContext, reason, confidence);
        }

        @Override
        public String toString() {
            return "AIDecision{" +
                    "needsDetailedContext=" + needsDetailedContext +
                    ", reason='" + reason + '\'' +
                    ", confidence=" + confidence +
                    '}';
        }
    }
}