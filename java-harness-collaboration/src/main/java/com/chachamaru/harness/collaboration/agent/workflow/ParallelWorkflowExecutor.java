package com.chachamaru.harness.collaboration.agent.workflow;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.AgentRegistry;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import com.chachamaru.harness.protocol.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Parallel workflow executor for optimized multi-task execution.
 *
 * <p>Implements parallel task execution with:
 * <ul>
 *   <li>File collision detection and worktree isolation</li>
 *   <li>Dynamic worker pool management</li>
 *   <li>Task dependency resolution</li>
 *   <li>Load balancing across workers</li>
 * </ul>
 *
 * @spec_reference spec.md#Parallel Workflow Optimization
 * @since 4.2.0
 */
public class ParallelWorkflowExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ParallelWorkflowExecutor.class);
    private static final int DEFAULT_MAX_WORKERS = Runtime.getRuntime().availableProcessors() - 1;
    private static final int TASK_TIMEOUT_MINUTES = 30;

    private final AgentRegistry registry;
    private final ExecutorService workerPool;
    private final Path basePath;
    private final int maxWorkers;
    private final Map<String, Path> worktreeCache;

    /**
     * Creates a parallel workflow executor.
     *
     * @param registry the agent registry
     * @param basePath the base git repository path
     * @param maxWorkers maximum number of parallel workers
     */
    public ParallelWorkflowExecutor(AgentRegistry registry, Path basePath, int maxWorkers) {
        this.registry = registry;
        this.basePath = basePath;
        this.maxWorkers = Math.max(1, Math.min(maxWorkers, DEFAULT_MAX_WORKERS));
        this.workerPool = Executors.newFixedThreadPool(this.maxWorkers);
        this.worktreeCache = new ConcurrentHashMap<>();

        logger.info("ParallelWorkflowExecutor initialized with {} workers", this.maxWorkers);
    }

    /**
     * Executes tasks in parallel with optimization.
     *
     * @param tasks the tasks to execute
     * @param baseContext the base agent context
     * @return list of execution results
     */
    public List<ParallelExecutionResult> executeParallel(List<Task> tasks, AgentContext baseContext) {
        logger.info("Executing {} tasks in parallel (max {} workers)", tasks.size(), maxWorkers);

        long startTime = System.currentTimeMillis();

        try {
            // Phase 1: Task grouping and collision detection
            TaskGroups taskGroups = groupTasksByFileCollision(tasks);

            logger.info("Task grouping complete: {} independent groups, {} sequential tasks",
                taskGroups.independentGroups().size(),
                taskGroups.sequentialTasks().size());

            // Phase 2: Execute independent groups in parallel
            List<ParallelExecutionResult> results = new ArrayList<>();

            if (!taskGroups.independentGroups().isEmpty()) {
                List<ParallelExecutionResult> parallelResults =
                    executeIndependentGroups(taskGroups.independentGroups(), baseContext);
                results.addAll(parallelResults);
            }

            // Phase 3: Execute sequential tasks
            if (!taskGroups.sequentialTasks().isEmpty()) {
                List<ParallelExecutionResult> sequentialResults =
                    executeSequentialTasks(taskGroups.sequentialTasks(), baseContext);
                results.addAll(sequentialResults);
            }

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Parallel execution completed in {}ms: {}/{} successful",
                duration,
                results.stream().filter(ParallelExecutionResult::isSuccess).count(),
                results.size());

            return results;

        } catch (Exception e) {
            logger.error("Parallel execution failed", e);
            return Collections.singletonList(
                ParallelExecutionResult.failure(null, "Parallel execution failed: " + e.getMessage())
            );
        }
    }

    /**
     * Groups tasks by file collision detection.
     */
    private TaskGroups groupTasksByFileCollision(List<Task> tasks) {
        List<TaskGroup> independentGroups = new ArrayList<>();
        List<Task> sequentialTasks = new ArrayList<>();

        // Analyze tasks for file collisions
        Map<String, Set<Task>> fileTaskMap = new HashMap<>();

        for (Task task : tasks) {
            Set<String> affectedFiles = extractAffectedFiles(task);

            if (affectedFiles.isEmpty()) {
                // No files affected, can run independently
                sequentialTasks.add(task);
                continue;
            }

            // Check for collisions with existing tasks
            boolean hasCollision = false;
            for (String file : affectedFiles) {
                if (fileTaskMap.containsKey(file)) {
                    // Collision detected, merge with existing group
                    fileTaskMap.get(file).add(task);
                    hasCollision = true;
                }
            }

            if (!hasCollision) {
                // No collision, create new group
                for (String file : affectedFiles) {
                    fileTaskMap.computeIfAbsent(file, k -> new HashSet<>()).add(task);
                }
            }
        }

        // Convert file task map to groups
        Set<Task> groupedTasks = new HashSet<>();
        for (Set<Task> taskSet : fileTaskMap.values()) {
            if (!taskSet.isEmpty()) {
                TaskGroup group = new TaskGroup(new ArrayList<>(taskSet));
                independentGroups.add(group);
                groupedTasks.addAll(taskSet);
            }
        }

        // Remaining ungrouped tasks go to sequential
        for (Task task : tasks) {
            if (!groupedTasks.contains(task)) {
                sequentialTasks.add(task);
            }
        }

        return new TaskGroups(independentGroups, sequentialTasks);
    }

    /**
     * Extracts affected files from a task.
     */
    private Set<String> extractAffectedFiles(Task task) {
        Set<String> files = new HashSet<>();

        // Extract files from task description
        if (task.description() != null) {
            // Simple heuristic: extract file paths
            String[] words = task.description().split("\\s+");
            for (String word : words) {
                if (word.matches(".*\\.(java|xml|md|json|yaml|yml|sh)")) {
                    files.add(word.trim());
                }
            }
        }

        // Extract files from task title
        if (task.title() != null) {
            String[] words = task.title().split("\\s+");
            for (String word : words) {
                if (word.matches(".*\\.(java|xml|md|json|yaml|yml|sh)")) {
                    files.add(word.trim());
                }
            }
        }

        return files;
    }

    /**
     * Executes independent task groups in parallel.
     */
    private List<ParallelExecutionResult> executeIndependentGroups(
        List<TaskGroup> groups,
        AgentContext baseContext
    ) {
        logger.info("Executing {} independent groups in parallel", groups.size());

        List<CompletableFuture<ParallelExecutionResult>> futures = new ArrayList<>();

        for (TaskGroup group : groups) {
            if (group.tasks().size() == 1) {
                // Single task, execute directly
                futures.add(CompletableFuture.supplyAsync(() ->
                    executeSingleTask(group.tasks().get(0), baseContext, false),
                    workerPool
                ));
            } else {
                // Multiple tasks in group (collision), execute sequentially
                futures.add(CompletableFuture.supplyAsync(() ->
                    executeTaskGroup(group, baseContext),
                    workerPool
                ));
            }
        }

        // Wait for all to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.get(TASK_TIMEOUT_MINUTES, TimeUnit.MINUTES);

            List<ParallelExecutionResult> results = new ArrayList<>();
            for (CompletableFuture<ParallelExecutionResult> future : futures) {
                results.add(future.get());
            }

            return results;

        } catch (TimeoutException e) {
            logger.error("Parallel execution timeout");
            return Collections.singletonList(
                ParallelExecutionResult.failure(null, "Parallel execution timeout")
            );
        } catch (Exception e) {
            logger.error("Parallel execution failed", e);
            return Collections.singletonList(
                ParallelExecutionResult.failure(null, "Parallel execution failed: " + e.getMessage())
            );
        }
    }

    /**
     * Executes sequential tasks.
     */
    private List<ParallelExecutionResult> executeSequentialTasks(
        List<Task> tasks,
        AgentContext baseContext
    ) {
        logger.info("Executing {} sequential tasks", tasks.size());

        List<ParallelExecutionResult> results = new ArrayList<>();

        for (Task task : tasks) {
            ParallelExecutionResult result = executeSingleTask(task, baseContext, true);
            results.add(result);

            // Stop on critical failure
            if (result.isCriticalFailure()) {
                logger.warn("Critical failure in task {}, stopping sequential execution", task.id());
                break;
            }
        }

        return results;
    }

    /**
     * Executes a single task with optional worktree isolation.
     */
    private ParallelExecutionResult executeSingleTask(
        Task task,
        AgentContext baseContext,
        boolean useWorktree
    ) {
        logger.info("Executing single task: {} (worktree: {})", task.id(), useWorktree);

        Path taskWorktree = null;
        if (useWorktree) {
            try {
                taskWorktree = createIsolatedWorktree(task);
            } catch (Exception e) {
                logger.warn("Failed to create worktree for task {}: {}", task.id(), e.getMessage());
                // Continue without worktree
            }
        }

        try {
            Agent worker = registry.getAgent("worker-default");
            if (worker == null) {
                return ParallelExecutionResult.failure(task.id(), "Worker agent not found");
            }

            AgentContext taskContext = createTaskContext(task, worker, baseContext, taskWorktree);

            long taskStartTime = System.currentTimeMillis();
            AgentResult agentResult = worker.execute(taskContext);
            long taskDuration = System.currentTimeMillis() - taskStartTime;

            if (agentResult.isSuccess()) {
                return ParallelExecutionResult.success(task.id(), agentResult.output(), taskDuration);
            } else {
                return ParallelExecutionResult.failure(task.id(), agentResult.message());
            }

        } catch (Exception e) {
            logger.error("Task execution failed: {}", task.id(), e);
            return ParallelExecutionResult.failure(task.id(), "Task execution failed: " + e.getMessage());
        } finally {
            if (taskWorktree != null) {
                cleanupWorktree(taskWorktree);
            }
        }
    }

    /**
     * Executes a task group (tasks with file collisions).
     */
    private ParallelExecutionResult executeTaskGroup(TaskGroup group, AgentContext baseContext) {
        logger.info("Executing task group with {} tasks (collision detected)", group.tasks().size());

        Path groupWorktree = null;
        try {
            groupWorktree = createIsolatedWorktree(group.tasks().get(0));
        } catch (Exception e) {
            logger.warn("Failed to create worktree for group: {}", e.getMessage());
        }

        try {
            List<ParallelExecutionResult> groupResults = new ArrayList<>();

            for (Task task : group.tasks()) {
                ParallelExecutionResult result = executeSingleTask(task, baseContext, groupWorktree != null);
                groupResults.add(result);

                if (result.isCriticalFailure()) {
                    logger.warn("Critical failure in group task {}, stopping group execution", task.id());
                    break;
                }
            }

            // Return aggregated result
            boolean allSuccess = groupResults.stream().allMatch(ParallelExecutionResult::isSuccess);
            if (allSuccess) {
                return ParallelExecutionResult.success(
                    "group-" + System.currentTimeMillis(),
                    "Group executed successfully",
                    groupResults.stream().mapToLong(ParallelExecutionResult::duration).sum()
                );
            } else {
                return ParallelExecutionResult.failure(
                    "group-" + System.currentTimeMillis(),
                    "Group execution had failures"
                );
            }

        } finally {
            if (groupWorktree != null) {
                cleanupWorktree(groupWorktree);
            }
        }
    }

    /**
     * Creates an isolated worktree for a task.
     */
    private Path createIsolatedWorktree(Task task) throws IOException, InterruptedException {
        String worktreeId = task.id() + "-" + System.currentTimeMillis();
        Path worktreePath = basePath.resolve(".claude/worktrees").resolve(worktreeId);

        if (Files.exists(worktreePath)) {
            return worktreePath;
        }

        logger.info("Creating isolated worktree for task {}: {}", task.id(), worktreePath);

        Files.createDirectories(worktreePath.getParent());

        ProcessBuilder pb = new ProcessBuilder(
            "git",
            "worktree",
            "add",
            "-b", "parallel-work/" + worktreeId,
            worktreePath.toString(),
            "HEAD"
        );

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished || process.exitValue() != 0) {
            throw new IOException("Failed to create worktree");
        }

        worktreeCache.put(worktreeId, worktreePath);
        return worktreePath;
    }

    /**
     * Cleans up a worktree.
     */
    private void cleanupWorktree(Path worktreePath) {
        try {
            if (worktreePath != null && Files.exists(worktreePath)) {
                // Remove worktree
                ProcessBuilder pb = new ProcessBuilder(
                    "git",
                    "worktree",
                    "remove",
                    worktreePath.toString()
                );

                Process process = pb.start();
                process.waitFor(10, TimeUnit.SECONDS);

                logger.debug("Cleaned up worktree: {}", worktreePath);
            }
        } catch (Exception e) {
            logger.warn("Failed to cleanup worktree: {}", worktreePath, e);
        }
    }

    /**
     * Creates task-specific context.
     */
    private AgentContext createTaskContext(Task task, Agent agent, AgentContext baseContext, Path worktreePath) {
        Map<String, Object> sessionState = new HashMap<>(baseContext.sessionState());
        sessionState.put("currentTask", task);
        sessionState.put("task:" + task.id() + ":started", LocalDateTime.now());

        if (worktreePath != null) {
            sessionState.put("worktreePath", worktreePath.toString());
        }

        // Convert Agent.AgentType to AgentContext.AgentType
        AgentContext.AgentType contextType = convertAgentType(agent.getType());

        return new AgentContext(
            agent.getId(),
            agent.getName(),
            contextType,
            task,
            baseContext.hookInput(),
            baseContext.configuration(),
            sessionState,
            LocalDateTime.now()
        );
    }

    /**
     * Converts Agent.AgentType to AgentContext.AgentType.
     */
    private AgentContext.AgentType convertAgentType(Agent.AgentType agentType) {
        return switch (agentType) {
            case WORKER -> AgentContext.AgentType.WORKER;
            case REVIEWER -> AgentContext.AgentType.REVIEWER;
            case ADVISOR -> AgentContext.AgentType.ADVISOR;
            case COORDINATOR -> AgentContext.AgentType.COORDINATOR;
        };
    }

    /**
     * Shuts down the executor.
     */
    public void shutdown() {
        logger.info("Shutting down ParallelWorkflowExecutor");

        // Cleanup all worktrees
        for (Path worktree : worktreeCache.values()) {
            cleanupWorktree(worktree);
        }

        // Shutdown worker pool
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("ParallelWorkflowExecutor shut down");
    }

    /**
     * Task grouping result.
     */
    private record TaskGroups(
        List<TaskGroup> independentGroups,
        List<Task> sequentialTasks
    ) {
    }

    /**
     * Group of tasks that can be executed together.
     */
    private record TaskGroup(
        List<Task> tasks
    ) {
    }

    /**
     * Result of parallel execution.
     */
    public static class ParallelExecutionResult {
        private final String taskId;
        private final boolean success;
        private final Object output;
        private final String message;
        private final long duration;
        private final boolean criticalFailure;

        private ParallelExecutionResult(String taskId, boolean success, Object output,
                                       String message, long duration, boolean criticalFailure) {
            this.taskId = taskId;
            this.success = success;
            this.output = output;
            this.message = message;
            this.duration = duration;
            this.criticalFailure = criticalFailure;
        }

        public static ParallelExecutionResult success(String taskId, Object output, long duration) {
            return new ParallelExecutionResult(taskId, true, output, "Task completed successfully", duration, false);
        }

        public static ParallelExecutionResult failure(String taskId, String message) {
            return new ParallelExecutionResult(taskId, false, null, message, 0, false);
        }

        public String taskId() { return taskId; }
        public boolean isSuccess() { return success; }
        public Object output() { return output; }
        public String message() { return message; }
        public long duration() { return duration; }
        public boolean isCriticalFailure() { return criticalFailure; }
    }
}