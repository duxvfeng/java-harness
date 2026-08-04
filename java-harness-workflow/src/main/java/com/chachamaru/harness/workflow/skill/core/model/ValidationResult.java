package com.chachamaru.harness.workflow.skill.core.model;

import java.util.List;

/**
 * 验证结果
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> issues;
    private final List<String> warnings;

    private ValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.issues = builder.issues != null ? List.copyOf(builder.issues) : List.of();
        this.warnings = builder.warnings != null ? List.copyOf(builder.warnings) : List.of();
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getIssues() {
        return issues;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getIssueCount() {
        return issues.size();
    }

    public int getWarningCount() {
        return warnings.size();
    }

    public boolean hasIssues() {
        return !issues.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid = true;
        private List<String> issues;
        private List<String> warnings;

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder issues(List<String> issues) {
            this.issues = issues;
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }
}