package com.chachamaru.harness.session.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.Objects;

/**
 * 会话恢复建议
 *
 * <p>用于新会话启动时生成恢复提示，包含恢复建议和决策依据。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class RestoreSuggestion {

    @JsonProperty("saveId")
    private final String saveId;

    @JsonProperty("summary")
    private final SessionSummary summary;

    @JsonProperty("needsDetailedContext")
    private final boolean needsDetailedContext;

    @JsonProperty("reason")
    private final String reason;

    @JsonProperty("confidence")
    private final double confidence;

    @JsonProperty("timeSinceSave")
    private final long timeSinceSaveHours;

    @JsonProperty("saveTimestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant saveTimestamp;

    /**
     * 构造 RestoreSuggestion
     *
     * @param saveId 保存ID
     * @param summary 会话摘要
     * @param needsDetailedContext 是否需要恢复完整上下文
     * @param reason 决策原因
     * @param confidence 置信度 (0.0-1.0)
     * @param timeSinceSaveHours 距离保存的小时数
     * @param saveTimestamp 保存时间戳
     */
    public RestoreSuggestion(
            String saveId,
            SessionSummary summary,
            boolean needsDetailedContext,
            String reason,
            double confidence,
            long timeSinceSaveHours,
            Instant saveTimestamp) {
        this.saveId = saveId;
        this.summary = summary;
        this.needsDetailedContext = needsDetailedContext;
        this.reason = reason;
        this.confidence = Math.max(0.0, Math.min(1.0, confidence)); // Clamp to [0, 1]
        this.timeSinceSaveHours = timeSinceSaveHours;
        this.saveTimestamp = saveTimestamp;
    }

    // Getters
    public String getSaveId() { return saveId; }
    public SessionSummary getSummary() { return summary; }
    public boolean needsDetailedContext() { return needsDetailedContext; }
    public String getReason() { return reason; }
    public double getConfidence() { return confidence; }
    public long getTimeSinceSaveHours() { return timeSinceSaveHours; }
    public Instant getSaveTimestamp() { return saveTimestamp; }

    /**
     * 判断是否是高置信度建议
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * 判断是否是中置信度建议
     */
    public boolean isMediumConfidence() {
        return confidence >= 0.5 && confidence < 0.8;
    }

    /**
     * 判断是否是最近保存（24小时内）
     */
    public boolean isRecentSave() {
        return timeSinceSaveHours <= 24;
    }

    /**
     * 判断是否是较久保存（超过7天）
     */
    public boolean isOldSave() {
        return timeSinceSaveHours > 168; // 7 * 24
    }

    /**
     * 获取建议等级
     */
    public SuggestionLevel getSuggestionLevel() {
        if (isHighConfidence() && isRecentSave()) {
            return SuggestionLevel.STRONGLY_RECOMMENDED;
        } else if (isMediumConfidence() && isRecentSave()) {
            return SuggestionLevel.RECOMMENDED;
        } else if (isRecentSave()) {
            return SuggestionLevel.OPTIONAL;
        } else {
            return SuggestionLevel.LOW_PRIORITY;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestoreSuggestion that = (RestoreSuggestion) o;
        return Objects.equals(saveId, that.saveId) &&
               Objects.equals(saveTimestamp, that.saveTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveId, saveTimestamp);
    }

    @Override
    public String toString() {
        return "RestoreSuggestion{" +
                "saveId='" + saveId + '\'' +
                ", needsDetailedContext=" + needsDetailedContext +
                ", reason='" + reason + '\'' +
                ", confidence=" + confidence +
                ", timeSinceSaveHours=" + timeSinceSaveHours +
                ", level=" + getSuggestionLevel() +
                '}';
    }

    /**
     * 建议等级
     */
    public enum SuggestionLevel {
        STRONGLY_RECOMMENDED("强烈推荐", 4),
        RECOMMENDED("推荐", 3),
        OPTIONAL("可选", 2),
        LOW_PRIORITY("低优先级", 1);

        private final String description;
        private final int priority;

        SuggestionLevel(String description, int priority) {
            this.description = description;
            this.priority = priority;
        }

        public String getDescription() { return description; }
        public int getPriority() { return priority; }
    }
}