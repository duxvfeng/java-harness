package com.chachamaru.harness.workflow.recovery;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Four-phase recovery coordinator for workflow sessions.
 *
 * <p>Implements the complete 4-phase recovery mechanism:
 * <ol>
 *   <li>Self-healing: Automatic retry with exponential backoff</li>
 *   <li>Peer recovery: Alternative worker assignment</li>
 *   <li>Lead intervention: Escalation to coordination layer</li>
 *   <li>Abort: Session termination</li>
 * </ol>
 *
 * <p>This coordinator manages the recovery lifecycle, attempting each phase
 * sequentially until recovery succeeds or the session is aborted.</p>
 *
 * @spec_reference spec.md#Workflow System - State Recovery
 */
public class FourPhaseRecovery implements StateRecovery {

    /** Recovery strategies for each phase */
    private final Map<RecoveryResult.RecoveryPhase, RecoveryStrategy> strategies;

    /** Session recovery tracking */
    private final Map<String, SessionRecoveryState> sessionStates;

    /** Default strategies */
    private final SelfHealingStrategy selfHealingStrategy;
    private final PeerRecoveryStrategy peerRecoveryStrategy;
    private final LeadInterventionStrategy leadInterventionStrategy;
    private final AbortStrategy abortStrategy;

    /**
     * Creates a four-phase recovery coordinator with default strategies.
     */
    public FourPhaseRecovery() {
        this.selfHealingStrategy = new SelfHealingStrategy();
        this.peerRecoveryStrategy = new PeerRecoveryStrategy();
        this.leadInterventionStrategy = new LeadInterventionStrategy();
        this.abortStrategy = new AbortStrategy();

        this.strategies = new LinkedHashMap<>();
        strategies.put(RecoveryResult.RecoveryPhase.SELF_HEALING, selfHealingStrategy);
        strategies.put(RecoveryResult.RecoveryPhase.PEER_RECOVERY, peerRecoveryStrategy);
        strategies.put(RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, leadInterventionStrategy);
        strategies.put(RecoveryResult.RecoveryPhase.ABORT, abortStrategy);

        this.sessionStates = new ConcurrentHashMap<>();
    }

    /**
     * Creates a four-phase recovery coordinator with custom strategies.
     *
     * @param selfHealing Self-healing strategy
     * @param peerRecovery Peer recovery strategy
     * @param leadIntervention Lead intervention strategy
     * @param abort Abort strategy
     */
    public FourPhaseRecovery(
            SelfHealingStrategy selfHealing,
            PeerRecoveryStrategy peerRecovery,
            LeadInterventionStrategy leadIntervention,
            AbortStrategy abort) {
        this.selfHealingStrategy = selfHealing;
        this.peerRecoveryStrategy = peerRecovery;
        this.leadInterventionStrategy = leadIntervention;
        this.abortStrategy = abort;

        this.strategies = new LinkedHashMap<>();
        strategies.put(RecoveryResult.RecoveryPhase.SELF_HEALING, selfHealing);
        strategies.put(RecoveryResult.RecoveryPhase.PEER_RECOVERY, peerRecovery);
        strategies.put(RecoveryResult.RecoveryPhase.LEAD_INTERVENTION, leadIntervention);
        strategies.put(RecoveryResult.RecoveryPhase.ABORT, abort);

        this.sessionStates = new ConcurrentHashMap<>();
    }

    @Override
    public RecoveryResult attemptRecovery(String sessionId) throws RecoveryException {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }

        // Check if session is already aborted
        if (abortStrategy.isAborted(sessionId)) {
            AbortStrategy.AbortRecord record = abortStrategy.getAbortRecord(sessionId);
            return RecoveryResult.aborted(
                sessionId,
                String.format("Session was aborted: %s", record.abortReason())
            );
        }

        // Get or create session state
        SessionRecoveryState state = sessionStates.computeIfAbsent(
            sessionId,
            k -> new SessionRecoveryState(sessionId)
        );

        // Start recovery from current phase
        return attemptPhaseRecovery(sessionId, state);
    }

    @Override
    public RecoveryResult attemptSelfHealing(String sessionId) throws RecoveryException {
        return executeStrategy(sessionId, RecoveryResult.RecoveryPhase.SELF_HEALING);
    }

    @Override
    public RecoveryResult attemptPeerRecovery(String sessionId) throws RecoveryException {
        return executeStrategy(sessionId, RecoveryResult.RecoveryPhase.PEER_RECOVERY);
    }

    @Override
    public RecoveryResult attemptLeadIntervention(String sessionId) throws RecoveryException {
        return executeStrategy(sessionId, RecoveryResult.RecoveryPhase.LEAD_INTERVENTION);
    }

    @Override
    public void markAborted(String sessionId) throws RecoveryException {
        try {
            RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
                sessionId,
                new RuntimeException("Session aborted by request"),
                "AbortRequest",
                RecoveryResult.RecoveryPhase.ABORT,
                0,
                java.time.LocalDateTime.now(),
                null
            );

            abortStrategy.recover(sessionId, context);
            sessionStates.remove(sessionId);
        } catch (RecoveryStrategy.RecoveryException e) {
            throw new RecoveryException(
                "Failed to mark session as aborted: " + e.getMessage(),
                e,
                sessionId
            );
        }
    }

    @Override
    public boolean isRecoverable(String sessionId) {
        return !abortStrategy.isAborted(sessionId);
    }

    @Override
    public RecoveryResult.RecoveryPhase getCurrentPhase(String sessionId) {
        SessionRecoveryState state = sessionStates.get(sessionId);
        return state != null ? state.currentPhase() : null;
    }

    @Override
    public int getRecoveryAttempts(String sessionId) {
        SessionRecoveryState state = sessionStates.get(sessionId);
        return state != null ? state.totalAttempts() : 0;
    }

    /**
     * Attempts recovery for the current phase of the session.
     *
     * @param sessionId Session to recover
     * @param state Session recovery state
     * @return Recovery result
     * @throws RecoveryException if recovery fails catastrophically
     */
    private RecoveryResult attemptPhaseRecovery(String sessionId, SessionRecoveryState state)
            throws RecoveryException {
        RecoveryResult.RecoveryPhase currentPhase = state.currentPhase();
        RecoveryStrategy strategy = strategies.get(currentPhase);

        if (strategy == null) {
            throw new RecoveryException(
                "No strategy found for phase: " + currentPhase,
                sessionId
            );
        }

        // Execute the strategy
        RecoveryResult result = executeStrategy(sessionId, currentPhase);

        // Update session state
        state.recordAttempt(result);

        // Handle result
        if (result.isSuccess()) {
            // Recovery successful, clean up session state
            sessionStates.remove(sessionId);
            return result;
        }

        // Check next action
        if (result.shouldEscalate()) {
            // Move to next phase
            RecoveryResult.RecoveryPhase nextPhase = getNextPhase(currentPhase);
            if (nextPhase != null) {
                state.setCurrentPhase(nextPhase);
                return attemptPhaseRecovery(sessionId, state);
            } else {
                // No more phases, abort
                markAborted(sessionId);
                return RecoveryResult.aborted(sessionId, "All recovery phases exhausted");
            }
        }

        // Retry same phase or return failure
        return result;
    }

    /**
     * Executes a specific recovery strategy.
     *
     * @param sessionId Session to recover
     * @param phase Recovery phase to execute
     * @return Recovery result
     * @throws RecoveryException if execution fails
     */
    private RecoveryResult executeStrategy(String sessionId, RecoveryResult.RecoveryPhase phase)
            throws RecoveryException {
        RecoveryStrategy strategy = strategies.get(phase);
        if (strategy == null) {
            throw new RecoveryException("No strategy for phase: " + phase, sessionId);
        }

        SessionRecoveryState state = sessionStates.get(sessionId);
        if (state == null) {
            state = new SessionRecoveryState(sessionId);
            sessionStates.put(sessionId, state);
        }

        RecoveryStrategy.RecoveryContext context = new RecoveryStrategy.RecoveryContext(
            sessionId,
            state.lastError(),
            state.lastErrorType(),
            phase,
            state.getPhaseAttempts(phase),
            java.time.LocalDateTime.now(),
            state.stateSnapshot()
        );

        try {
            RecoveryResult result = strategy.recover(sessionId, context);
            // Record attempt in session state
            state.recordAttempt(result);
            return result;
        } catch (RecoveryStrategy.RecoveryException e) {
            throw new RecoveryException(
                "Strategy execution failed: " + e.getMessage(),
                e,
                sessionId
            );
        }
    }

    /**
     * Gets the next recovery phase after the given phase.
     *
     * @param currentPhase Current phase
     * @return Next phase, or null if current is the last phase
     */
    private RecoveryResult.RecoveryPhase getNextPhase(RecoveryResult.RecoveryPhase currentPhase) {
        return switch (currentPhase) {
            case SELF_HEALING -> RecoveryResult.RecoveryPhase.PEER_RECOVERY;
            case PEER_RECOVERY -> RecoveryResult.RecoveryPhase.LEAD_INTERVENTION;
            case LEAD_INTERVENTION -> RecoveryResult.RecoveryPhase.ABORT;
            case ABORT -> null; // Last phase, no next
        };
    }

    /**
     * Clears session recovery state (for testing or administrative purposes).
     *
     * @param sessionId Session ID to clear
     */
    public void clearSessionState(String sessionId) {
        sessionStates.remove(sessionId);
    }

    /**
     * Clears all session states (for testing purposes).
     */
    public void clearAllSessionStates() {
        sessionStates.clear();
    }

    /**
     * Gets the count of active recovery sessions.
     *
     * @return Number of sessions in recovery
     */
    public int getActiveRecoveryCount() {
        return sessionStates.size();
    }

    /**
     * Session recovery state tracking.
     */
    private static class SessionRecoveryState {
        private final String sessionId;
        private RecoveryResult.RecoveryPhase currentPhase;
        private int totalAttempts;
        private Throwable lastError;
        private String lastErrorType;
        private final Map<RecoveryResult.RecoveryPhase, Integer> phaseAttempts;
        private Object stateSnapshot;

        SessionRecoveryState(String sessionId) {
            this.sessionId = sessionId;
            this.currentPhase = RecoveryResult.RecoveryPhase.SELF_HEALING;
            this.totalAttempts = 0;
            this.phaseAttempts = new HashMap<>();
            // Initialize all phases with 0 attempts
            for (RecoveryResult.RecoveryPhase phase : RecoveryResult.RecoveryPhase.values()) {
                phaseAttempts.put(phase, 0);
            }
        }

        RecoveryResult.RecoveryPhase currentPhase() {
            return currentPhase;
        }

        void setCurrentPhase(RecoveryResult.RecoveryPhase phase) {
            this.currentPhase = phase;
        }

        int totalAttempts() {
            return totalAttempts;
        }

        Throwable lastError() {
            return lastError;
        }

        String lastErrorType() {
            return lastErrorType;
        }

        Object stateSnapshot() {
            return stateSnapshot;
        }

        void setStateSnapshot(Object snapshot) {
            this.stateSnapshot = snapshot;
        }

        int getPhaseAttempts(RecoveryResult.RecoveryPhase phase) {
            return phaseAttempts.getOrDefault(phase, 0);
        }

        void recordAttempt(RecoveryResult result) {
            totalAttempts++;
            phaseAttempts.put(result.phase(), phaseAttempts.get(result.phase()) + 1);
            // Update error info if available
            if (result.status() == RecoveryResult.RecoveryStatus.FAILED) {
                // In a real implementation, would extract error from context
                this.lastErrorType = "RecoveryFailed";
            }
        }
    }
}
