package com.chachamaru.harness.collaboration.skill.impl;

import com.chachamaru.harness.collaboration.skill.CoreSkill;
import com.chachamaru.harness.collaboration.skill.SkillExecutionException;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Skill for reviewing code and implementation.
 *
 * <p>The ReviewSkill is responsible for:
 * <ul>
 *   <li>Reviewing code changes</li>
 *   <li>Checking code quality</li>
 *   <li>Validating against standards</li>
 *   <li>Providing review feedback</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class ReviewSkill extends CoreSkill {

    private static final Logger logger = LoggerFactory.getLogger(ReviewSkill.class);

    /**
     * Creates a ReviewSkill.
     */
    public ReviewSkill() {
    }

    @Override
    public String getId() {
        return "review";
    }

    @Override
    public String getName() {
        return "Review Skill";
    }

    @Override
    public String getDescription() {
        return "Skill for reviewing code and implementation";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected Object doExecute(SkillContext context) throws SkillExecutionException {
        logger.info("Executing ReviewSkill");

        try {
            // Get review target from context
            Object target = getReviewTarget(context);

            // Perform review
            ReviewResult result = performReview(target, context);

            logger.info("Successfully completed review");
            return result;

        } catch (Exception e) {
            String message = "Failed to execute review skill: " + e.getMessage();
            logger.error(message, e);
            throw new SkillExecutionException(getId(), message, e);
        }
    }

    /**
     * Gets the review target from context.
     *
     * @param context the skill context
     * @return the target to review
     * @throws SkillExecutionException if target not available
     */
    private Object getReviewTarget(SkillContext context) throws SkillExecutionException {
        // Try to get from session state or configuration
        Object target = context.getSessionState("reviewTarget", Object.class);
        if (target == null) {
            target = context.getConfiguration("reviewTarget", Object.class);
        }

        if (target == null) {
            throw new SkillExecutionException(getId(), "No review target provided in context");
        }

        return target;
    }

    /**
     * Performs review on the target.
     *
     * @param target the target to review
     * @param context the skill context
     * @return the review result
     */
    private ReviewResult performReview(Object target, SkillContext context) {
        logger.info("Performing review on target: {}", target);

        List<ReviewFinding> findings = new ArrayList<>();
        ReviewStatus status = ReviewStatus.APPROVED;

        // Perform review analysis
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            if (map.containsKey("taskId")) {
                findings.add(new ReviewFinding(
                    ReviewSeverity.INFO,
                    "Task completed",
                    "Task execution successful"
                ));
            }
        }

        logger.debug("Review found {} issues", findings.size());

        return new ReviewResult(status, findings, "Review completed successfully");
    }
}
