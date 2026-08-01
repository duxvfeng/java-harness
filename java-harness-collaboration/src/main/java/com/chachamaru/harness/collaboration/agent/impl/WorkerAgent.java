package com.chachamaru.harness.collaboration.agent.impl;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.message.AdvisorRequestV1;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import com.chachamaru.harness.protocol.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Worker agent for task execution.
 *
 * <p>The WorkerAgent is responsible for:
 * <ul>
 *   <li>Executing workflow tasks</li>
 *   <li>Managing task dependencies</li>
 *   <li>Tracking task progress</li>
 *   <li>Handling task failures and retries</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class WorkerAgent implements Agent {

    private static final Logger logger = LoggerFactory.getLogger(WorkerAgent.class);

    private final String id;
    private final String name;
    private int maxRetries = 3;
    private long timeoutMs = 300000; // 5 minutes default

    /**
     * Creates a WorkerAgent with default settings.
     */
    public WorkerAgent() {
        this("worker-default", "Default Worker Agent");
    }

    /**
     * Creates a WorkerAgent with custom ID and name.
     *
     * @param id the agent ID
     * @param name the agent name
     */
    public WorkerAgent(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Agent for executing workflow tasks";
    }

    @Override
    public AgentType getType() {
        return AgentType.WORKER;
    }

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        logger.info("WorkerAgent {} executing task", id);

        try {
            Task task = getTaskFromContext(context);
            int attempt = 0;
            Exception lastException = null;

            while (attempt < maxRetries) {
                try {
                    attempt++;
                    logger.info("Execution attempt {}/{} for task {}", attempt, maxRetries, task.id());

                    // Execute the task
                    Object result = executeTask(task, context, attempt);

                    logger.info("Task {} completed successfully on attempt {}", task.id(), attempt);
                    return AgentResult.success(id, result, "Task completed successfully", context.executionStartTime());

                } catch (Exception e) {
                    lastException = e;
                    logger.warn("Task {} failed on attempt {}: {}", task.id(), attempt, e.getMessage());

                    if (attempt >= maxRetries) {
                        break;
                    }

                    // Wait before retry (exponential backoff)
                    try {
                        long backoffMs = (long) Math.pow(2, attempt) * 1000;
                        logger.info("Waiting {}ms before retry", backoffMs);
                        Thread.sleep(Math.min(backoffMs, 10000)); // Cap at 10 seconds
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AgentExecutionException(id, "Worker interrupted during retry backoff", ie);
                    }
                }
            }

            // All retries exhausted
            String message = "Task failed after " + maxRetries + " attempts: " + lastException.getMessage();
            logger.error(message);
            throw new AgentExecutionException(id, message, lastException);

        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentExecutionException(id, "Unexpected error in worker agent: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canExecute(AgentContext context) {
        // Worker can execute if task is available
        return context.task() != null;
    }

    @Override
    public void initialize() throws AgentExecutionException {
        logger.info("WorkerAgent {} initialized", id);
    }

    @Override
    public void shutdown() throws AgentExecutionException {
        logger.info("WorkerAgent {} shut down", id);
    }

    /**
     * Gets the task from context.
     *
     * @param context the agent context
     * @return the task to execute
     * @throws AgentExecutionException if task not available
     */
    private Task getTaskFromContext(AgentContext context) throws AgentExecutionException {
        Task task = context.task();
        if (task == null) {
            throw new AgentExecutionException(id, "No task provided in context");
        }
        return task;
    }

    /**
     * Executes a single task.
     *
     * @param task the task to execute
     * @param context the agent context
     * @param attempt the attempt number
     * @return the task output
     * @throws Exception if execution fails
     */
    private Object executeTask(Task task, AgentContext context, int attempt) throws Exception {
        logger.debug("Executing task: {} (attempt {})", task.id(), attempt);

        // Check dependencies
        if (!areDependenciesSatisfied(task, context)) {
            throw new AgentExecutionException(id, "Task dependencies not satisfied for: " + task.id());
        }

        // Check if we have advisor response from previous consultation
        AdvisorRequestV1.QuestionType questionType = determineIfNeedsAdvisor(task, context);

        if (questionType != null && context.getSessionState("advisorResponse", Object.class) == null) {
            // Need advisor consultation - return request
            logger.info("Task {} requires advisor consultation", task.id());

            AdvisorRequestV1 request = AdvisorRequestV1.create(
                id,
                task.id(),
                generateAdvisorQuestion(task, context),
                questionType,
                task.description()
            );

            // Return waiting result with advisor request
            throw new AdvisorRequestException("Advisor consultation needed", request);
        }

        // Execute task implementation
        return executeTaskImplementation(task, context);
    }

    /**
     * Determines if task needs advisor consultation.
     *
     * @param task the task to check
     * @param context the agent context
     * @return the question type if advisor needed, null otherwise
     */
    private AdvisorRequestV1.QuestionType determineIfNeedsAdvisor(Task task, AgentContext context) {
        // Check if task involves complex architectural decisions
        if (task.description() != null && task.description().toLowerCase().matches(".*(architecture|design|pattern|structure).*")) {
            return AdvisorRequestV1.QuestionType.ARCHITECTURE;
        }

        // Check if task involves debugging
        if (task.description() != null && task.description().toLowerCase().matches(".*(debug|fix|issue|error|bug).*")) {
            return AdvisorRequestV1.QuestionType.DEBUGGING;
        }

        // Check if we have review findings that need addressing
        Object mustAddressObj = context.getSessionState("mustAddress", Object.class);
        if (mustAddressObj != null) {
            return AdvisorRequestV1.QuestionType.BEST_PRACTICE;
        }

        // Default: no advisor needed
        return null;
    }

    /**
     * Generates advisor question for the task.
     *
     * @param task the task
     * @param context the agent context
     * @return the question text
     */
    private String generateAdvisorQuestion(Task task, AgentContext context) {
        StringBuilder question = new StringBuilder();
        question.append("Task: ").append(task.title()).append("\n");
        question.append("Description: ").append(task.description()).append("\n");

        // Add review findings if present
        Object mustAddressObj = context.getSessionState("mustAddress", Object.class);
        if (mustAddressObj != null) {
            question.append("\nReview findings to address:\n").append(mustAddressObj).append("\n");
            question.append("How should I address these review findings?");
        } else {
            question.append("\nWhat is the recommended approach for implementing this task?");
        }

        return question.toString();
    }

    /**
     * Executes the actual task implementation.
     *
     * @param task the task to execute
     * @param context the agent context
     * @return the task output
     * @throws AgentExecutionException if execution fails
     */
    private Object executeTaskImplementation(Task task, AgentContext context) throws AgentExecutionException {
        logger.debug("Executing task implementation for: {}", task.id());

        // Placeholder: In real implementation, this would:
        // 1. Apply advisor recommendations if present
        // 2. Analyze task requirements
        // 3. Select appropriate execution strategy
        // 4. Execute the task logic
        // 5. Monitor progress
        // 6. Handle timeouts

        // Check for advisor recommendations
        Object advisorResponseObj = context.getSessionState("advisorResponse", Object.class);
        if (advisorResponseObj != null) {
            logger.debug("Applying advisor recommendations");
            // Apply recommendations to implementation
        }

        // Simulate task execution
        Map<String, Object> result = Map.of(
            "taskId", task.id(),
            "title", task.title(),
            "description", task.description(),
            "status", "completed",
            "workerId", id,
            "implementationApplied", advisorResponseObj != null
        );

        return result;
    }

    /**
     * Checks if task dependencies are satisfied.
     *
     * @param task the task to check
     * @param context the agent context
     * @return true if dependencies are satisfied, false otherwise
     */
    private boolean areDependenciesSatisfied(Task task, AgentContext context) {
        if (task.dependencies() == null || task.dependencies().isEmpty()) {
            return true;
        }

        // Check session state for completed dependencies
        for (String depId : task.dependencies()) {
            Object completed = context.getSessionState("task:" + depId + ":completed", Boolean.class);
            if (completed == null || !Boolean.TRUE.equals(completed)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the maximum retry count.
     *
     * @return the max retries
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Sets the maximum retry count.
     *
     * @param maxRetries the max retries to set
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(1, maxRetries);
    }

    /**
     * Gets the timeout in milliseconds.
     *
     * @return the timeout
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Sets the timeout in milliseconds.
     *
     * @param timeoutMs the timeout to set
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = Math.max(1000, timeoutMs);
    }
}
