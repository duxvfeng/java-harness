package com.chachamaru.harness.workflow.skill.core.model;

import java.util.List;

/**
 * 验收标准
 */
public class AcceptanceCriteria {
    private final List<String> functionalRequirements;
    private final List<String> nonFunctionalRequirements;

    private AcceptanceCriteria(Builder builder) {
        this.functionalRequirements = builder.functionalRequirements != null ? List.copyOf(builder.functionalRequirements) : List.of();
        this.nonFunctionalRequirements = builder.nonFunctionalRequirements != null ? List.copyOf(builder.nonFunctionalRequirements) : List.of();
    }

    public List<String> getFunctionalRequirements() {
        return functionalRequirements;
    }

    public List<String> getNonFunctionalRequirements() {
        return nonFunctionalRequirements;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> functionalRequirements;
        private List<String> nonFunctionalRequirements;

        public Builder functionalRequirements(List<String> functionalRequirements) {
            this.functionalRequirements = functionalRequirements;
            return this;
        }

        public Builder nonFunctionalRequirements(List<String> nonFunctionalRequirements) {
            this.nonFunctionalRequirements = nonFunctionalRequirements;
            return this;
        }

        public AcceptanceCriteria build() {
            return new AcceptanceCriteria(this);
        }
    }
}