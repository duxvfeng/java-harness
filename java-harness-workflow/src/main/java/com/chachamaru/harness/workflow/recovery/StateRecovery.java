package com.chachamaru.harness.workflow.recovery;

/**
 * State recovery interface for workflow sessions.
 *
 * <p>Manages recovery of failed or interrupted workflow executions
 * through a 4-phase recovery mechanism:
 * <ol>
 *   <li>Self-healing: Automatic retry with exponential backoff</li>
 *   <li>Peer recovery: Alternative worker assignment</li>
 *   <li>Lead intervention: Escalation to coordination layer</li>
 *   <li>Abort: Session termination</li>
 * </ol>
 *
 * <p>Each phase is attempted in order until recovery succeeds
 * or the session is marked as aborted.</p>
 *
 * @spec_reference spec.md#API Contracts
 */
public interface StateRecovery {

    /**
     * Attempts to recover a session using the appropriate phase.
     *
     * <p>Automatically selects the recovery phase based on:
     * <ul>
     *   <li>Error type (timeout, connection, configuration, etc.)</li>
 *   *   <li>Previous recovery attempts</li>
 *   *   <li>Session state and age</li>
 * </ul>
     *
     * @param sessionId The session to recover
     * @return Recovery result with status and recommended next action
     * @throws RecoveryException if recovery fails catastrophically
     */
    RecoveryResult attemptRecovery(String sessionId) throws RecoveryException;

    /**
     * Attempts self-healing recovery (Phase 1).
     *
     * <p>Self-healing strategies include:
     * <ul>
     *   <li>Task retries (up to 3 attempts)</li>
     *   <li>Timeout adjustments</li>
     *   <li>Configuration corrections</li>
     * </ul>
     *
     * @param sessionId The session to recover
     * @return Recovery result
     * @throws RecoveryException if recovery fails
     */
    RecoveryResult attemptSelfHealing(String sessionId) throws RecoveryException;

    /**
     * Attempts peer recovery (Phase 2).
     *
     * <p>Peer recovery strategies include:
     * <ul>
     *   <li>Reassigning to alternative worker</li>
     *   <li>Load balancing across workers</li>
     *   <li>Resource reallocation</li>
     * </ul>
     *
     * @param sessionId The session to recover
     * @return Recovery result
     * @throws RecoveryException if recovery fails
     */
    RecoveryResult attemptPeerRecovery(String sessionId) throws RecoveryException;

    /**
     * Attempts lead intervention (Phase 3).
     *
     * <p>Lead intervention strategies include:
     * <ul>
     *   <li>Escalating to human coordinator</li>
     *   <li>Session state analysis and manual correction</li>
     *   <li>Workflow adjustment</li>
     * </ul>
     *
     * @param sessionId The session to recover
     * @return Recovery result
     * @throws RecoveryException if recovery fails
     */
    RecoveryResult attemptLeadIntervention(String sessionId) throws RecoveryException;

    /**
     * Marks a session as aborted (Phase 4).
     *
     * <p>Aborted sessions cannot be recovered and are marked
     * as permanently failed.</p>
     *
     * @param sessionId The session to abort
     * @throws RecoveryException if abort operation fails
     */
    void markAborted(String sessionId) throws RecoveryException;

    /**
     * Checks if a session is recoverable.
     *
     * @param sessionId The session to check
     * @return true if the session can be recovered
     */
    boolean isRecoverable(String sessionId);

    /**
     * Gets the current recovery phase for a session.
     *
     * @param sessionId The session to check
     * @return Current recovery phase, or null if not in recovery
     */
    RecoveryResult.RecoveryPhase getCurrentPhase(String sessionId);

    /**
     * Gets the number of recovery attempts for a session.
     *
     * @param sessionId The session to check
     * @return Number of attempts made, or 0 if no recovery attempted
     */
    int getRecoveryAttempts(String sessionId);

    /**
     * Exception thrown during state recovery operations.
     */
    class RecoveryException extends Exception {
        private final String sessionId;

        public RecoveryException(String message, String sessionId) {
            super(message);
            this.sessionId = sessionId;
        }

        public RecoveryException(String message, Throwable cause, String sessionId) {
            super(message, cause);
            this.sessionId = sessionId;
        }

        public String getSessionId() {
            return sessionId;
        }
    }
}
