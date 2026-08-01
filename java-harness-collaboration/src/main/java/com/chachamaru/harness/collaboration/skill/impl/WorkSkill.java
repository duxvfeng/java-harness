package com.chachamaru.harness.collaboration.skill.impl;

import com.chachamaru.harness.collaboration.skill.CoreSkill;
import com.chachamaru.harness.collaboration.skill.SkillExecutionException;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.protocol.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Skill for executing work tasks.
 *
 * <p>The WorkSkill is responsible for:
 * <ul>
 *   <li>Executing individual tasks from Plans.md</li>
 *   <li>Managing task execution flow</li>
 *   <li>Handling task dependencies</li>
 *   <li>Tracking task progress</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class WorkSkill extends CoreSkill {

    private static final Logger logger = LoggerFactory.getLogger(WorkSkill.class);

    /**
     * Creates a WorkSkill.
     */
    public WorkSkill() {
    }

    @Override
    public String getId() {
        return "work";
    }

    @Override
    public String getName() {
        return "Work Skill";
    }

    @Override
    public String getDescription() {
        return "Skill for executing work tasks";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected Object doExecute(SkillContext context) throws SkillExecutionException {
        logger.info("Executing WorkSkill");

        try {
            // Get task from context
            Task task = getTaskFromContext(context);

            // Execute the task
            TaskResult result = executeTask(task, context);

            logger.info("Successfully executed task: {}", task.id());
            return result;

        } catch (Exception e) {
            String message = "Failed to execute work skill: " + e.getMessage();
            logger.error(message, e);
            throw new SkillExecutionException(getId(), message, e);
        }
    }

    @Override
    protected void validateContext(SkillContext context) throws SkillExecutionException {
        super.validateContext(context);

        // Additional validation: check if task is available
        if (context.task() == null) {
            throw new SkillExecutionException(getId(), "Task not provided in context");
        }
    }

    /**
     * Gets the task from context.
     *
     * @param context the skill context
     * @return the task to execute
     */
    private Task getTaskFromContext(SkillContext context) {
        return context.task();
    }

    /**
     * Executes a single task.
     *
     * @param task the task to execute
     * @param context the skill context
     * @return the task execution result
     * @throws SkillExecutionException if execution fails
     */
    private TaskResult executeTask(Task task, SkillContext context) throws SkillExecutionException {
        logger.info("Executing task: {} - {}", task.id(), task.title());

        // Check dependencies
        if (!areDependenciesSatisfied(task, context)) {
            String message = "Task dependencies not satisfied for: " + task.id();
            logger.warn(message);
            return new TaskResult(task.id(), TaskResult.TaskStatus.SKIPPED, message, null);
        }

        // Execute task logic (placeholder - actual implementation depends on task type)
        Object output = executeTaskImplementation(task, context);

        return new TaskResult(task.id(), TaskResult.TaskStatus.SUCCESS, "Task executed successfully", output);
    }

    /**
     * Checks if task dependencies are satisfied.
     *
     * @param task the task to check
     * @param context the skill context
     * @return true if dependencies are satisfied, false otherwise
     */
    private boolean areDependenciesSatisfied(Task task, SkillContext context) {
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
     * Executes the actual task implementation.
     *
     * <p>This is a placeholder implementation. Real implementation would delegate
     * to appropriate executors based on task type and configuration.</p>
     *
     * @param task the task to execute
     * @param context the skill context
     * @return the task output
     * @throws SkillExecutionException if execution fails
     */
    private Object executeTaskImplementation(Task task, SkillContext context) throws SkillExecutionException {
        // Placeholder: In real implementation, this would:
        // 1. Analyze task description
        // 2. Select appropriate executor
        // 3. Execute the task
        // 4. Return results

        logger.debug("Executing task implementation for: {}", task.id());

        // Simulate task execution
        Map<String, Object> result = Map.of(
            "taskId", task.id(),
            "title", task.title(),
            "description", task.description(),
            "status", "completed"
        );

        return result;
    }

    /**
     * Result of task execution.
     */
    public record TaskResult(
        String taskId,
        TaskStatus status,
        String message,
        Object output
    ) {
        /**
         * Task execution status.
         */
        public enum TaskStatus {
            /** Task is pending execution */
            PENDING,
            /** Task is currently executing */
            RUNNING,
            /** Task completed successfully */
            SUCCESS,
            /** Task failed */
            FAILED,
            /** Task was skipped (e.g., dependencies not met) */
            SKIPPED
        }
    }
}
