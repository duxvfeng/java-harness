package com.chachamaru.harness.isolation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Information about the current task series and isolation context.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeriesInfo {

    @JsonProperty("seriesId")
    private String seriesId;

    @JsonProperty("startDate")
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private LocalDateTime startDate;

    @JsonProperty("lastActivityDate")
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private LocalDateTime lastActivityDate;

    @JsonProperty("lastCommitDate")
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private LocalDateTime lastCommitDate;

    @JsonProperty("taskSequence")
    private List<Double> taskSequence = new ArrayList<>();

    @JsonProperty("currentTask")
    private Double currentTask;

    @JsonProperty("taskCount")
    private Integer taskCount;

    @JsonProperty("isolationActive")
    private Boolean isolationActive;

    @JsonProperty("autoResetPending")
    private Boolean autoResetPending;

    @JsonProperty("branchInfo")
    private BranchInfo branchInfo;

    @JsonProperty("seriesContext")
    private SeriesContext seriesContext;

    // Constructors
    public SeriesInfo() {
    }

    public SeriesInfo(String seriesId) {
        this.seriesId = seriesId;
        this.startDate = LocalDateTime.now();
        this.isolationActive = false;
        this.autoResetPending = false;
    }

    // Getters and Setters
    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getLastActivityDate() {
        return lastActivityDate;
    }

    public void setLastActivityDate(LocalDateTime lastActivityDate) {
        this.lastActivityDate = lastActivityDate;
    }

    public LocalDateTime getLastCommitDate() {
        return lastCommitDate;
    }

    public void setLastCommitDate(LocalDateTime lastCommitDate) {
        this.lastCommitDate = lastCommitDate;
    }

    public List<Double> getTaskSequence() {
        return taskSequence;
    }

    public void setTaskSequence(List<Double> taskSequence) {
        this.taskSequence = taskSequence;
    }

    public Double getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(Double currentTask) {
        this.currentTask = currentTask;
    }

    public Integer getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(Integer taskCount) {
        this.taskCount = taskCount;
    }

    public Boolean getIsolationActive() {
        return isolationActive;
    }

    public void setIsolationActive(Boolean isolationActive) {
        this.isolationActive = isolationActive;
    }

    public Boolean getAutoResetPending() {
        return autoResetPending;
    }

    public void setAutoResetPending(Boolean autoResetPending) {
        this.autoResetPending = autoResetPending;
    }

    public BranchInfo getBranchInfo() {
        return branchInfo;
    }

    public void setBranchInfo(BranchInfo branchInfo) {
        this.branchInfo = branchInfo;
    }

    public SeriesContext getSeriesContext() {
        return seriesContext;
    }

    public Integer getEstimatedTasks() {
        if (seriesContext != null) {
            return seriesContext.getEstimatedTasks();
        }
        return null;
    }

    public void setSeriesContext(SeriesContext seriesContext) {
        this.seriesContext = seriesContext;
    }

    // Helper methods
    public void addTaskToSequence(double taskId) {
        if (this.taskSequence == null) {
            this.taskSequence = new ArrayList<>();
        }
        this.taskSequence.add(taskId);
        this.taskCount = this.taskSequence.size();
        this.currentTask = taskId;
        this.lastActivityDate = LocalDateTime.now();
    }

    public boolean isIsolationActive() {
        return isolationActive != null && isolationActive;
    }

    public boolean isAutoResetPending() {
        return autoResetPending != null && autoResetPending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeriesInfo seriesInfo = (SeriesInfo) o;
        return Objects.equals(seriesId, seriesInfo.seriesId) &&
               Objects.equals(startDate, seriesInfo.startDate) &&
               Objects.equals(lastActivityDate, seriesInfo.lastActivityDate) &&
               Objects.equals(lastCommitDate, seriesInfo.lastCommitDate) &&
               Objects.equals(taskSequence, seriesInfo.taskSequence) &&
               Objects.equals(currentTask, seriesInfo.currentTask) &&
               Objects.equals(taskCount, seriesInfo.taskCount) &&
               Objects.equals(isolationActive, seriesInfo.isolationActive) &&
               Objects.equals(autoResetPending, seriesInfo.autoResetPending) &&
               Objects.equals(branchInfo, seriesInfo.branchInfo) &&
               Objects.equals(seriesContext, seriesInfo.seriesContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seriesId, startDate, lastActivityDate, lastCommitDate,
                           taskSequence, currentTask, taskCount, isolationActive,
                           autoResetPending, branchInfo, seriesContext);
    }

    @Override
    public String toString() {
        return "SeriesInfo{" +
               "seriesId='" + seriesId + '\'' +
               ", startDate=" + startDate +
               ", lastActivityDate=" + lastActivityDate +
               ", taskCount=" + taskCount +
               ", currentTask=" + currentTask +
               ", isolationActive=" + isolationActive +
               '}';
    }
}