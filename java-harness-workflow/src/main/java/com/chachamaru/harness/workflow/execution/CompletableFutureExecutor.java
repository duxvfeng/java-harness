package com.chachamaru.harness.workflow.execution;

import com.chachamaru.harness.protocol.model.Task;
import com.chachamaru.harness.workflow.orchestration.ExecutionResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CompletableFuture-based parallel task executor.
 *
 * <p>Executes tasks concurrently using CompletableFuture with:
 * <ul>
 *   <li>Automatic dependency resolution</li>
 *   <li>Configurable concurrency limits</li>
 *   <li>Exception handling and aggregation</li>
 *   <li>Performance metrics tracking</li>
 * </ul>
 *
 * @spec_reference spec.md#Workflow System
 */
public class CompletableFutureExecutor implements ParallelExecutor {

    private final Executor executor;
    private int maxConcurrency;

    /**
     * Creates an executor with default concurrency (Runtime.availableProcessors()).
     */
    public CompletableFutureExecutor() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates an executor with specified concurrency.
     *
     * @param maxConcurrency Maximum concurrent tasks
     */
    public CompletableFutureExecutor(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        this.executor = Executors.newFixedThreadPool(maxConcurrency);
    }

    @Override
    public <R> CompletableFuture<ExecutionResult> executeParallel(
        List<Task> tasks,
        Function<Task, R> taskFunction
    ) throws ExecutionException {
        return executeParallel(tasks, taskFunction, maxConcurrency);
    }

    @Override
    public <R> CompletableFuture<ExecutionResult> executeParallel(
        List<Task> tasks,
        Function<Task, R> taskFunction,
        int concurrencyLimit
    ) throws ExecutionException {
        if (tasks == null || tasks.isEmpty()) {
            return CompletableFuture.completedFuture(emptyResult());
        }

        String executionId = UUID.randomUUID().toString();
        String planId = "parallel-plan-" + System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();

        // Create a semaphore for concurrency control
        Semaphore semaphore = new Semaphore(concurrencyLimit);
        Map<String, CompletableFuture<R>> futures = new ConcurrentHashMap<>();
        List<ExecutionResult.TaskOutcome> outcomes = new CopyOnWriteArrayList<>();

        // Execute all tasks
        for (Task task : tasks) {
            CompletableFuture<R> future = executeTaskWithDependencies(
                task, tasks, taskFunction, semaphore, futures, outcomes, executionId
            );
            futures.put(task.id(), future);
        }

        // Wait for all to complete and aggregate results
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                long totalDuration = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();

                ExecutionResult.ExecutionMetrics metrics = new ExecutionResult.ExecutionMetrics(
                    tasks.size(),
                    (int) outcomes.stream().filter(o -> o.status() == ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS).count(),
                    (int) outcomes.stream().filter(o -> o.status() == ExecutionResult.TaskOutcome.TaskOutcomeStatus.FAILED).count(),
                    (int) outcomes.stream().filter(o -> o.status() == ExecutionResult.TaskOutcome.TaskOutcomeStatus.SKIPPED).count(),
                    totalDuration,
                    calculateEfficiency(tasks.size(), totalDuration, concurrencyLimit)
                );

                return new ExecutionResult(
                    executionId,
                    planId,
                    ExecutionResult.ExecutionStatus.SUCCESS,
                    outcomes,
                    metrics,
                    startTime,
                    LocalDateTime.now(),
                    null
                );
            })
            .exceptionally(ex -> {
                long totalDuration = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
                return failureResult(executionId, planId, ex.getMessage(), startTime, outcomes);
            });
    }

    @Override
    public <R> ExecutionResult executeSequential(
        List<Task> tasks,
        Function<Task, R> taskFunction
    ) throws ExecutionException {
        if (tasks == null || tasks.isEmpty()) {
            return emptyResult();
        }

        String executionId = UUID.randomUUID().toString();
        String planId = "sequential-plan-" + System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();
        List<ExecutionResult.TaskOutcome> outcomes = new ArrayList<>();

        try {
            for (Task task : tasks) {
                LocalDateTime taskStart = LocalDateTime.now();
                ExecutionResult.TaskOutcome.TaskOutcomeStatus status;
                String message;
                long durationMs;

                try {
                    R result = taskFunction.apply(task);
                    status = ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS;
                    message = "Task completed successfully";
                    durationMs = java.time.Duration.between(taskStart, LocalDateTime.now()).toMillis();
                } catch (Exception e) {
                    status = ExecutionResult.TaskOutcome.TaskOutcomeStatus.FAILED;
                    message = "Task failed: " + e.getMessage();
                    durationMs = java.time.Duration.between(taskStart, LocalDateTime.now()).toMillis();
                }

                outcomes.add(new ExecutionResult.TaskOutcome(
                    task.id(), status, message, durationMs, LocalDateTime.now()
                ));
            }

            long totalDuration = java.time.Duration.between(startTime, LocalDateTime.now()).toMillis();
            ExecutionResult.ExecutionMetrics metrics = new ExecutionResult.ExecutionMetrics(
                tasks.size(),
                (int) outcomes.stream().filter(o -> o.status() == ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS).count(),
                (int) outcomes.stream().filter(o -> o.status() == ExecutionResult.TaskOutcome.TaskOutcomeStatus.FAILED).count(),
                0,
                totalDuration,
                0.0 // Sequential has no parallelization efficiency
            );

            return new ExecutionResult(
                executionId,
                planId,
                ExecutionResult.ExecutionStatus.SUCCESS,
                outcomes,
                metrics,
                startTime,
                LocalDateTime.now(),
                null
            );

        } catch (Exception e) {
            return ExecutionResult.failure(executionId, planId, e.getMessage(), startTime);
        }
    }

    @Override
    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    @Override
    public void setMaxConcurrency(int maxConcurrency) {
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency must be > 0");
        }
        this.maxConcurrency = maxConcurrency;
    }

    private <R> CompletableFuture<R> executeTaskWithDependencies(
        Task task,
        List<Task> allTasks,
        Function<Task, R> taskFunction,
        Semaphore semaphore,
        Map<String, CompletableFuture<R>> futures,
        List<ExecutionResult.TaskOutcome> outcomes,
        String executionId
    ) {
        // Wait for dependencies to complete
        List<CompletableFuture<R>> dependencies = task.dependencies().stream()
            .filter(futures::containsKey)
            .map(futures::get)
            .toList();

        CompletableFuture<Void> dependenciesComplete = CompletableFuture.allOf(
            dependencies.toArray(new CompletableFuture[0])
        );

        // Execute this task after dependencies
        return dependenciesComplete.thenComposeAsync(v -> {
            try {
                semaphore.acquire();
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        LocalDateTime taskStart = LocalDateTime.now();
                        R result = taskFunction.apply(task);

                        long durationMs = java.time.Duration.between(taskStart, LocalDateTime.now()).toMillis();
                        outcomes.add(new ExecutionResult.TaskOutcome(
                            task.id(),
                            ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS,
                            "Task completed successfully",
                            durationMs,
                            LocalDateTime.now()
                        ));

                        return result;
                    } catch (Exception e) {
                        outcomes.add(new ExecutionResult.TaskOutcome(
                            task.id(),
                            ExecutionResult.TaskOutcome.TaskOutcomeStatus.FAILED,
                            e.getMessage(),
                            0,
                            LocalDateTime.now()
                        ));
                        throw new CompletionException(e);
                    } finally {
                        semaphore.release();
                    }
                }, executor);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                outcomes.add(new ExecutionResult.TaskOutcome(
                    task.id(),
                    ExecutionResult.TaskOutcome.TaskOutcomeStatus.FAILED,
                    "Task interrupted",
                    0,
                    LocalDateTime.now()
                ));
                return CompletableFuture.failedFuture(e);
            }
        }, executor);
    }

    private double calculateEfficiency(int taskCount, long totalDurationMs, int concurrency) {
        // Ideal duration = sequential duration / concurrency
        // Efficiency = ideal duration / actual duration
        // Simplified calculation assuming average task duration
        if (taskCount <= 1 || totalDurationMs <= 0) {
            return 1.0;
        }

        // Estimate sequential time (very rough approximation)
        long estimatedSequentialTime = totalDurationMs * concurrency;
        double idealParallelTime = estimatedSequentialTime / (double) concurrency;

        double efficiency = Math.min(1.0, idealParallelTime / totalDurationMs);
        return Math.max(0.0, efficiency);
    }

    private ExecutionResult emptyResult() {
        return new ExecutionResult(
            UUID.randomUUID().toString(),
            "empty-plan",
            ExecutionResult.ExecutionStatus.SUCCESS,
            List.of(),
            new ExecutionResult.ExecutionMetrics(0, 0, 0, 0, 0, 1.0),
            LocalDateTime.now(),
            LocalDateTime.now(),
            null
        );
    }

    private ExecutionResult failureResult(String executionId, String planId, String message, LocalDateTime startTime, List<ExecutionResult.TaskOutcome> outcomes) {
        return new ExecutionResult(
            executionId,
            planId,
            ExecutionResult.ExecutionStatus.FAILED,
            outcomes,
            new ExecutionResult.ExecutionMetrics(
                outcomes.size(),
                (int) outcomes.stream().filter(o -> o.status() == ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS).count(),
                (int) outcomes.stream().filter(o -> o.status() == ExecutionResult.TaskOutcome.TaskOutcomeStatus.FAILED).count(),
                0,
                java.time.Duration.between(startTime, LocalDateTime.now()).toMillis(),
                0.0
            ),
            startTime,
            LocalDateTime.now(),
            message
        );
    }
}
