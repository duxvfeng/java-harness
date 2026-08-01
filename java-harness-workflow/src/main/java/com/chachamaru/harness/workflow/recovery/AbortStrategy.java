package com.chachamaru.harness.workflow.recovery;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abort strategy (Phase 4).
 *
 * <p>Handles permanent session termination when recovery is not possible:
 * <ul>
 *   <li>Marks session as permanently failed (ABORTED status)</li>
 *   <li>Records abort reason and metadata</li>
 *   <li>Cleans up session resources</li>
 *   <li>Prevents further recovery attempts</li>
 * </ul>
 *
 * <p>This is the final phase in the 4-phase recovery mechanism and indicates
 * that the session cannot be recovered through any available means.</p>
 *
 * @spec_reference spec.md#Workflow System - State Recovery
 */
public class AbortStrategy implements RecoveryStrategy {

    /** Maximum attempts for abort phase (should always be 0) */
    private static final int MAX_ATTEMPTS = 0;

    /** Registry of aborted sessions for audit and tracking */
    private final Map<String, AbortRecord> abortedSessions;

    /**
     * Creates an abort strategy.
     */
    public AbortStrategy() {
        this.abortedSessions = new ConcurrentHashMap<>();
    }

    @Override
    public RecoveryResult.RecoveryPhase getPhase() {
        return RecoveryResult.RecoveryPhase.ABORT;
    }

    @Override
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    @Override
    public boolean canHandle(String errorType) {
        // Abort strategy can handle any error type as it's the final fallback
        return true;
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

        // Record abort information
        AbortRecord record = new AbortRecord(
            sessionId,
            context.errorType(),
            context.error() != null ? context.error().getMessage() : "Unknown error",
            LocalDateTime.now(),
            context.currentPhase(),
            context.previousAttempts()
        );

        abortedSessions.put(sessionId, record);

        System.err.printf(
            "[AbortStrategy] Session %s marked as ABORTED (reason: %s, attempts: %d)%n",
            sessionId, record.abortReason(), context.previousAttempts()
        );

        // Return aborted result
        return RecoveryResult.aborted(
            sessionId,
            String.format("Session aborted after %d recovery attempts: %s",
                         context.previousAttempts(), record.abortReason())
        );
    }

    /**
     * Checks if a session has been aborted.
     *
     * @param sessionId Session ID to check
     * @return true if session is aborted
     */
    public boolean isAborted(String sessionId) {
        return abortedSessions.containsKey(sessionId);
    }

    /**
     * Gets the abort record for a session.
     *
     * @param sessionId Session ID
     * @return Abort record, or null if session not aborted
     */
    public AbortRecord getAbortRecord(String sessionId) {
        return abortedSessions.get(sessionId);
    }

    /**
     * Gets all aborted sessions.
     *
     * @return Map of session ID to abort records
     */
    public Map<String, AbortRecord> getAbortedSessions() {
        return Map.copyOf(abortedSessions);
    }

    /**
     * Gets the count of aborted sessions.
     *
     * @return Number of aborted sessions
     */
    public int getAbortedSessionCount() {
        return abortedSessions.size();
    }

    /**
     * Clears an abort record (for testing or administrative purposes).
     *
     * @param sessionId Session ID to clear
     * @return true if record was cleared
     */
    public boolean clearAbortRecord(String sessionId) {
        return abortedSessions.remove(sessionId) != null;
    }

    /**
     * Clears all abort records (for testing or administrative purposes).
     */
    public void clearAllAbortRecords() {
        abortedSessions.clear();
    }

    /**
     * Record of a session abortion.
     */
    public record AbortRecord(
        String sessionId,
        String errorType,
        String abortReason,
        LocalDateTime abortedAt,
        RecoveryResult.RecoveryPhase lastPhase,
        int totalAttempts
    ) {
        public AbortRecord {
            if (sessionId == null || sessionId.isBlank()) {
                throw new IllegalArgumentException("sessionId cannot be null or blank");
            }
            if (abortReason == null || abortReason.isBlank()) {
                abortReason = "Unknown reason";
            }
            if (abortedAt == null) {
                abortedAt = LocalDateTime.now();
            }
        }
    }
}
