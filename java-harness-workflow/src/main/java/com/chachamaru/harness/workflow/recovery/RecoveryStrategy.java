package com.chachamaru.harness.workflow.recovery;

/**
 * State recovery strategy interface.
 *
 * <p>Defines the contract for implementing different recovery strategies
 * in the 4-phase recovery mechanism:
 * <ol>
 *   <li>Self-healing: Automatic retries and configuration adjustments</li>
 *   <li>Peer recovery: Assignment to alternative workers</li>
 *   <li>Lead intervention: Escalation to coordination layer</li>
 *   <li>Abort: Session termination</li>
 * </ol>
 *
 * @spec_reference spec.md#Workflow System
 */
public interface RecoveryStrategy {

    /**
     * Gets the recovery phase this strategy handles.
     *
     * @return The recovery phase
     */
    RecoveryResult.RecoveryPhase getPhase();

    /**
     * Gets the maximum number of recovery attempts for this phase.
     *
     * @return Maximum attempts (typically 3 for self-healing, 1 for others)
     */
    int getMaxAttempts();

    /**
     * Checks if this strategy can handle the given error type.
     *
     * @param errorType The error class or category
     * @return true if this strategy can recover from the error
     */
    boolean canHandle(String errorType);

    /**
     * Checks if this strategy can handle the given error.
     *
     * @param error The error that occurred
     * @return true if this strategy can recover from the error
     */
    default boolean canHandle(Throwable error) {
        return canHandle(error.getClass().getSimpleName());
    }

    /**
     * Executes the recovery strategy.
     *
     * @param sessionId The session to recover
     * @param context Recovery context (error, current state, etc.)
     * @return Recovery result with status and next action
     * @throws RecoveryException if recovery operation fails
     */
    RecoveryResult recover(String sessionId, RecoveryContext context) throws RecoveryException;

    /**
     * Context for recovery operations.
     */
    record RecoveryContext(
        String sessionId,
        Throwable error,
        String errorType,
        RecoveryResult.RecoveryPhase currentPhase,
        int previousAttempts,
        java.time.LocalDateTime errorTime,
        Object stateSnapshot
    ) {
        /**
         * Creates a recovery context.
         */
        public RecoveryContext {
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("sessionId cannot be null or blank");
            }
            if (errorType == null) {
                errorType = error != null ? error.getClass().getSimpleName() : "UNKNOWN";
            }
            if (errorTime == null) {
                errorTime = java.time.LocalDateTime.now();
            }
        }
    }

    /**
     * Exception thrown during recovery operations.
     */
    class RecoveryException extends Exception {
        private final String sessionId;
        private final RecoveryResult.RecoveryPhase phase;

        public RecoveryException(String message, String sessionId, RecoveryResult.RecoveryPhase phase) {
            super(message);
            this.sessionId = sessionId;
            this.phase = phase;
        }

        public RecoveryException(String message, Throwable cause, String sessionId, RecoveryResult.RecoveryPhase phase) {
            super(message, cause);
            this.sessionId = sessionId;
            this.phase = phase;
        }

        public String getSessionId() {
            return sessionId;
        }

        public RecoveryResult.RecoveryPhase getPhase() {
            return phase;
        }
    }
}
