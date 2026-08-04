package com.chachamaru.harness.workflow.skill.core.model;

/**
 * 任务条目
 * 表示 Plans.md 中的单个任务
 */
public class TaskEntry {
    private final String taskId;
    private final String taskName;
    private final String content;
    private final String definitionOfDone;
    private final String dependencies;
    private final TaskStatus status;

    private TaskEntry(Builder builder) {
        this.taskId = builder.taskId;
        this.taskName = builder.taskName;
        this.content = builder.content;
        this.definitionOfDone = builder.definitionOfDone;
        this.dependencies = builder.dependencies;
        this.status = builder.status != null ? builder.status : TaskStatus.TODO;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getContent() {
        return content;
    }

    public String getDefinitionOfDone() {
        return definitionOfDone;
    }

    public String getDependencies() {
        return dependencies;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String taskId;
        private String taskName;
        private String content;
        private String definitionOfDone;
        private String dependencies;
        private TaskStatus status;

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder taskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder definitionOfDone(String definitionOfDone) {
            this.definitionOfDone = definitionOfDone;
            return this;
        }

        public Builder dependencies(String dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder status(TaskStatus status) {
            this.status = status;
            return this;
        }

        public TaskEntry build() {
            if (taskId == null || taskName == null) {
                throw new IllegalStateException("taskId and taskName are required");
            }
            return new TaskEntry(this);
        }
    }

    public enum TaskStatus {
        TODO,
        IN_PROGRESS,
        DONE,
        BLOCKED
    }
}