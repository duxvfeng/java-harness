package com.chachamaru.harness.collaboration.agent.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Result of agent execution.
 *
 * <p>Contains the outcome, output data, and metadata from an agent execution.</p>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public record AgentResult(
    String agentId,
    AgentStatus status,
    Object output,
    String message,
    Map<String, Object> metadata,
    LocalDateTime startTime,
    LocalDateTime endTime,
    long durationMs
) {
    /**
     * Agent execution status.
     */
    public enum AgentStatus {
        /** Agent is pending execution */
        PENDING,
        /** Agent is currently executing */
        RUNNING,
        /** Agent completed successfully */
        SUCCESS,
        /** Agent failed */
        FAILED,
        /** Agent was skipped */
        SKIPPED,
        /** Agent is waiting for external input */
        WAITING
    }

    /**
     * Creates an agent result.
     */
    public AgentResult {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId cannot be null or blank");
        }
        if (status == null) {
            status = AgentStatus.PENDING;
        }
        if (metadata == null) {
            metadata = Map.of();
        }
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
        if (endTime == null && status != AgentStatus.PENDING && status != AgentStatus.RUNNING && status != AgentStatus.WAITING) {
            endTime = LocalDateTime.now();
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * Creates a successful agent result.
     */
    public static AgentResult success(String agentId, Object output, String message, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        return new AgentResult(agentId, AgentStatus.SUCCESS, output, message, Map.of(), startTime, endTime, duration);
    }

    /**
     * Creates a failed agent result.
     */
    public static AgentResult failure(String agentId, String message, LocalDateTime startTime) {
        LocalDateTime endTime = LocalDateTime.now();
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        return new AgentResult(agentId, AgentStatus.FAILED, null, message, Map.of(), startTime, endTime, duration);
    }

    /**
     * Creates a waiting agent result.
     */
    public static AgentResult waiting(String agentId, String reason, LocalDateTime startTime) {
        return new AgentResult(agentId, AgentStatus.WAITING, null, reason, Map.of(), startTime, null, 0);
    }

    /**
     * Checks if agent execution was successful.
     */
    public boolean isSuccess() {
        return status == AgentStatus.SUCCESS;
    }

    /**
     * Checks if agent execution failed.
     */
    public boolean isFailed() {
        return status == AgentStatus.FAILED;
    }

    /**
     * Checks if agent is waiting for input.
     */
    public boolean isWaiting() {
        return status == AgentStatus.WAITING;
    }
}
