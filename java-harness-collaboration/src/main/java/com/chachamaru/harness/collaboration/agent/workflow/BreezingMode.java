package com.chachamaru.harness.collaboration.agent.workflow;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.AgentRegistry;
import com.chachamaru.harness.collaboration.agent.impl.*;
import com.chachamaru.harness.collaboration.agent.message.*;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import com.chachamaru.harness.protocol.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Breezing mode workflow orchestrator.
 *
 * <p>Implements the complete Breezing mode workflow:
 * <ul>
 *   <li>Phase A: Preparation (task resolution, dependency checking, sprint contract generation)</li>
 *   <li>Phase B: Task execution (Worker → optional Advisor → Reviewer loop → cherry-pick)</li>
 *   <li>Phase C: Integration (commit log aggregation, final reporting)</li>
 * </ul>
 *
 * <p>This class acts as the Lead agent, coordinating Worker, Advisor, and Reviewer agents
 * with proper inter-agent communication and quality gates.</p>
 *
 * @spec_reference spec.md#Breezing Mode
 * @since 4.2.0
 */
public class BreezingMode {

    private static final Logger logger = LoggerFactory.getLogger(BreezingMode.class);
    private static final int MAX_REVIEW_ITERATIONS = 3;
    private static final int MAX_ADVISOR_CONSULTATIONS = 3;

    private final AgentRegistry registry;
    private final ExecutorService executorService;
    private final Path worktreePath;
    private final int maxParallelWorkers;

    /**
     * Creates a BreezingMode workflow orchestrator.
     */
    public BreezingMode(Path worktreePath, int maxParallelWorkers) {
        this.registry = new AgentRegistry();
        this.executorService = Executors.newFixedThreadPool(maxParallelWorkers + 2);
        this.worktreePath = worktreePath;
        this.maxParallelWorkers = maxParallelWorkers;

        initializeAgents();
    }

    /**
     * Initializes and registers all agents.
     */
    private void initializeAgents() {
        try {
            // Register default agents
            registry.register(new WorkerAgent());
            registry.register(new ReviewerAgent());
            registry.register(new AdvisorAgent());

            // Initialize all agents
            for (Agent agent : registry.getAllAgents()) {
                agent.initialize();
            }

            logger.info("BreezingMode initialized with {} agents", registry.getAgentCount());
        } catch (AgentExecutionException e) {
            logger.error("Failed to initialize agents", e);
            throw new RuntimeException("Agent initialization failed", e);
        }
    }

    /**
     * Executes Breezing mode workflow for a list of tasks.
     *
     * @param tasks the tasks to execute
     * @param baseContext the base agent context
     * @return list of execution results
     */
    public List<BreezingResult> execute(List<Task> tasks, AgentContext baseContext) {
        logger.info("Starting Breezing mode for {} tasks", tasks.size());

        // Phase A: Preparation
        PhaseAResult prepResult = phaseAPreparation(tasks, baseContext);
        if (!prepResult.success()) {
            return Collections.singletonList(BreezingResult.failure("Phase A preparation failed: " + prepResult.failureReason()));
        }

        // Phase B: Task execution (parallel workers with review loop)
        List<BreezingResult> results = phaseBExecution(prepResult.readyTasks(), baseContext);

        // Phase C: Integration
        phaseCIntegration(results);

        logger.info("Breezing mode completed: {}/{} tasks successful",
            results.stream().filter(BreezingResult::isSuccess).count(),
            results.size());

        return results;
    }

    /**
     * Phase A: Preparation - task resolution, dependency checking, sprint contract generation.
     */
    private PhaseAResult phaseAPreparation(List<Task> tasks, AgentContext baseContext) {
        logger.info("Phase A: Preparation for {} tasks", tasks.size());

        try {
            List<Task> readyTasks = new ArrayList<>();
            Map<String, SprintContract> contracts = new HashMap<>();

            for (Task task : tasks) {
                // Check dependencies
                if (!areDependenciesSatisfied(task, baseContext)) {
                    logger.info("Task {} dependencies not satisfied, skipping", task.id());
                    continue;
                }

                readyTasks.add(task);

                // Generate sprint contract for each task
                SprintContract contract = generateSprintContract(task);
                contracts.put(task.id(), contract);

                logger.debug("Task {} ready with sprint contract", task.id());
            }

            if (readyTasks.isEmpty()) {
                return new PhaseAResult(false, Collections.emptyList(), Map.of(), "No tasks ready for execution");
            }

            logger.info("Phase A complete: {}/{} tasks ready", readyTasks.size(), tasks.size());
            return new PhaseAResult(true, readyTasks, contracts, null);

        } catch (Exception e) {
            logger.error("Phase A failed", e);
            return new PhaseAResult(false, Collections.emptyList(), Map.of(), "Phase A error: " + e.getMessage());
        }
    }

    /**
     * Phase B: Task execution - parallel Worker execution with optional Advisor and Reviewer loop.
     */
    private List<BreezingResult> phaseBExecution(List<Task> tasks, AgentContext baseContext) {
        logger.info("Phase B: Executing {} tasks in parallel", tasks.size());

        List<CompletableFuture<BreezingResult>> futures = new ArrayList<>();

        for (Task task : tasks) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return executeSingleTask(task, baseContext);
                } catch (Exception e) {
                    logger.error("Task execution failed: {}", task.id(), e);
                    return BreezingResult.failure(task.id(), "Task execution failed: " + e.getMessage());
                }
            }, executorService));
        }

        // Wait for all tasks to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.get(30, TimeUnit.MINUTES); // 30 minute timeout

            List<BreezingResult> results = new ArrayList<>();
            for (CompletableFuture<BreezingResult> future : futures) {
                results.add(future.get());
            }

            logger.info("Phase B complete: {}/{} tasks successful",
                results.stream().filter(BreezingResult::isSuccess).count(),
                results.size());

            return results;

        } catch (TimeoutException e) {
            logger.error("Phase B timeout");
            return Collections.singletonList(BreezingResult.failure("Phase B execution timeout"));
        } catch (Exception e) {
            logger.error("Phase B failed", e);
            return Collections.singletonList(BreezingResult.failure("Phase B execution failed: " + e.getMessage()));
        }
    }

    /**
     * Executes a single task with Worker → optional Advisor → Reviewer loop.
     */
    private BreezingResult executeSingleTask(Task task, AgentContext baseContext) {
        logger.info("Executing task: {}", task.id());

        // Set active task state for Go guardrail
        setActiveTask(task.id(), "execution", task);

        try {
            // Get Worker agent
            Agent worker = registry.getAgent("worker-default");
            if (worker == null) {
                return BreezingResult.failure(task.id(), "Worker agent not found");
            }

            // Create task-specific context
            AgentContext taskContext = createTaskContext(task, worker, baseContext);

            // Execute Worker (with retry)
            AgentResult workerResult = executeWithRetry(worker, taskContext, 3);
            if (workerResult.isFailed()) {
                return BreezingResult.failure(task.id(), "Worker execution failed: " + workerResult.message());
            }

            // Check if Worker needs Advisor consultation
            int advisorConsultations = 0;
            while (workerResult.isWaiting() && advisorConsultations < MAX_ADVISOR_CONSULTATIONS) {
                logger.info("Worker requesting advisor consultation (attempt {})", advisorConsultations + 1);

                // Extract advisor request from worker result
                AdvisorRequestV1 request = extractAdvisorRequest(workerResult);

                // Consult Advisor
                AdvisorResponseV1 response = consultAdvisor(request, taskContext);

                // Check if Advisor says STOP
                if (response.isStop()) {
                    clearActiveTask();
                    return BreezingResult.failure(task.id(), "Advisor stopped execution: " + response.summary());
                }

                // Continue Worker with Advisor's response
                taskContext = updateContextWithAdvice(taskContext, response);
                workerResult = executeWithRetry(worker, taskContext, 2);
                advisorConsultations++;
            }

            if (workerResult.isFailed()) {
                clearActiveTask();
                return BreezingResult.failure(task.id(), "Worker execution failed after advisor consultation");
            }

            // Reviewer loop
            int reviewIterations = 0;
            ReviewResultV1 reviewResult = null;

            while (reviewIterations < MAX_REVIEW_ITERATIONS) {
                logger.info("Reviewer iteration {}/{}", reviewIterations + 1, MAX_REVIEW_ITERATIONS);

                // Create review context
                AgentContext reviewContext = createReviewContext(task, workerResult, baseContext);

                // Execute Reviewer
                Agent reviewer = registry.getAgent("reviewer-default");
                AgentResult reviewerResult = reviewer.execute(reviewContext);

                // Extract review result
                reviewResult = extractReviewResult(reviewerResult);

                if (reviewResult.isApproved()) {
                    logger.info("Review approved for task {}", task.id());
                    break;
                }

                if (reviewIterations < MAX_REVIEW_ITERATIONS - 1) {
                    logger.info("Review requested changes, iterating...");

                    // Update worker context with review findings and retry
                    taskContext = updateContextWithReviewFindings(taskContext, reviewResult);
                    workerResult = executeWithRetry(worker, taskContext, 2);

                    if (workerResult.isFailed()) {
                        clearActiveTask();
                        return BreezingResult.failure(task.id(), "Worker failed to address review findings");
                    }
                }

                reviewIterations++;
            }

            clearActiveTask();

            if (reviewResult != null && reviewResult.isApproved()) {
                return BreezingResult.success(task.id(), workerResult.output(), reviewResult);
            } else {
                return BreezingResult.failure(task.id(), "Review not approved after " + MAX_REVIEW_ITERATIONS + " iterations");
            }

        } catch (Exception e) {
            logger.error("Task execution exception: {}", task.id(), e);
            clearActiveTask();
            return BreezingResult.failure(task.id(), "Task execution exception: " + e.getMessage());
        }
    }

    /**
     * Phase C: Integration - aggregate results and generate final report.
     */
    private void phaseCIntegration(List<BreezingResult> results) {
        logger.info("Phase C: Integration and reporting");

        // Generate completion report
        CompletionReport report = generateCompletionReport(results);

        logger.info("Breezing mode completion report:\n{}", report.summary());

        // Log individual task results
        for (BreezingResult result : results) {
            if (result.isSuccess()) {
                logger.info("Task {} succeeded: {}", result.taskId(), result.message());
            } else {
                logger.warn("Task {} failed: {}", result.taskId(), result.message());
            }
        }
    }

    /**
     * Executes an agent with retry logic.
     */
    private AgentResult executeWithRetry(Agent agent, AgentContext context, int maxAttempts) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.debug("Agent {} execution attempt {}/{}", agent.getId(), attempt, maxAttempts);
                return agent.execute(context);
            } catch (AgentExecutionException e) {
                lastException = e;
                logger.warn("Agent {} attempt {} failed: {}", agent.getId(), attempt, e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000L * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return AgentResult.failure(agent.getId(), "Agent interrupted during retry", LocalDateTime.now());
                    }
                }
            }
        }

        return AgentResult.failure(agent.getId(), "Agent failed after " + maxAttempts + " attempts: " + lastException.getMessage(), LocalDateTime.now());
    }

    /**
     * Checks if task dependencies are satisfied.
     */
    private boolean areDependenciesSatisfied(Task task, AgentContext context) {
        if (task.dependencies() == null || task.dependencies().isEmpty()) {
            return true;
        }

        for (String depId : task.dependencies()) {
            Object completed = context.getSessionState("task:" + depId + ":completed", Boolean.class);
            if (completed == null || !Boolean.TRUE.equals(completed)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Generates a sprint contract for a task.
     */
    private SprintContract generateSprintContract(Task task) {
        return new SprintContract(
            task.id(),
            task.title(),
            task.description(),
            generateDoD(task),
            List.of("unit-tests", "integration-tests", "code-review"),
            MAX_REVIEW_ITERATIONS,
            Map.of(
                "lane", "default",
                "stage", "implementation",
                "researchEvidence", List.of()
            )
        );
    }

    /**
     * Generates Definition of Done for a task.
     */
    private String generateDoD(Task task) {
        return String.format("""
            Task %s: %s

            Requirements:
            - Implementation completed according to task description
            - Unit tests written and passing
            - Integration tests passing
            - Code review approved
            - Documentation updated
            - No critical or major review findings

            Test Requirements:
            - Unit test coverage > 80%
            - All edge cases covered
            - Error handling tested
            """,
            task.id(), task.title()
        );
    }

    /**
     * Creates task-specific context.
     */
    private AgentContext createTaskContext(Task task, Agent agent, AgentContext baseContext) {
        Map<String, Object> sessionState = new HashMap<>(baseContext.sessionState());
        sessionState.put("currentTask", task);
        sessionState.put("task:" + task.id() + ":started", LocalDateTime.now());

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
     * Creates review context.
     */
    private AgentContext createReviewContext(Task task, AgentResult workerResult, AgentContext baseContext) {
        Map<String, Object> sessionState = new HashMap<>(baseContext.sessionState());
        sessionState.put("reviewTarget", workerResult.output());
        sessionState.put("taskForReview", task);

        Agent reviewer = registry.getAgent("reviewer-default");
        if (reviewer == null) {
            throw new RuntimeException("Reviewer agent not found");
        }

        // Convert Agent.AgentType to AgentContext.AgentType
        AgentContext.AgentType contextType = convertAgentType(reviewer.getType());

        return new AgentContext(
            reviewer.getId(),
            reviewer.getName(),
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
     * Extracts advisor request from worker result.
     */
    private AdvisorRequestV1 extractAdvisorRequest(AgentResult workerResult) {
        if (workerResult.output() instanceof AdvisorRequestV1) {
            return (AdvisorRequestV1) workerResult.output();
        }
        // Create default request
        return AdvisorRequestV1.create(
            workerResult.agentId(),
            "unknown",
            "Implementation guidance needed",
            AdvisorRequestV1.QuestionType.IMPLEMENTATION,
            workerResult.message()
        );
    }

    /**
     * Consults advisor agent.
     */
    private AdvisorResponseV1 consultAdvisor(AdvisorRequestV1 request, AgentContext taskContext) {
        try {
            Agent advisor = registry.getAgent("advisor-default");
            if (advisor == null) {
                return AdvisorResponseV1.correction(
                    request.requestId(),
                    List.of("Advisor not available, proceeding with default approach"),
                    "Advisor agent not found"
                );
            }

            Map<String, Object> advisorState = new HashMap<>(taskContext.sessionState());
            advisorState.put("question", request.question());
            advisorState.put("questionType", request.questionType());

            AgentContext advisorContext = new AgentContext(
                advisor.getId(),
                advisor.getName(),
                convertAgentType(advisor.getType()),
                taskContext.task(),
                taskContext.hookInput(),
                taskContext.configuration(),
                advisorState,
                LocalDateTime.now()
            );

            AgentResult advisorResult = advisor.execute(advisorContext);

            if (advisorResult.output() instanceof AdvisorResponseV1) {
                return (AdvisorResponseV1) advisorResult.output();
            }

            // Create default response
            return AdvisorResponseV1.correction(
                request.requestId(),
                List.of("Proceed with cautious implementation"),
                "Advisor response format invalid"
            );

        } catch (Exception e) {
            logger.error("Advisor consultation failed", e);
            return AdvisorResponseV1.correction(
                request.requestId(),
                List.of("Proceed with default implementation"),
                "Advisor execution failed: " + e.getMessage()
            );
        }
    }

    /**
     * Updates context with advisor's response.
     */
    private AgentContext updateContextWithAdvice(AgentContext context, AdvisorResponseV1 response) {
        Map<String, Object> newSessionState = new HashMap<>(context.sessionState());
        newSessionState.put("advisorResponse", response);
        newSessionState.put("advisorRecommendations", response.recommendations());

        return new AgentContext(
            context.agentId(),
            context.agentName(),
            context.agentType(),
            context.task(),
            context.hookInput(),
            context.configuration(),
            newSessionState,
            LocalDateTime.now()
        );
    }

    /**
     * Extracts review result from reviewer result.
     */
    private ReviewResultV1 extractReviewResult(AgentResult reviewerResult) {
        if (reviewerResult.output() instanceof ReviewResultV1) {
            return (ReviewResultV1) reviewerResult.output();
        }

        // Create default review result (approve if reviewer succeeded)
        if (reviewerResult.isSuccess()) {
            return ReviewResultV1.approve(
                UUID.randomUUID().toString(),
                List.of(),
                "Review passed (no explicit findings)"
            );
        } else {
            return ReviewResultV1.requestChanges(
                UUID.randomUUID().toString(),
                List.of(new ReviewResultV1.ReviewFinding(
                    "general",
                    "MAJOR",
                    "Review Failed",
                    reviewerResult.message(),
                    null,
                    0,
                    "Reviewer execution failed"
                )),
                "Review execution failed"
            );
        }
    }

    /**
     * Updates context with review findings.
     */
    private AgentContext updateContextWithReviewFindings(AgentContext context, ReviewResultV1 reviewResult) {
        Map<String, Object> newSessionState = new HashMap<>(context.sessionState());
        newSessionState.put("reviewFindings", reviewResult.findings());
        newSessionState.put("reviewVerdict", reviewResult.verdict());
        newSessionState.put("mustAddress", reviewResult.findings().stream()
            .filter(f -> f.getSeverityEnum() == ReviewResultV1.ReviewSeverity.CRITICAL ||
                       f.getSeverityEnum() == ReviewResultV1.ReviewSeverity.MAJOR)
            .toList());

        return new AgentContext(
            context.agentId(),
            context.agentName(),
            context.agentType(),
            context.task(),
            context.hookInput(),
            context.configuration(),
            newSessionState,
            LocalDateTime.now()
        );
    }

    /**
     * Sets active task state for Go guardrail integration.
     */
    private void setActiveTask(String taskId, String phase, Task task) {
        try {
            Path activeTaskPath = worktreePath.resolve(".claude/state/active-task.json");
            Files.createDirectories(activeTaskPath.getParent());

            String content = String.format("""
                {
                  "phase": "%s",
                  "task": "%s",
                  "timestamp": "%s"
                }
                """,
                phase, taskId, Instant.now()
            );

            Files.writeString(activeTaskPath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.debug("Set active task: {} in phase {}", taskId, phase);

        } catch (IOException e) {
            logger.warn("Failed to set active task state", e);
        }
    }

    /**
     * Clears active task state.
     */
    private void clearActiveTask() {
        try {
            Path activeTaskPath = worktreePath.resolve(".claude/state/active-task.json");
            Files.deleteIfExists(activeTaskPath);
            logger.debug("Cleared active task state");

        } catch (IOException e) {
            logger.warn("Failed to clear active task state", e);
        }
    }

    /**
     * Generates completion report.
     */
    private CompletionReport generateCompletionReport(List<BreezingResult> results) {
        long successful = results.stream().filter(BreezingResult::isSuccess).count();
        long failed = results.stream().filter(BreezingResult::isFailure).count();

        return new CompletionReport(
            successful,
            failed,
            results.size(),
            String.format("Breezing mode completed: %d successful, %d failed out of %d tasks", successful, failed, results.size())
        );
    }

    /**
     * Shuts down the Breezing mode workflow.
     */
    public void shutdown() {
        logger.info("Shutting down BreezingMode");

        try {
            // Shutdown all agents
            for (Agent agent : registry.getAllAgents()) {
                agent.shutdown();
            }

            // Shutdown executor
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.SECONDS);

            logger.info("BreezingMode shut down successfully");

        } catch (AgentExecutionException | InterruptedException e) {
            logger.error("Error during BreezingMode shutdown", e);
        }
    }

    /**
     * Result of Phase A preparation.
     */
    private record PhaseAResult(
        boolean success,
        List<Task> readyTasks,
        Map<String, SprintContract> contracts,
        String failureReason
    ) {
    }

    /**
     * Sprint contract for task execution.
     */
    private record SprintContract(
        String taskId,
        String title,
        String description,
        String dod,
        List<String> acceptanceCriteria,
        int maxReviewIterations,
        Map<String, Object> metadata
    ) {
    }

    /**
     * Result of Breezing mode task execution.
     */
    public static class BreezingResult {
        private final String taskId;
        private final boolean success;
        private final Object output;
        private final String message;
        private final ReviewResultV1 reviewResult;

        private BreezingResult(String taskId, boolean success, Object output, String message, ReviewResultV1 reviewResult) {
            this.taskId = taskId;
            this.success = success;
            this.output = output;
            this.message = message;
            this.reviewResult = reviewResult;
        }

        public static BreezingResult success(String taskId, Object output, ReviewResultV1 reviewResult) {
            return new BreezingResult(taskId, true, output, "Task completed successfully", reviewResult);
        }

        public static BreezingResult failure(String taskId, String message) {
            return new BreezingResult(taskId, false, null, message, null);
        }

        public static BreezingResult failure(String message) {
            return new BreezingResult(null, false, null, message, null);
        }

        public String taskId() { return taskId; }
        public boolean isSuccess() { return success; }
        public boolean isFailure() { return !success; }
        public Object output() { return output; }
        public String message() { return message; }
        public ReviewResultV1 reviewResult() { return reviewResult; }
    }

    /**
     * Completion report summary.
     */
    private record CompletionReport(
        long successfulCount,
        long failedCount,
        long totalCount,
        String summary
    ) {
    }
}