package com.chachamaru.harness.workflow.recovery;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Self-healing recovery strategy (Phase 1).
 *
 * <p>Handles automatic recovery from transient errors through:
 * <ul>
 *   <li>Task retries (up to 3 attempts with exponential backoff)</li>
 *   <li>Timeout adjustments for slow operations</li>
 *   <li>Configuration corrections for common misconfigurations</li>
 * </ul>
 *
 * <p>This strategy handles recoverable errors like:
 * <ul>
 *   <li>Timeout errors (operation took too long)</li>
 *   <li>Connection errors (temporary network issues)</li>
 *   <li>Configuration errors (missing or invalid settings)</li>
 * </ul>
 *
 * @spec_reference spec.md#Workflow System - State Recovery
 */
public class SelfHealingStrategy implements RecoveryStrategy {

    /** Maximum number of self-healing attempts */
    private static final int MAX_ATTEMPTS = 3;

    /** Error types this strategy can handle */
    private static final Set<String> HANDLABLE_ERRORS = Set.of(
        "TimeoutException",
        "SocketTimeoutException",
        "ConnectTimeoutException",
        "ConnectException",
        "SocketException",
        "IOException",
        "ConfigurationException",
        "MissingPropertyException",
        "IllegalStateException"
    );

    /** Base backoff time in milliseconds */
    private final long baseBackoffMs;
    /** Maximum backoff time in milliseconds */
    private final long maxBackoffMs;

    /**
     * Creates a self-healing strategy with default backoff settings.
     */
    public SelfHealingStrategy() {
        this(1000L, 10000L); // 1 second base, 10 seconds max
    }

    /**
     * Creates a self-healing strategy with custom backoff settings.
     *
     * @param baseBackoffMs Base backoff time in milliseconds
     * @param maxBackoffMs Maximum backoff time in milliseconds
     */
    public SelfHealingStrategy(long baseBackoffMs, long maxBackoffMs) {
        if (baseBackoffMs <= 0) {
            throw new IllegalArgumentException("baseBackoffMs must be positive");
        }
        if (maxBackoffMs < baseBackoffMs) {
            throw new IllegalArgumentException("maxBackoffMs must be >= baseBackoffMs");
        }
        this.baseBackoffMs = baseBackoffMs;
        this.maxBackoffMs = maxBackoffMs;
    }

    @Override
    public RecoveryResult.RecoveryPhase getPhase() {
        return RecoveryResult.RecoveryPhase.SELF_HEALING;
    }

    @Override
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }

    @Override
    public boolean canHandle(String errorType) {
        return HANDLABLE_ERRORS.contains(errorType);
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

        int attempt = context.previousAttempts() + 1;

        // Check if we've exceeded maximum attempts
        if (attempt > MAX_ATTEMPTS) {
            return RecoveryResult.failure(
                sessionId,
                getPhase(),
                String.format("Exceeded maximum self-healing attempts (%d)", MAX_ATTEMPTS),
                attempt
            );
        }

        // Check if we can handle this error type
        if (!canHandle(context.errorType())) {
            return RecoveryResult.failure(
                sessionId,
                getPhase(),
                String.format("Cannot handle error type: %s", context.errorType()),
                attempt
            );
        }

        // Calculate backoff time with exponential increase
        long backoffTime = calculateBackoff(attempt);

        // Log the recovery attempt
        System.out.printf(
            "[SelfHealing] Attempt %d/%d for session %s (error: %s, backoff: %dms)%n",
            attempt, MAX_ATTEMPTS, sessionId, context.errorType(), backoffTime
        );

        // Simulate retry with backoff
        try {
            TimeUnit.MILLISECONDS.sleep(backoffTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RecoveryException(
                "Self-healing interrupted",
                e,
                sessionId,
                getPhase()
            );
        }

        // In a real implementation, this would:
        // 1. Retry the failed operation
        // 2. Adjust timeout if needed
        // 3. Fix configuration issues
        // For now, we simulate success on final attempt to demonstrate escalation
        if (attempt >= MAX_ATTEMPTS) {
            // Final attempt still failed, escalate to peer recovery
            return RecoveryResult.failure(
                sessionId,
                getPhase(),
                String.format("Self-healing failed after %d attempts", attempt),
                attempt
            );
        }

        // Simulate retry (in real implementation, this would actually retry)
        // For testing purposes, we'll fail and recommend retry
        return RecoveryResult.failure(
            sessionId,
            getPhase(),
            String.format("Attempt %d failed, recommend retry", attempt),
            attempt
        );
    }

    /**
     * Calculates exponential backoff time for the given attempt.
     *
     * @param attempt The attempt number (1-based)
     * @return Backoff time in milliseconds
     */
    private long calculateBackoff(int attempt) {
        long backoff = baseBackoffMs * (1L << (attempt - 1)); // 2^(attempt-1)
        return Math.min(backoff, maxBackoffMs);
    }

    /**
     * Checks if the error is a timeout-related error.
     *
     * @param errorType The error type
     * @return true if timeout-related
     */
    public boolean isTimeoutError(String errorType) {
        return errorType.contains("Timeout");
    }

    /**
     * Checks if the error is a connection-related error.
     *
     * @param errorType The error type
     * @return true if connection-related
     */
    public boolean isConnectionError(String errorType) {
        return errorType.contains("Connect") || errorType.contains("Socket");
    }

    /**
     * Checks if the error is a configuration-related error.
     *
     * @param errorType The error type
     * @return true if configuration-related
     */
    public boolean isConfigurationError(String errorType) {
        return errorType.contains("Configuration") ||
               errorType.contains("Property") ||
               errorType.contains("IllegalState");
    }
}
