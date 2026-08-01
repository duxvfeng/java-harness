package com.chachamaru.harness.collaboration.skill.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of skill execution.
 *
 * <p>Contains the outcome, output data, and metadata from a skill execution.</p>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public record SkillResult(
    String skillId,
    SkillStatus status,
    Object output,
    String message,
    Map<String, Object> metadata,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long durationMs
) {
    /**
     * Skill execution status.
     */
    public enum SkillStatus {
        /** Skill is pending execution */
        PENDING,

        /** Skill is currently executing */
        RUNNING,

        /** Skill completed successfully */
        SUCCESS,

        /** Skill failed */
        FAILED,

        /** Skill was skipped */
        SKIPPED
    }

    /**
     * Creates a skill result.
     */
    public SkillResult {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId cannot be null or blank");
        }
        if (status == null) {
            status = SkillStatus.PENDING;
        }
        if (metadata == null) {
            metadata = Map.of();
        }
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (endTime == null && status != SkillStatus.PENDING && status != SkillStatus.RUNNING) {
            endTime = LocalDateTime.now();
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * Creates a successful skill result.
     */
    public static SkillResult success(String skillId, Object output, String message, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        return new SkillResult(skillId, SkillStatus.SUCCESS, output, message, Map.of(), startTime, endTime, duration);
    }

    /**
     * Creates a failed skill result.
     */
    public static SkillResult failure(String skillId, String message, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        return new SkillResult(skillId, SkillStatus.FAILED, null, message, Map.of(), startTime, endTime, duration);
    }

    /**
     * Checks if skill execution was successful.
     */
    public boolean isSuccess() {
        return status == SkillStatus.SUCCESS;
    }

    /**
     * Checks if skill execution failed.
     */
    public boolean isFailed() {
        return status == SkillStatus.FAILED;
    }
}
