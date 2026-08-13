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
 * Current code status in the isolated branch.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeStatus {

    @JsonProperty("hasUncommittedChanges")
    private Boolean hasUncommittedChanges;

    @JsonProperty("lastCommitTime")
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private LocalDateTime lastCommitTime;

    @JsonProperty("branchClean")
    private Boolean branchClean;

    @JsonProperty("filesChanged")
    private List<String> filesChanged = new ArrayList<>();

    @JsonProperty("commitsCount")
    private Integer commitsCount;

    @JsonProperty("lastCommitMessage")
    private String lastCommitMessage;

    @JsonProperty("untrackedFilesCount")
    private Integer untrackedFilesCount;

    @JsonProperty("detectionError")
    private String detectionError;

    // Constructors
    public CodeStatus() {
    }

    // Getters and Setters
    public Boolean getHasUncommittedChanges() {
        return hasUncommittedChanges;
    }

    public void setHasUncommittedChanges(Boolean hasUncommittedChanges) {
        this.hasUncommittedChanges = hasUncommittedChanges;
    }

    public LocalDateTime getLastCommitTime() {
        return lastCommitTime;
    }

    public void setLastCommitTime(LocalDateTime lastCommitTime) {
        this.lastCommitTime = lastCommitTime;
    }

    public Boolean getBranchClean() {
        return branchClean;
    }

    public void setBranchClean(Boolean branchClean) {
        this.branchClean = branchClean;
    }

    public List<String> getFilesChanged() {
        return filesChanged;
    }

    public void setFilesChanged(List<String> filesChanged) {
        this.filesChanged = filesChanged;
    }

    public Integer getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(Integer commitsCount) {
        this.commitsCount = commitsCount;
    }

    public String getLastCommitMessage() {
        return lastCommitMessage;
    }

    public void setLastCommitMessage(String lastCommitMessage) {
        this.lastCommitMessage = lastCommitMessage;
    }

    public Integer getUntrackedFilesCount() {
        return untrackedFilesCount;
    }

    public void setUntrackedFilesCount(Integer untrackedFilesCount) {
        this.untrackedFilesCount = untrackedFilesCount;
    }

    public String getDetectionError() {
        return detectionError;
    }

    public void setDetectionError(String detectionError) {
        this.detectionError = detectionError;
    }

    // Helper methods
    public boolean hasUncommittedChanges() {
        return hasUncommittedChanges != null && hasUncommittedChanges;
    }

    public boolean isBranchClean() {
        return branchClean != null && branchClean;
    }

    public boolean hasError() {
        return detectionError != null && !detectionError.isEmpty();
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CodeStatus instance = new CodeStatus();

        public Builder hasUncommittedChanges(Boolean hasUncommittedChanges) {
            instance.setHasUncommittedChanges(hasUncommittedChanges);
            return this;
        }

        public Builder lastCommitTime(LocalDateTime lastCommitTime) {
            instance.setLastCommitTime(lastCommitTime);
            return this;
        }

        public Builder branchClean(Boolean branchClean) {
            instance.setBranchClean(branchClean);
            return this;
        }

        public Builder filesChanged(List<String> filesChanged) {
            instance.setFilesChanged(filesChanged);
            return this;
        }

        public Builder commitsCount(Integer commitsCount) {
            instance.setCommitsCount(commitsCount);
            return this;
        }

        public Builder lastCommitMessage(String lastCommitMessage) {
            instance.setLastCommitMessage(lastCommitMessage);
            return this;
        }

        public Builder untrackedFilesCount(Integer untrackedFilesCount) {
            instance.setUntrackedFilesCount(untrackedFilesCount);
            return this;
        }

        public Builder detectionError(String detectionError) {
            instance.setDetectionError(detectionError);
            return this;
        }

        public CodeStatus build() {
            return instance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeStatus that = (CodeStatus) o;
        return Objects.equals(hasUncommittedChanges, that.hasUncommittedChanges) &&
               Objects.equals(lastCommitTime, that.lastCommitTime) &&
               Objects.equals(branchClean, that.branchClean) &&
               Objects.equals(filesChanged, that.filesChanged) &&
               Objects.equals(commitsCount, that.commitsCount) &&
               Objects.equals(lastCommitMessage, that.lastCommitMessage) &&
               Objects.equals(untrackedFilesCount, that.untrackedFilesCount) &&
               Objects.equals(detectionError, that.detectionError);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hasUncommittedChanges, lastCommitTime, branchClean,
                           filesChanged, commitsCount, lastCommitMessage,
                           untrackedFilesCount, detectionError);
    }

    @Override
    public String toString() {
        return "CodeStatus{" +
               "hasUncommittedChanges=" + hasUncommittedChanges +
               ", branchClean=" + branchClean +
               ", commitsCount=" + commitsCount +
               ", detectionError='" + detectionError + '\'' +
               '}';
    }
}