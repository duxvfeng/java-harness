package com.chachamaru.harness.workflow.skill.core.model;

/**
 * 规格变更
 */
public class SpecChange {
    private final String section;
    private final SpecDelta.ChangeType type;
    private final String content;
    private final String rationale;

    private SpecChange(Builder builder) {
        this.section = builder.section;
        this.type = builder.type;
        this.content = builder.content;
        this.rationale = builder.rationale;
    }

    public String getSection() {
        return section;
    }

    public SpecDelta.ChangeType getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public String getRationale() {
        return rationale;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String section;
        private SpecDelta.ChangeType type;
        private String content;
        private String rationale;

        public Builder section(String section) {
            this.section = section;
            return this;
        }

        public Builder type(SpecDelta.ChangeType type) {
            this.type = type;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder rationale(String rationale) {
            this.rationale = rationale;
            return this;
        }

        public SpecChange build() {
            return new SpecChange(this);
        }
    }
}