package com.chachamaru.harness.workflow.execution;

import com.chachamaru.harness.protocol.model.Task;
import com.chachamaru.harness.workflow.orchestration.ExecutionResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Parallel task executor interface.
 *
 * <p>Executes tasks concurrently with support for dependency management,
 * parallelization limits, and execution strategies.</p>
 *
 * @spec_reference spec.md#Workflow System
 */
public interface ParallelExecutor {

    /**
     * Executes tasks in parallel respecting dependencies.
     *
     * <p>Tasks with no dependencies are executed immediately.
     * Tasks with dependencies wait for their dependencies to complete.</p>
     *
     * @param tasks Tasks to execute
     * @param taskFunction Function to execute a single task
     * @param <R> Result type
     * @return Future containing execution results
     * @throws ExecutionException if execution fails
     */
    <R> CompletableFuture<ExecutionResult> executeParallel(
        List<Task> tasks,
        Function<Task, R> taskFunction
    ) throws ExecutionException;

    /**
     * Executes tasks in parallel with a maximum concurrency limit.
     *
     * @param tasks Tasks to execute
     * @param taskFunction Function to execute a single task
     * @param maxConcurrency Maximum number of concurrent tasks
     * @param <R> Result type
     * @return Future containing execution results
     * @throws ExecutionException if execution fails
     */
    <R> CompletableFuture<ExecutionResult> executeParallel(
        List<Task> tasks,
        Function<Task, R> taskFunction,
        int maxConcurrency
    ) throws ExecutionException;

    /**
     * Executes tasks sequentially (for comparison).
     *
     * @param tasks Tasks to execute
     * @param taskFunction Function to execute a single task
     * @param <R> Result type
     * @return Execution results
     * @throws ExecutionException if execution fails
     */
    <R> ExecutionResult executeSequential(
        List<Task> tasks,
        Function<Task, R> taskFunction
    ) throws ExecutionException;

    /**
     * Gets the current concurrency limit.
     *
     * @return Maximum concurrent tasks
     */
    int getMaxConcurrency();

    /**
     * Sets the concurrency limit.
     *
     * @param maxConcurrency New maximum (must be > 0)
     * @throws IllegalArgumentException if maxConcurrency <= 0
     */
    void setMaxConcurrency(int maxConcurrency);

    /**
     * Exception thrown during task execution.
     */
    class ExecutionException extends Exception {
        private final String taskId;

        public ExecutionException(String message, String taskId) {
            super(message);
            this.taskId = taskId;
        }

        public ExecutionException(String message, Throwable cause, String taskId) {
            super(message, cause);
            this.taskId = taskId;
        }

        public String getTaskId() {
            return taskId;
        }
    }
}
