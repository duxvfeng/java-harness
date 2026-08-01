package com.chachamaru.harness.collaboration.skill;

import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Abstract base class for skill implementations.
 *
 * <p>Provides common functionality and default implementations for skills.
 * Extending this class is recommended but not required - the {@link Skill}
 * interface can be implemented directly.</p>
 *
 * <p>This base class provides:
 * <ul>
 *   <li>Logging infrastructure</li>
 *   <li>Common validation logic</li>
 *   <li>Standard error handling</li>
 *   <li>Lifecycle hooks</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public abstract class CoreSkill implements Skill {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Creates a core skill.
     */
    protected CoreSkill() {
    }

    @Override
    public SkillResult execute(SkillContext context) throws SkillExecutionException {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            logger.info("Executing skill: {} (ID: {})", getName(), getId());

            // Pre-execution validation
            validateContext(context);

            if (!canExecute(context)) {
                logger.warn("Skill {} cannot execute in current context", getId());
                return SkillResult.success(getId(), null, "Skipped: cannot execute in current context", startTime);
            }

            // Pre-execution hook
            beforeExecution(context);

            // Execute the skill
            Object output = doExecute(context);

            // Post-execution hook
            afterExecution(context, output);

            logger.info("Skill {} completed successfully", getId());
            return SkillResult.success(getId(), output, "Skill executed successfully", startTime);

        } catch (SkillExecutionException e) {
            logger.error("Skill {} failed: {}", getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error executing skill {}", getId(), e);
            throw new SkillExecutionException(getId(), "Unexpected error during skill execution: " + e.getMessage(), e);
        } finally {
            cleanup(context);
        }
    }

    /**
     * Validates the skill context before execution.
     *
     * @param context the skill context to validate
     * @throws SkillExecutionException if validation fails
     */
    protected void validateContext(SkillContext context) throws SkillExecutionException {
        if (context == null) {
            throw new SkillExecutionException(getId(), "Skill context cannot be null");
        }
        if (!context.skillId().equals(getId())) {
            throw new SkillExecutionException(getId(), "Skill context ID mismatch: expected " + getId() + ", got " + context.skillId());
        }
    }

    /**
     * Hook called before skill execution.
     *
     * <p>Override this method to perform pre-execution setup or validation.</p>
     *
     * @param context the skill context
     * @throws SkillExecutionException if pre-execution fails
     */
    protected void beforeExecution(SkillContext context) throws SkillExecutionException {
        // Default: no-op
    }

    /**
     * Performs the actual skill execution.
     *
     * <p>This method must be implemented by subclasses to provide the specific skill logic.</p>
     *
     * @param context the skill context
     * @return the skill execution output
     * @throws SkillExecutionException if execution fails
     */
    protected abstract Object doExecute(SkillContext context) throws SkillExecutionException;

    /**
     * Hook called after successful skill execution.
     *
     * <p>Override this method to perform post-execution cleanup or processing.</p>
     *
     * @param context the skill context
     * @param output the execution output
     * @throws SkillExecutionException if post-execution fails
     */
    protected void afterExecution(SkillContext context, Object output) throws SkillExecutionException {
        // Default: no-op
    }

    /**
     * Hook called after skill execution (success or failure).
     *
     * <p>Override this method to perform cleanup that should always happen.</p>
     *
     * @param context the skill context
     */
    protected void cleanup(SkillContext context) {
        // Default: no-op
    }

    /**
     * Gets the logger for this skill.
     *
     * @return the logger
     */
    protected Logger getLogger() {
        return logger;
    }
}
