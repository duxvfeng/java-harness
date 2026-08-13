package com.chachamaru.harness.isolation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reset trigger configuration and state.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResetTriggers {

    @JsonProperty("autoResetCondition")
    private String autoResetCondition;

    @JsonProperty("autoResetAfterHours")
    private Integer autoResetAfterHours;

    @JsonProperty("manualResetAvailable")
    private Boolean manualResetAvailable;

    @JsonProperty("taskSeriesComplete")
    private Boolean taskSeriesComplete;

    @JsonProperty("autoResetEnabled")
    private Boolean autoResetEnabled;

    @JsonProperty("customConditions")
    private List<String> customConditions = new ArrayList<>();

    // Constructors
    public ResetTriggers() {
        this.autoResetEnabled = true;
        this.manualResetAvailable = true;
        this.autoResetAfterHours = 4;
        this.autoResetCondition = "branch_clean_and_no_uncommitted_changes";
    }

    // Getters and Setters
    public String getAutoResetCondition() {
        return autoResetCondition;
    }

    public void setAutoResetCondition(String autoResetCondition) {
        this.autoResetCondition = autoResetCondition;
    }

    public Integer getAutoResetAfterHours() {
        return autoResetAfterHours;
    }

    public void setAutoResetAfterHours(Integer autoResetAfterHours) {
        this.autoResetAfterHours = autoResetAfterHours;
    }

    public Boolean getManualResetAvailable() {
        return manualResetAvailable;
    }

    public void setManualResetAvailable(Boolean manualResetAvailable) {
        this.manualResetAvailable = manualResetAvailable;
    }

    public Boolean getTaskSeriesComplete() {
        return taskSeriesComplete;
    }

    public void setTaskSeriesComplete(Boolean taskSeriesComplete) {
        this.taskSeriesComplete = taskSeriesComplete;
    }

    public Boolean getAutoResetEnabled() {
        return autoResetEnabled;
    }

    public void setAutoResetEnabled(Boolean autoResetEnabled) {
        this.autoResetEnabled = autoResetEnabled;
    }

    public List<String> getCustomConditions() {
        return customConditions;
    }

    public void setCustomConditions(List<String> customConditions) {
        this.customConditions = customConditions;
    }

    // Helper methods
    @JsonIgnore
    public boolean isAutoResetEnabled() {
        return autoResetEnabled != null && autoResetEnabled;
    }

    @JsonIgnore
    public boolean isManualResetAvailable() {
        return manualResetAvailable != null && manualResetAvailable;
    }

    @JsonIgnore
    public boolean isTaskSeriesComplete() {
        return taskSeriesComplete != null && taskSeriesComplete;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResetTriggers that = (ResetTriggers) o;
        return Objects.equals(autoResetCondition, that.autoResetCondition) &&
               Objects.equals(autoResetAfterHours, that.autoResetAfterHours) &&
               Objects.equals(manualResetAvailable, that.manualResetAvailable) &&
               Objects.equals(taskSeriesComplete, that.taskSeriesComplete) &&
               Objects.equals(autoResetEnabled, that.autoResetEnabled) &&
               Objects.equals(customConditions, that.customConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoResetCondition, autoResetAfterHours, manualResetAvailable,
                           taskSeriesComplete, autoResetEnabled, customConditions);
    }

    @Override
    public String toString() {
        return "ResetTriggers{" +
               "autoResetCondition='" + autoResetCondition + '\'' +
               ", autoResetAfterHours=" + autoResetAfterHours +
               ", taskSeriesComplete=" + taskSeriesComplete +
               ", autoResetEnabled=" + autoResetEnabled +
               '}';
    }
}
