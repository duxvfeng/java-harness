package com.chachamaru.harness.workflow.agent.framework;

import java.time.Instant;
import java.util.*;

/**
 * Agent 执行结果（阶段1：最小实现）
 */
public class AgentResult {
    private final String agentId;
    private final String executionId;
    private final AgentStatus status;
    private final Instant startTime;
    private final Instant completedTime;
    private final Object output;
    private final String errorMessage;
    private final List<SkillCallTrace> skillCalls;

    private AgentResult(Builder builder) {
        this.agentId = builder.agentId;
        this.executionId = builder.executionId;
        this.status = builder.status;
        this.startTime = builder.startTime;
        this.completedTime = builder.completedTime;
        this.output = builder.output;
        this.errorMessage = builder.errorMessage;
        this.skillCalls = Collections.unmodifiableList(builder.skillCalls);
    }

    public String getAgentId() { return agentId; }
    public String getExecutionId() { return executionId; }
    public AgentStatus getStatus() { return status; }
    public Instant getStartTime() { return startTime; }
    public Instant getCompletedTime() { return completedTime; }
    public Object getOutput() { return output; }
    public String getErrorMessage() { return errorMessage; }
    public List<SkillCallTrace> getSkillCalls() { return skillCalls; }

    public long getExecutionDurationMs() {
        return completedTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public boolean isSuccess() {
        return status.isSuccess();
    }

    public boolean isPartialSuccess() {
        return status.isPartialSuccess();
    }

    public boolean hasWarnings() {
        return status == AgentStatus.SUCCESS_WITH_WARNINGS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String agentId;
        private String executionId = UUID.randomUUID().toString();
        private AgentStatus status = AgentStatus.PENDING;
        private Instant startTime = Instant.now();
        private Instant completedTime;
        private Object output;
        private String errorMessage;
        private List<SkillCallTrace> skillCalls = new ArrayList<>();

        public Builder agentId(String agentId) {
            this.agentId = agentId;
            return this;
        }

        public Builder executionId(String executionId) {
            this.executionId = executionId;
            return this;
        }

        public Builder status(AgentStatus status) {
            this.status = status;
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

        public Builder addSkillCall(SkillCallTrace skillCall) {
            this.skillCalls.add(skillCall);
            return this;
        }

        public Builder skillCalls(List<SkillCallTrace> skillCalls) {
            this.skillCalls = new ArrayList<>(skillCalls);
            return this;
        }

        public Builder completedTime(Instant completedTime) {
            this.completedTime = completedTime;
            return this;
        }

        public Builder startTime(Instant startTime) {
            this.startTime = startTime;
            return this;
        }

        public AgentResult build() {
            if (completedTime == null) {
                completedTime = Instant.now();
            }
            return new AgentResult(this);
        }

        public Builder success(Object output) {
            return status(AgentStatus.SUCCESS)
                    .output(output)
                    .completedTime(Instant.now());
        }

        public Builder failed(String errorMessage) {
            return status(AgentStatus.FAILED)
                    .errorMessage(errorMessage)
                    .completedTime(Instant.now());
        }
    }
}
