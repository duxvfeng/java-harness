package com.chachamaru.harness.workflow.skill.framework;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 技能执行结果
 * 包含技能执行的完整结果信息
 */
public class SkillResult {
    private final String skillId;
    private final String executionId;
    private final SkillStatus status;
    private final Instant startTime;
    private final Instant completedTime;
    private final Object output;
    private final String errorMessage;
    private final Map<String, Object> metadata;

    private SkillResult(Builder builder) {
        this.skillId = builder.skillId;
        this.executionId = builder.executionId;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.completedTime = builder.completedTime != null ? builder.completedTime : Instant.now();
        this.output = builder.output;
        this.errorMessage = builder.errorMessage;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : Map.of();
    }

    public String getSkillId() {
        return skillId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public SkillStatus getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public Object getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public long getExecutionDurationMs() {
        return completedTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public boolean isSuccess() {
        return status == SkillStatus.SUCCESS;
    }

    public boolean isFailed() {
        return status == SkillStatus.FAILED;
    }

    public boolean isRunning() {
        return status == SkillStatus.RUNNING;
    }

    public boolean isPending() {
        return status == SkillStatus.PENDING;
    }

    public boolean isCancelled() {
        return status == SkillStatus.CANCELLED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static SkillResult success(String skillId, Object output) {
        return new Builder()
                .skillId(skillId)
                .status(SkillStatus.SUCCESS)
                .output(output)
                .build();
    }

    public static SkillResult failed(String skillId, String errorMessage) {
        return new Builder()
                .skillId(skillId)
                .status(SkillStatus.FAILED)
                .errorMessage(errorMessage)
                .build();
    }

    public static SkillResult pending(String skillId) {
        return new Builder()
                .skillId(skillId)
                .status(SkillStatus.PENDING)
                .build();
    }

    /**
     * Builder for SkillResult
     */
    public static class Builder {
        private String skillId;
        private String executionId = UUID.randomUUID().toString();
        private SkillStatus status = SkillStatus.PENDING;
        private Instant startTime = Instant.now();
        private Instant completedTime;
        private Object output;
        private String errorMessage;
        private Map<String, Object> metadata;

        public Builder skillId(String skillId) {
            this.skillId = skillId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder status(SkillStatus status) {
            this.status = status;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder completedTime(Instant completedTime) {
            this.completedTime = completedTime;
            return this;
        }

        public Builder output(Object output) {
            this.output = output;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            if (this.metadata == null) {
                this.metadata = new java.util.HashMap<>();
            }
            this.metadata.put(key, value);
            return this;
        }

        public Builder failed(String errorMessage) {
            return status(SkillStatus.FAILED)
                    .completedTime(Instant.now())
                    .errorMessage(errorMessage);
        }

        public Builder success() {
            return status(SkillStatus.SUCCESS)
                    .completedTime(Instant.now());
        }

        public SkillResult build() {
            if (skillId == null || skillId.isEmpty()) {
                throw new IllegalStateException("skillId is required");
            }
            return new SkillResult(this);
        }
    }

    /**
     * 技能状态枚举
     */
    public enum SkillStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        SUCCESS,    // 执行成功
        FAILED,     // 执行失败
        CANCELLED   // 执行取消
    }

    @Override
    public String toString() {
        return "SkillResult{" +
                "skillId='" + skillId + '\'' +
                ", executionId='" + executionId + '\'' +
                ", status=" + status +
                ", duration=" + getExecutionDurationMs() + "ms" +
                ", success=" + isSuccess() +
                '}';
    }
}