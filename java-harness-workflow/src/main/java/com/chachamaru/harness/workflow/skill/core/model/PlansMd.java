package com.chachamaru.harness.workflow.skill.core.model;

import java.util.List;

/**
 * Plans.md 文档模型
 */
public class PlansMd {
    private final String specReference;
    private final List<TaskEntry> tasks;
    private final List<String> phases;

    private PlansMd(Builder builder) {
        this.specReference = builder.specReference;
        this.tasks = builder.tasks != null ? List.copyOf(builder.tasks) : List.of();
        this.phases = builder.phases != null ? List.copyOf(builder.phases) : List.of();
    }

    public String getSpecReference() {
        return specReference;
    }

    public List<TaskEntry> getTasks() {
        return tasks;
    }

    public List<String> getPhases() {
        return phases;
    }

    public int getTaskCount() {
        return tasks.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String specReference;
        private List<TaskEntry> tasks;
        private List<String> phases;

        public Builder specReference(String specReference) {
            this.specReference = specReference;
            return this;
        }

        public Builder tasks(List<TaskEntry> tasks) {
            this.tasks = tasks;
            return this;
        }

        public Builder phases(List<String> phases) {
            this.phases = phases;
            return this;
        }

        public PlansMd build() {
            return new PlansMd(this);
        }
    }
}