package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import java.time.Instant;
import java.util.UUID;

/**
 * Skill 调用追踪
 */
public class SkillCallTrace {
    private final String callId;
    private final String skillId;
    private final SkillResult result;
    private final Instant callTime;
    private final String callerDecision;
    private final int callOrder;

    private SkillCallTrace(Builder builder) {
        this.callId = builder.callId;
        this.skillId = builder.skillId;
        this.result = builder.result;
        this.callTime = builder.callTime;
        this.callerDecision = builder.callerDecision;
        this.callOrder = builder.callOrder;
    }

    public String getCallId() { return callId; }
    public String getSkillId() { return skillId; }
    public SkillResult getResult() { return result; }
    public Instant getCallTime() { return callTime; }
    public String getCallerDecision() { return callerDecision; }
    public int getCallOrder() { return callOrder; }

    public boolean isSuccessful() {
        return result != null && result.isSuccess();
    }

    public long getDuration() {
        if (result == null) return 0;
        return result.getExecutionDurationMs();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String callId = UUID.randomUUID().toString();
        private String skillId;
        private SkillResult result;
        private Instant callTime = Instant.now();
        private String callerDecision;
        private int callOrder;

        public Builder skillId(String skillId) {
            this.skillId = skillId;
            return this;
        }

        public Builder result(SkillResult result) {
            this.result = result;
            return this;
        }

        public Builder callerDecision(String callerDecision) {
            this.callerDecision = callerDecision;
            return this;
        }

        public Builder callOrder(int callOrder) {
            this.callOrder = callOrder;
            return this;
        }

        public SkillCallTrace build() {
            if (skillId == null) {
                throw new IllegalArgumentException("skillId is required");
            }
            return new SkillCallTrace(this);
        }
    }
}
