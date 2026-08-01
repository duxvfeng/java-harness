package com.chachamaru.harness.collaboration.agent.impl;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reviewer agent for code review and quality validation.
 *
 * <p>The ReviewerAgent is responsible for:
 * <ul>
 *   <li>Reviewing code changes</li>
 *   <li>Checking code quality standards</li>
 *   <li>Identifying potential issues</li>
 *   <li>Providing improvement suggestions</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class ReviewerAgent implements Agent {

    private static final Logger logger = LoggerFactory.getLogger(ReviewerAgent.class);

    private final String id;
    private final String name;
    private boolean crossModel = false;
    private double temperature = 0.2;

    /**
     * Creates a ReviewerAgent with default settings.
     */
    public ReviewerAgent() {
        this("reviewer-default", "Default Reviewer Agent");
    }

    /**
     * Creates a ReviewerAgent with custom ID and name.
     *
     * @param id the agent ID
     * @param name the agent name
     */
    public ReviewerAgent(String id, String name) {
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
        return "Agent for reviewing code and validating quality standards";
    }

    @Override
    public AgentType getType() {
        return AgentType.REVIEWER;
    }

    @Override
    public AgentResult execute(AgentContext context) throws AgentExecutionException {
        logger.info("ReviewerAgent {} starting review", id);

        try {
            // Get review target from context
            Object target = getReviewTarget(context);

            // Perform review
            ReviewReport report = performReview(target, context);

            logger.info("Review completed: {} findings", report.findings().size());

            // Determine verdict
            AgentResult result;
            if (report.hasCriticalIssues()) {
                result = AgentResult.failure(id, "Review failed: critical issues found", context.executionStartTime());
            } else {
                result = AgentResult.success(id, report, "Review completed successfully", context.executionStartTime());
            }

            return result;

        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentExecutionException(id, "Unexpected error in reviewer agent: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canExecute(AgentContext context) {
        // Reviewer can execute if review target is available
        return getReviewTargetFromContext(context) != null;
    }

    @Override
    public void initialize() throws AgentExecutionException {
        logger.info("ReviewerAgent {} initialized (crossModel: {}, temperature: {})", id, crossModel, temperature);
    }

    @Override
    public void shutdown() throws AgentExecutionException {
        logger.info("ReviewerAgent {} shut down", id);
    }

    /**
     * Gets the review target from context.
     *
     * @param context the agent context
     * @return the target to review
     * @throws AgentExecutionException if target not available
     */
    private Object getReviewTarget(AgentContext context) throws AgentExecutionException {
        Object target = getReviewTargetFromContext(context);
        if (target == null) {
            throw new AgentExecutionException(id, "No review target provided in context");
        }
        return target;
    }

    /**
     * Gets the review target from context (without throwing).
     *
     * @param context the agent context
     * @return the target to review, or null if not available
     */
    private Object getReviewTargetFromContext(AgentContext context) {
        // Try to get from session state or configuration
        Object target = context.getSessionState("reviewTarget", Object.class);
        if (target == null) {
            target = context.getConfiguration("reviewTarget", Object.class);
        }
        return target;
    }

    /**
     * Performs review on the target.
     *
     * @param target the target to review
     * @param context the agent context
     * @return the review report
     */
    private ReviewReport performReview(Object target, AgentContext context) {
        logger.info("Performing review on target: {}", target);

        List<ReviewFinding> findings = new ArrayList<>();

        // Perform review analysis
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;

            // Check for common issues
            if (map.containsKey("code")) {
                findings.addAll(reviewCode(map.get("code").toString()));
            }

            if (map.containsKey("changes")) {
                findings.addAll(reviewChanges(map.get("changes")));
            }

            // Always add a summary finding
            findings.add(new ReviewFinding(
                ReviewSeverity.INFO,
                "Review Summary",
                "Review completed using temperature: " + temperature
            ));
        }

        return new ReviewReport(findings, "Review completed");
    }

    /**
     * Reviews code for potential issues.
     *
     * @param code the code to review
     * @return list of findings
     */
    private List<ReviewFinding> reviewCode(String code) {
        List<ReviewFinding> findings = new ArrayList();

        // Placeholder: In real implementation, this would:
        // 1. Analyze code structure
        // 2. Check for anti-patterns
        // 3. Validate naming conventions
        // 4. Check for security issues
        // 5. Assess code complexity

        if (code.length() < 50) {
            findings.add(new ReviewFinding(
                ReviewSeverity.MINOR,
                "Short Code",
                "Code appears to be very short, might need more context"
            ));
        }

        return findings;
    }

    /**
     * Reviews changes for potential issues.
     *
     * @param changes the changes to review
     * @return list of findings
     */
    @SuppressWarnings("unchecked")
    private List<ReviewFinding> reviewChanges(Object changes) {
        List<ReviewFinding> findings = new ArrayList();

        // Placeholder: In real implementation, this would:
        // 1. Analyze change impact
        // 2. Check for breaking changes
        // 3. Validate test coverage
        // 4. Review documentation updates

        if (changes instanceof List) {
            List<?> changeList = (List<?>) changes;
            findings.add(new ReviewFinding(
                ReviewSeverity.INFO,
                "Changes Reviewed",
                "Reviewed " + changeList.size() + " changes"
            ));
        }

        return findings;
    }

    /**
     * Gets whether cross-model review is enabled.
     *
     * @return true if cross-model review is enabled
     */
    public boolean isCrossModel() {
        return crossModel;
    }

    /**
     * Sets whether cross-model review is enabled.
     *
     * @param crossModel true to enable cross-model review
     */
    public void setCrossModel(boolean crossModel) {
        this.crossModel = crossModel;
    }

    /**
     * Gets the review temperature.
     *
     * @return the temperature
     */
    public double getTemperature() {
        return temperature;
    }

    /**
     * Sets the review temperature.
     *
     * @param temperature the temperature to set (0.0 to 1.0)
     */
    public void setTemperature(double temperature) {
        this.temperature = Math.max(0.0, Math.min(1.0, temperature));
    }

    /**
     * Review report.
     */
    public record ReviewReport(
        List<ReviewFinding> findings,
        String summary
    ) {
        public boolean hasCriticalIssues() {
            return findings.stream().anyMatch(f -> f.severity() == ReviewSeverity.CRITICAL);
        }

        public long getCriticalCount() {
            return findings.stream().filter(f -> f.severity() == ReviewSeverity.CRITICAL).count();
        }

        public long getMajorCount() {
            return findings.stream().filter(f -> f.severity() == ReviewSeverity.MAJOR).count();
        }
    }

    /**
     * Review finding.
     */
    public record ReviewFinding(
        ReviewSeverity severity,
        String title,
        String description
    ) {
    }

    /**
     * Review severity enumeration.
     */
    public enum ReviewSeverity {
        /** Critical issue that must be fixed */
        CRITICAL,
        /** Major issue that should be fixed */
        MAJOR,
        /** Minor issue or suggestion */
        MINOR,
        /** Informational note */
        INFO
    }
}
