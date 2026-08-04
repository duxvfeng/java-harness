package com.chachamaru.harness.workflow.skill.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作执行结果
 */
public class WorkResult {
    private final boolean success;
    private final String message;
    private final Object output;
    private final List<String> completedTasks;
    private final Instant startTime;
    private final Instant completedTime;

    private WorkResult(Builder builder) {
        this.success = builder.success;
        this.message = builder.message;
        this.output = builder.output;
        this.completedTasks = builder.completedTasks;
        this.startTime = builder.startTime;
        this.completedTime = builder.completedTime;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getOutput() { return output; }
    public List<String> getCompletedTasks() { return completedTasks; }
    public Instant getStartTime() { return startTime; }
    public Instant getCompletedTime() { return completedTime; }

    public long getExecutionDurationMs() {
        return completedTime.toEpochMilli() - startTime.toEpochMilli();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success = false;
        private String message;
        private Object output;
        private List<String> completedTasks = new ArrayList<>();
        private Instant startTime = Instant.now();
        private Instant completedTime;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder output(Object output) {
            this.output = output;
            return this;
        }

        public Builder addCompletedTask(String task) {
            this.completedTasks.add(task);
            return this;
        }

        public Builder completedTime(Instant completedTime) {
            this.completedTime = completedTime;
            return this;
        }

        public WorkResult build() {
            if (completedTime == null) {
                completedTime = Instant.now();
            }
            return new WorkResult(this);
        }
    }
}
