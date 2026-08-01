package com.chachamaru.harness.workflow.orchestration;

import com.chachamaru.harness.workflow.model.Task;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Result of task execution.
 *
 * <p>Contains execution outcomes, metrics, and state changes
 * from a workflow orchestration run.</p>
 *
 * @spec_reference spec.md#Workflow System
 */
public record ExecutionResult(
    String executionId,
    String planId,
    ExecutionStatus status,
    List<TaskOutcome> outcomes,
    ExecutionMetrics metrics,
    LocalDateTime startTime,
    LocalDateTime endTime,
    String failureReason
) {
    /**
     * Execution status.
     */
    public enum ExecutionStatus {
        /** Execution is in progress */
        RUNNING,

        /** Execution completed successfully */
        SUCCESS,

        /** Execution failed */
        FAILED,

        /** Execution was cancelled */
        CANCELLED,

        /** Execution is paused */
        PAUSED
    }

    /**
     * Outcome of a single task execution.
     */
    public record TaskOutcome(
        String taskId,
        TaskOutcomeStatus status,
        String message,
        long durationMs,
        LocalDateTime completedAt
    ) {
        public enum TaskOutcomeStatus {
            /** Task completed successfully */
            SUCCESS,

            /** Task failed */
            FAILED,

            /** Task was skipped */
            SKIPPED,

            /** Task is still running */
            RUNNING
        }
    }

    /**
     * Execution metrics.
     */
    public record ExecutionMetrics(
        int totalTasks,
        int completedTasks,
        int failedTasks,
        int skippedTasks,
        long totalDurationMs,
        double parallelizationEfficiency
    ) {
        public ExecutionMetrics {
            if (parallelizationEfficiency < 0.0 || parallelizationEfficiency > 1.0) {
                throw new IllegalArgumentException("parallelizationEfficiency must be between 0.0 and 1.0");
            }
        }
    }

    /**
     * Creates an execution result.
     */
    public ExecutionResult {
        if (executionId == null || executionId.isBlank()) {
            throw new IllegalArgumentException("executionId cannot be null or blank");
        }
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId cannot be null or blank");
        }
        if (status == null) {
            status = ExecutionStatus.RUNNING;
        }
        if (outcomes == null) {
            outcomes = List.of();
        }
        if (startTime == null) {
            startTime = LocalDateTime.now();
        }
    }

    /**
     * Creates a successful execution result.
     */
    public static ExecutionResult success(String executionId, String planId, List<TaskOutcome> outcomes, ExecutionMetrics metrics, LocalDateTime startTime) {
        return new ExecutionResult(executionId, planId, ExecutionStatus.SUCCESS, outcomes, metrics, startTime, LocalDateTime.now(), null);
    }

    /**
     * Creates a failed execution result.
     */
    public static ExecutionResult failure(String executionId, String planId, String failureReason, LocalDateTime startTime) {
        return new ExecutionResult(executionId, planId, ExecutionStatus.FAILED, List.of(), null, startTime, LocalDateTime.now(), failureReason);
    }

    /**
     * Checks if execution was successful.
     */
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }

    /**
     * Gets success rate (0.0 to 1.0).
     */
    public double successRate() {
        if (outcomes.isEmpty()) {
            return 0.0;
        }
        long successful = outcomes.stream().filter(o -> o.status() == TaskOutcome.TaskOutcomeStatus.SUCCESS).count();
        return (double) successful / outcomes.size();
    }
}
