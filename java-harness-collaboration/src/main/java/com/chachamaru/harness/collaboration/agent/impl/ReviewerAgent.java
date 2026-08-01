package com.chachamaru.harness.collaboration.agent.impl;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.message.ReviewResultV1;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
            ReviewResultV1 reviewResult = performReview(target, context);

            logger.info("Review completed: {} findings (verdict: {})",
                reviewResult.findings().size(), reviewResult.verdict());

            // Determine agent result based on verdict
            AgentResult result;
            if (reviewResult.isApproved()) {
                result = AgentResult.success(id, reviewResult, "Review approved", context.executionStartTime());
            } else {
                result = AgentResult.failure(id, "Review requested changes: " + reviewResult.summary(), context.executionStartTime());
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
     * @return the review result
     */
    private ReviewResultV1 performReview(Object target, AgentContext context) {
        logger.info("Performing review on target: {}", target);

        List<ReviewResultV1.ReviewFinding> findings = new ArrayList<>();

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

            // Check for review findings from context
            Object mustAddressObj = context.getSessionState("mustAddress", Object.class);
            if (mustAddressObj != null) {
                findings.addAll(reviewMustAddressItems(mustAddressObj));
            }
        }

        // Determine verdict based on findings
        long criticalCount = findings.stream().filter(f -> f.getSeverityEnum() == ReviewResultV1.ReviewSeverity.CRITICAL).count();
        long majorCount = findings.stream().filter(f -> f.getSeverityEnum() == ReviewResultV1.ReviewSeverity.MAJOR).count();

        String verdict;
        String summary;

        if (criticalCount > 0 || majorCount > 0) {
            verdict = ReviewResultV1.Verdict.REQUEST_CHANGES.name();
            summary = String.format("Found %d critical and %d major issues that must be addressed",
                criticalCount, majorCount);
        } else {
            verdict = ReviewResultV1.Verdict.APPROVE.name();
            summary = "Review approved - no critical or major issues found";
        }

        String requestId = UUID.randomUUID().toString();

        return new ReviewResultV1(
            requestId,
            verdict,
            findings,
            summary,
            (int) criticalCount,
            (int) majorCount,
            Map.of(
                "reviewerId", id,
                "temperature", temperature,
                "crossModel", crossModel
            ),
            java.time.Instant.now()
        );
    }

    /**
     * Reviews code for potential issues.
     *
     * @param code the code to review
     * @return list of findings
     */
    private List<ReviewResultV1.ReviewFinding> reviewCode(String code) {
        List<ReviewResultV1.ReviewFinding> findings = new ArrayList<>();

        // Check code length
        if (code.length() < 50) {
            findings.add(new ReviewResultV1.ReviewFinding(
                "code-quality",
                "MINOR",
                "Short Code",
                "Code appears to be very short, might need more context or implementation",
                null,
                0,
                "Code length < 50 characters"
            ));
        }

        // Check for TODO comments
        if (code.toLowerCase().contains("todo")) {
            findings.add(new ReviewResultV1.ReviewFinding(
                "code-quality",
                "MAJOR",
                "TODO Comments Found",
                "Code contains TODO comments that should be addressed",
                null,
                code.indexOf("TODO"),
                "TODO comments indicate incomplete work"
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
    private List<ReviewResultV1.ReviewFinding> reviewChanges(Object changes) {
        List<ReviewResultV1.ReviewFinding> findings = new ArrayList<>();

        if (changes instanceof List) {
            List<?> changeList = (List<?>) changes;

            findings.add(new ReviewResultV1.ReviewFinding(
                "changes",
                "INFO",
                "Changes Reviewed",
                String.format("Reviewed %d changes", changeList.size()),
                null,
                0,
                "Change count: " + changeList.size()
            ));
        }

        return findings;
    }

    /**
     * Reviews must-address items from previous review.
     *
     * @param mustAddress the items that must be addressed
     * @return list of findings
     */
    @SuppressWarnings("unchecked")
    private List<ReviewResultV1.ReviewFinding> reviewMustAddressItems(Object mustAddress) {
        List<ReviewResultV1.ReviewFinding> findings = new ArrayList<>();

        if (mustAddress instanceof List) {
            List<?> items = (List<?>) mustAddress;

            for (Object item : items) {
                if (item instanceof ReviewResultV1.ReviewFinding) {
                    ReviewResultV1.ReviewFinding finding = (ReviewResultV1.ReviewFinding) item;
                    // Check if the same issue persists
                    findings.add(new ReviewResultV1.ReviewFinding(
                        "persistent-issue",
                        finding.severity(),
                        "Persistent Issue: " + finding.title(),
                        finding.description() + " - This issue was not properly addressed",
                        finding.file(),
                        finding.line(),
                        finding.failureScenario()
                    ));
                }
            }
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
}
