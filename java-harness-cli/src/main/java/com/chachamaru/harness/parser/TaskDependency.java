package com.chachamaru.harness.parser;

import java.util.Objects;

/**
 * Task dependency model.
 * Represents a dependency relationship between tasks.
 */
public class TaskDependency {
    private String taskId;
    private String dependsOn;

    public TaskDependency() {
    }

    public TaskDependency(String taskId, String dependsOn) {
        this.taskId = taskId;
        this.dependsOn = dependsOn;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getDependsOn() {
        return dependsOn;
    }

    public void setDependsOn(String dependsOn) {
        this.dependsOn = dependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskDependency that = (TaskDependency) o;
        return Objects.equals(taskId, that.taskId) &&
                Objects.equals(dependsOn, that.dependsOn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, dependsOn);
    }

    @Override
    public String toString() {
        return "TaskDependency{" +
                "taskId='" + taskId + '\'' +
                ", dependsOn='" + dependsOn + '\'' +
                '}';
    }
}
