package com.chachamaru.harness.isolation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Record of a user decision about branch isolation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DecisionRecord {

    @JsonProperty("timestamp")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime timestamp;

    @JsonProperty("seriesId")
    private String seriesId;

    @JsonProperty("task")
    private Double task;

    @JsonProperty("decision")
    private String decision;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("interactionType")
    private String interactionType;

    @JsonProperty("userChoice")
    private String userChoice;

    @JsonProperty("worktreePath")
    private String worktreePath;

    // Constructors
    public DecisionRecord() {
        this.timestamp = LocalDateTime.now();
    }

    public DecisionRecord(String seriesId, Double task, String decision, String reason) {
        this();
        this.seriesId = seriesId;
        this.task = task;
        this.decision = decision;
        this.reason = reason;
    }

    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getSeriesId() {
        return seriesId;
    }

    public void setSeriesId(String seriesId) {
        this.seriesId = seriesId;
    }

    public Double getTask() {
        return task;
    }

    public void setTask(Double task) {
        this.task = task;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
    }

    public String getUserChoice() {
        return userChoice;
    }

    public void setUserChoice(String userChoice) {
        this.userChoice = userChoice;
    }

    public String getWorktreePath() {
        return worktreePath;
    }

    public void setWorktreePath(String worktreePath) {
        this.worktreePath = worktreePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecisionRecord that = (DecisionRecord) o;
        return Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(seriesId, that.seriesId) &&
               Objects.equals(task, that.task) &&
               Objects.equals(decision, that.decision) &&
               Objects.equals(reason, that.reason) &&
               Objects.equals(interactionType, that.interactionType) &&
               Objects.equals(userChoice, that.userChoice) &&
               Objects.equals(worktreePath, that.worktreePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, seriesId, task, decision, reason,
                           interactionType, userChoice, worktreePath);
    }

    @Override
    public String toString() {
        return "DecisionRecord{" +
               "timestamp=" + timestamp +
               ", task=" + task +
               ", decision='" + decision + '\'' +
               ", reason='" + reason + '\'' +
               '}';
    }
}