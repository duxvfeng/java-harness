package com.chachamaru.harness.workflow.skill.core.model;

import java.util.List;

/**
 * 规划意图
 * 表示用户的规划需求和目标
 */
public class PlanningIntent {
    private final String userIntent;
    private final List<String> targetGoals;
    private final Constraints constraints;
    private final AcceptanceCriteria acceptanceCriteria;

    private PlanningIntent(Builder builder) {
        this.userIntent = builder.userIntent;
        this.targetGoals = builder.targetGoals != null ? List.copyOf(builder.targetGoals) : List.of();
        this.constraints = builder.constraints;
        this.acceptanceCriteria = builder.acceptanceCriteria;
    }

    public String getUserIntent() {
        return userIntent;
    }

    public List<String> getTargetGoals() {
        return targetGoals;
    }

    public Constraints getConstraints() {
        return constraints;
    }

    public AcceptanceCriteria getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String userIntent;
        private List<String> targetGoals;
        private Constraints constraints;
        private AcceptanceCriteria acceptanceCriteria;

        public Builder userIntent(String userIntent) {
            this.userIntent = userIntent;
            return this;
        }

        public Builder targetGoals(List<String> targetGoals) {
            this.targetGoals = targetGoals;
            return this;
        }

        public Builder constraints(Constraints constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder acceptanceCriteria(AcceptanceCriteria acceptanceCriteria) {
            this.acceptanceCriteria = acceptanceCriteria;
            return this;
        }

        public PlanningIntent build() {
            if (userIntent == null || userIntent.isEmpty()) {
                throw new IllegalStateException("userIntent is required");
            }
            return new PlanningIntent(this);
        }
    }
}