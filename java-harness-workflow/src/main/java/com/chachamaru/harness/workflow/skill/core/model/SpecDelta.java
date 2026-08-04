package com.chachamaru.harness.workflow.skill.core.model;

import java.util.List;

/**
 * Spec 增量
 * 表示对 spec.md 的变更
 */
public class SpecDelta {
    private final String targetSpecPath;
    private final ChangeType changeType;
    private final List<SpecChange> changes;
    private final String rationale;

    private SpecDelta(Builder builder) {
        this.targetSpecPath = builder.targetSpecPath;
        this.changeType = builder.changeType != null ? builder.changeType : ChangeType.UPDATE;
        this.changes = builder.changes != null ? List.copyOf(builder.changes) : List.of();
        this.rationale = builder.rationale;
    }

    public String getTargetSpecPath() {
        return targetSpecPath;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public List<SpecChange> getChanges() {
        return changes;
    }

    public String getRationale() {
        return rationale;
    }

    public int getChangeCount() {
        return changes.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String targetSpecPath;
        private ChangeType changeType;
        private List<SpecChange> changes;
        private String rationale;

        public Builder targetSpecPath(String targetSpecPath) {
            this.targetSpecPath = targetSpecPath;
            return this;
        }

        public Builder changeType(ChangeType changeType) {
            this.changeType = changeType;
            return this;
        }

        public Builder changes(List<SpecChange> changes) {
            this.changes = changes;
            return this;
        }

        public Builder rationale(String rationale) {
            this.rationale = rationale;
            return this;
        }

        public SpecDelta build() {
            return new SpecDelta(this);
        }
    }

    public enum ChangeType {
        ADD,
        UPDATE,
        DELETE,
        REPLACE
    }
}