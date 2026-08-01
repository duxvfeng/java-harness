package com.chachamaru.harness.workflow.recovery;

import java.time.LocalDateTime;

/**
 * Result of a state recovery attempt.
 *
 * <p>Contains information about recovery attempts, outcomes,
 * and next steps in the recovery pipeline.</p>
 *
 * @spec_reference spec.md#Workflow System
 */
public record RecoveryResult(
    String sessionId,
    RecoveryStatus status,
    RecoveryPhase phase,
    String message,
    LocalDateTime recoveredAt,
    int attemptsMade,
    RecoveryAction nextAction
) {
    /**
     * Status of the recovery operation.
     */
    public enum RecoveryStatus {
        /** Recovery succeeded */
        SUCCESS,

        /** Recovery failed, should retry or escalate */
        FAILED,

        /** Recovery in progress */
        IN_PROGRESS,

        /** Recovery aborted, session beyond recovery */
        ABORTED
    }

    /**
     * Phase of recovery that was attempted.
     */
    public enum RecoveryPhase {
        /** Self-healing: Task retries, timeout adjustments */
        SELF_HEALING,

        /** Peer recovery: Alternative worker assignment */
        PEER_RECOVERY,

        /** Lead intervention: Escalation to human or higher authority */
        LEAD_INTERVENTION,

        /** Abort: Session marked as unrecoverable */
        ABORT
    }

    /**
     * Recommended next action after this recovery attempt.
     */
    public enum RecoveryAction {
        /** No action needed, recovery complete */
        NONE,

        /** Retry the same recovery phase */
        RETRY,

        /** Escalate to next recovery phase */
        ESCALATE,

        /** Abort the session */
        ABORT,

        /** Request human intervention */
        REQUEST_INTERVENTION
    }

    /**
     * Creates a recovery result.
     */
    public RecoveryResult {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }
        if (status == null) {
            status = RecoveryStatus.FAILED;
        }
        if (phase == null) {
            phase = RecoveryPhase.SELF_HEALING;
        }
        if (attemptsMade < 0) {
            throw new IllegalArgumentException("attemptsMade cannot be negative");
        }
        if (nextAction == null) {
            nextAction = RecoveryAction.ESCALATE;
        }
    }

    /**
     * Creates a successful recovery result.
     */
    public static RecoveryResult success(String sessionId, RecoveryPhase phase, int attemptsMade) {
        return new RecoveryResult(
            sessionId,
            RecoveryStatus.SUCCESS,
            phase,
            "Recovery successful",
            LocalDateTime.now(),
            attemptsMade,
            RecoveryAction.NONE
        );
    }

    /**
     * Creates a failed recovery result.
     */
    public static RecoveryResult failure(String sessionId, RecoveryPhase phase, String reason, int attemptsMade) {
        return new RecoveryResult(
            sessionId,
            RecoveryStatus.FAILED,
            phase,
            reason,
            LocalDateTime.now(),
            attemptsMade,
            RecoveryAction.ESCALATE
        );
    }

    /**
     * Creates an aborted recovery result.
     */
    public static RecoveryResult aborted(String sessionId, String reason) {
        return new RecoveryResult(
            sessionId,
            RecoveryStatus.ABORTED,
            RecoveryPhase.ABORT,
            reason,
            LocalDateTime.now(),
            0,
            RecoveryAction.NONE
        );
    }

    /**
     * Checks if recovery was successful.
     */
    public boolean isSuccess() {
        return status == RecoveryStatus.SUCCESS;
    }

    /**
     * Checks if recovery should be retried.
     */
    public boolean shouldRetry() {
        return nextAction == RecoveryAction.RETRY;
    }

    /**
     * Checks if recovery should escalate to next phase.
     */
    public boolean shouldEscalate() {
        return nextAction == RecoveryAction.ESCALATE;
    }
}
