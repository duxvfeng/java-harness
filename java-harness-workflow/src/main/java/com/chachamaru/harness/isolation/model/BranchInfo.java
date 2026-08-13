package com.chachamaru.harness.isolation.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Information about the isolated branch.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchInfo {

    @JsonProperty("featureBranch")
    private String featureBranch;

    @JsonProperty("worktreePath")
    private String worktreePath;

    @JsonProperty("baseRef")
    private String baseRef;

    @JsonProperty("originalBranch")
    private String originalBranch;

    @JsonProperty("createdAt")
    @JsonSerialize(using = com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer.class)
    @JsonDeserialize(using = com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    // Constructors
    public BranchInfo() {
    }

    public BranchInfo(String featureBranch, String worktreePath, String baseRef, String originalBranch) {
        this.featureBranch = featureBranch;
        this.worktreePath = worktreePath;
        this.baseRef = baseRef;
        this.originalBranch = originalBranch;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getFeatureBranch() {
        return featureBranch;
    }

    public void setFeatureBranch(String featureBranch) {
        this.featureBranch = featureBranch;
    }

    public String getWorktreePath() {
        return worktreePath;
    }

    public void setWorktreePath(String worktreePath) {
        this.worktreePath = worktreePath;
    }

    public String getBaseRef() {
        return baseRef;
    }

    public void setBaseRef(String baseRef) {
        this.baseRef = baseRef;
    }

    public String getOriginalBranch() {
        return originalBranch;
    }

    public void setOriginalBranch(String originalBranch) {
        this.originalBranch = originalBranch;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BranchInfo branchInfo = (BranchInfo) o;
        return Objects.equals(featureBranch, branchInfo.featureBranch) &&
               Objects.equals(worktreePath, branchInfo.worktreePath) &&
               Objects.equals(baseRef, branchInfo.baseRef) &&
               Objects.equals(originalBranch, branchInfo.originalBranch) &&
               Objects.equals(createdAt, branchInfo.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(featureBranch, worktreePath, baseRef, originalBranch, createdAt);
    }

    @Override
    public String toString() {
        return "BranchInfo{" +
               "featureBranch='" + featureBranch + '\'' +
               ", worktreePath='" + worktreePath + '\'' +
               ", originalBranch='" + originalBranch + '\'' +
               ", createdAt=" + createdAt +
               '}';
    }
}