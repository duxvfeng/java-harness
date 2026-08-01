package com.chachamaru.harness.workflow.recovery;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Lead intervention recovery strategy (Phase 3).
 *
 * <p>Handles recovery by escalating to human coordination or higher-level intervention:
 * <ul>
 *   <li>Escalating to human coordinator for manual intervention</li>
 *   <li>Session state analysis and correction</li>
 *   <li>Workflow adjustment and reconfiguration</li>
 * </ul>
 *
 * <p>This strategy is invoked when peer recovery fails, indicating the issue
 * may require human judgment or workflow-level changes.
 *
 * @spec_reference spec.md#Workflow System - State Recovery
 */
public class LeadInterventionStrategy implements RecoveryStrategy {

    /** Maximum number of lead intervention attempts */
    private static final int MAX_ATTEMPTS = 1;

    /** Error types this strategy can handle (critical failures) */
    private static final Set<String> HANDLABLE_ERRORS = Set.of(
        "WorkflowException",
        "CoordinationException",
        "CriticalStateException",
        "UnrecoverableException",
        "ConfigurationMismatchException",
        "SchemaValidationException"
    );

    /** Intervention request handler for external coordination */
    private InterventionHandler interventionHandler;

    /**
     * Creates a lead intervention strategy.
     */
    public LeadInterventionStrategy() {
        this.interventionHandler = null; // Default: no external handler
    }

    /**
     * Creates a lead intervention strategy with custom intervention handler.
     *
     * @param handler Custom intervention handler
     */
    public LeadInterventionStrategy(InterventionHandler handler) {
        this.interventionHandler = handler;
    }

    @Override
    public RecoveryResult.RecoveryPhase getPhase() {
        return RecoveryResult.RecoveryPhase.LEAD_INTERVENTION;
    }

    @Override
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    @Override
    public boolean canHandle(String errorType) {
        return HANDLABLE_ERRORS.contains(errorType) ||
               errorType.contains("Workflow") ||
               errorType.contains("Coordination") ||
               errorType.contains("Critical");
    }

    @Override
    public RecoveryResult recover(String sessionId, RecoveryContext context)
            throws RecoveryException {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("context cannot be null");
        }

        System.out.printf(
            "[LeadIntervention] Escalating session %s to human coordinator (error: %s)%n",
            sessionId, context.errorType()
        );

        // Log the escalation
        logInterventionRequest(sessionId, context);

        // If intervention handler is available, delegate to it
        if (interventionHandler != null) {
            try {
                InterventionResult result = interventionHandler.handleIntervention(
                    sessionId,
                    context.error(),
                    context.stateSnapshot()
                );

                if (result.resolved()) {
                    return RecoveryResult.success(
                        sessionId,
                        getPhase(),
                        1
                    );
                } else {
                    return RecoveryResult.failure(
                        sessionId,
                        getPhase(),
                        String.format("Intervention failed: %s", result.reason()),
                        1
                    );
                }
            } catch (Exception e) {
                return RecoveryResult.failure(
                    sessionId,
                    getPhase(),
                    String.format("Intervention handler error: %s", e.getMessage()),
                    1
                );
            }
        }

        // Default behavior: request human intervention
        return RecoveryResult.failure(
            sessionId,
            getPhase(),
            "Escalated to human coordinator - awaiting manual intervention",
            1
        );
    }

    /**
     * Logs the intervention request for audit and tracking.
     *
     * @param sessionId Session ID
     * @param context Recovery context
     */
    private void logInterventionRequest(String sessionId, RecoveryContext context) {
        System.err.printf(
            "[INTERVENTION_REQUEST] Session: %s | Error: %s | Time: %s%n",
            sessionId,
            context.errorType(),
            LocalDateTime.now()
        );
    }

    /**
     * Sets the intervention handler.
     *
     * @param handler Intervention handler to use
     */
    public void setInterventionHandler(InterventionHandler handler) {
        this.interventionHandler = handler;
    }

    /**
     * Checks if an intervention handler is configured.
     *
     * @return true if handler is available
     */
    public boolean hasInterventionHandler() {
        return interventionHandler != null;
    }

    /**
     * Interface for handling intervention requests.
     */
    public interface InterventionHandler {
        /**
         * Handles an intervention request.
         *
         * @param sessionId Session requiring intervention
         * @param error Error that triggered intervention
         * @param stateSnapshot Current session state
         * @return Intervention result
         */
        InterventionResult handleIntervention(String sessionId, Throwable error, Object stateSnapshot);
    }

    /**
     * Result of an intervention attempt.
     */
    public record InterventionResult(
        boolean resolved,
        String reason,
        String resolutionDetails
    ) {
        /**
         * Creates a successful intervention result.
         */
        public static InterventionResult success(String details) {
            return new InterventionResult(true, null, details);
        }

        /**
         * Creates a failed intervention result.
         */
        public static InterventionResult failure(String reason) {
            return new InterventionResult(false, reason, null);
        }
    }
}
