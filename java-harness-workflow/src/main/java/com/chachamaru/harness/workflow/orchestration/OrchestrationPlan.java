package com.chachamaru.harness.workflow.orchestration;

import com.chachamaru.harness.workflow.model.PlansDocument;
import com.chachamaru.harness.workflow.model.Task;

import java.util.List;

/**
 * Workflow orchestration plan.
 *
 * <p>Represents a computed execution plan for a set of tasks,
 * including execution order, parallelization strategy, and dependencies.</p>
 *
 * @spec_reference spec.md#Workflow System
 */
public record OrchestrationPlan(
    String planId,
    String sourceDocument,
    List<Task> allTasks,
    List<Task> readyTasks,
    List<Task> pendingTasks,
    ExecutionStrategy strategy
) {
    /**
     * Execution strategy for task execution.
     */
    public enum ExecutionStrategy {
        /** Execute tasks sequentially in dependency order */
        SEQUENTIAL,

        /** Execute tasks in parallel when possible */
        PARALLEL,

        /** Mixed strategy with critical path prioritization */
        HYBRID
    }

    /**
     * Creates an orchestration plan.
     */
    public OrchestrationPlan {
        if (planId == null || planId.isBlank()) {
            throw new IllegalArgumentException("planId cannot be null or blank");
        }
        if (allTasks == null) {
            allTasks = List.of();
        }
        if (readyTasks == null) {
            readyTasks = List.of();
        }
        if (pendingTasks == null) {
            pendingTasks = List.of();
        }
        if (strategy == null) {
            strategy = ExecutionStrategy.SEQUENTIAL;
        }
    }

    /**
     * Gets total task count.
     */
    public int totalTaskCount() {
        return allTasks.size();
    }

    /**
     * Gets completion percentage (0.0 to 1.0).
     */
    public double completionPercentage() {
        if (allTasks.isEmpty()) {
            return 1.0;
        }
        long completed = allTasks.stream().filter(t -> t.status().isCompleted()).count();
        return (double) completed / allTasks.size();
    }

    /**
     * Checks if the plan is fully executed.
     */
    public boolean isComplete() {
        return allTasks.stream().allMatch(t -> t.status().isCompleted());
    }
}
