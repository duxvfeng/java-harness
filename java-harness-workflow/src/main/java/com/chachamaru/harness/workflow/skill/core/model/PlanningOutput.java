package com.chachamaru.harness.workflow.skill.core.model;

/**
 * 规划输出
 * PlanSkill 执行的完整输出结果
 */
public class PlanningOutput {
    private final SpecDelta specDelta;
    private final PlansMd plansMd;
    private final PreApprovalSection preApproval;
    private final ValidationResult validation;

    private PlanningOutput(Builder builder) {
        this.specDelta = builder.specDelta;
        this.plansMd = builder.plansMd;
        this.preApproval = builder.preApproval;
        this.validation = builder.validation;
    }

    public SpecDelta getSpecDelta() {
        return specDelta;
    }

    public PlansMd getPlansMd() {
        return plansMd;
    }

    public PreApprovalSection getPreApproval() {
        return preApproval;
    }

    public ValidationResult getValidation() {
        return validation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SpecDelta specDelta;
        private PlansMd plansMd;
        private PreApprovalSection preApproval;
        private ValidationResult validation;

        public Builder specDelta(SpecDelta specDelta) {
            this.specDelta = specDelta;
            return this;
        }

        public Builder plansMd(PlansMd plansMd) {
            this.plansMd = plansMd;
            return this;
        }

        public Builder preApproval(PreApprovalSection preApproval) {
            this.preApproval = preApproval;
            return this;
        }

        public Builder validation(ValidationResult validation) {
            this.validation = validation;
            return this;
        }

        public PlanningOutput build() {
            return new PlanningOutput(this);
        }
    }
}