package com.chachamaru.harness.workflow.skill.core.model;

import java.util.List;

/**
 * 约束条件
 */
public class Constraints {
    private final List<String> timeConstraints;
    private final List<String> resourceConstraints;
    private final List<String> technicalConstraints;

    private Constraints(Builder builder) {
        this.timeConstraints = builder.timeConstraints != null ? List.copyOf(builder.timeConstraints) : List.of();
        this.resourceConstraints = builder.resourceConstraints != null ? List.copyOf(builder.resourceConstraints) : List.of();
        this.technicalConstraints = builder.technicalConstraints != null ? List.copyOf(builder.technicalConstraints) : List.of();
    }

    public List<String> getTimeConstraints() {
        return timeConstraints;
    }

    public List<String> getResourceConstraints() {
        return resourceConstraints;
    }

    public List<String> getTechnicalConstraints() {
        return technicalConstraints;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> timeConstraints;
        private List<String> resourceConstraints;
        private List<String> technicalConstraints;

        public Builder timeConstraints(List<String> timeConstraints) {
            this.timeConstraints = timeConstraints;
            return this;
        }

        public Builder resourceConstraints(List<String> resourceConstraints) {
            this.resourceConstraints = resourceConstraints;
            return this;
        }

        public Builder technicalConstraints(List<String> technicalConstraints) {
            this.technicalConstraints = technicalConstraints;
            return this;
        }

        public Constraints build() {
            return new Constraints(this);
        }
    }
}