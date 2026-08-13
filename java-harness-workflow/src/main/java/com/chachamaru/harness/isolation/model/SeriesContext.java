package com.chachamaru.harness.isolation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Context information about the task series.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeriesContext {

    @JsonProperty("purpose")
    private String purpose;

    @JsonProperty("estimatedTasks")
    private Integer estimatedTasks;

    @JsonProperty("completionPercentage")
    private Integer completionPercentage;

    @JsonProperty("phaseName")
    private String phaseName;

    @JsonProperty("taskType")
    private String taskType;

    // Constructors
    public SeriesContext() {
    }

    // Getters and Setters
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Integer getEstimatedTasks() {
        return estimatedTasks;
    }

    public void setEstimatedTasks(Integer estimatedTasks) {
        this.estimatedTasks = estimatedTasks;
    }

    public Integer getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(Integer completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public String getPhaseName() {
        return phaseName;
    }

    public void setPhaseName(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    // Helper methods
    @JsonIgnore
    public boolean isTaskSeriesComplete() {
        return completionPercentage != null && completionPercentage >= 100;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SeriesContext instance = new SeriesContext();

        public Builder purpose(String purpose) {
            instance.setPurpose(purpose);
            return this;
        }

        public Builder estimatedTasks(Integer estimatedTasks) {
            instance.setEstimatedTasks(estimatedTasks);
            return this;
        }

        public Builder completionPercentage(Integer completionPercentage) {
            instance.setCompletionPercentage(completionPercentage);
            return this;
        }

        public Builder phaseName(String phaseName) {
            instance.setPhaseName(phaseName);
            return this;
        }

        public Builder taskType(String taskType) {
            instance.setTaskType(taskType);
            return this;
        }

        public SeriesContext build() {
            return instance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeriesContext that = (SeriesContext) o;
        return Objects.equals(purpose, that.purpose) &&
               Objects.equals(estimatedTasks, that.estimatedTasks) &&
               Objects.equals(completionPercentage, that.completionPercentage) &&
               Objects.equals(phaseName, that.phaseName) &&
               Objects.equals(taskType, that.taskType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(purpose, estimatedTasks, completionPercentage,
                           phaseName, taskType);
    }

    @Override
    public String toString() {
        return "SeriesContext{" +
               "purpose='" + purpose + '\'' +
               ", estimatedTasks=" + estimatedTasks +
               ", completionPercentage=" + completionPercentage +
               ", phaseName='" + phaseName + '\'' +
               '}';
    }
}
