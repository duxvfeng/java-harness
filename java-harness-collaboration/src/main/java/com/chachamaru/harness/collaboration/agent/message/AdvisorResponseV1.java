package com.chachamaru.harness.collaboration.agent.message;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Response from advisor consultation.
 *
 * <p>Message returned by Advisor agent with guidance on the requested question.
 * Contains advice type (PLAN/CORRECTION/STOP), recommendations, and reasoning.</p>
 *
 * @spec_reference spec.md#Breezing Mode Advisor Protocol
 * @since 4.2.0
 */
public record AdvisorResponseV1(
    String requestId,
    String adviceType,
    List<String> recommendations,
    String reasoning,
    String summary,
    Map<String, Object> metadata,
    Instant timestamp
) {
    /**
     * Creates an advisor response.
     */
    public AdvisorResponseV1 {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("requestId cannot be null or blank");
        }
        if (adviceType == null) {
            throw new IllegalArgumentException("adviceType cannot be null");
        }
        if (recommendations == null) {
            recommendations = List.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Creates a PLAN response - provides implementation plan.
     */
    public static AdvisorResponseV1 plan(String requestId, List<String> steps, String reasoning) {
        return new AdvisorResponseV1(
            requestId,
            AdviceType.PLAN.name(),
            steps,
            reasoning,
            "Implementation plan provided",
            Map.of(),
            Instant.now()
        );
    }

    /**
     * Creates a CORRECTION response - suggests fixes.
     */
    public static AdvisorResponseV1 correction(String requestId, List<String> fixes, String reasoning) {
        return new AdvisorResponseV1(
            requestId,
            AdviceType.CORRECTION.name(),
            fixes,
            reasoning,
            "Corrections suggested",
            Map.of(),
            Instant.now()
        );
    }

    /**
     * Creates a STOP response - halts execution.
     */
    public static AdvisorResponseV1 stop(String requestId, String reason) {
        return new AdvisorResponseV1(
            requestId,
            AdviceType.STOP.name(),
            List.of("Execution stopped: " + reason),
            reason,
            "Execution halted",
            Map.of(),
            Instant.now()
        );
    }

    /**
     * Gets the advice type as enum.
     */
    public AdviceType getAdviceTypeEnum() {
        try {
            return AdviceType.valueOf(adviceType);
        } catch (IllegalArgumentException e) {
            return AdviceType.CORRECTION; // Default
        }
    }

    /**
     * Checks if this is a STOP response.
     */
    public boolean isStop() {
        return getAdviceTypeEnum() == AdviceType.STOP;
    }

    /**
     * Advice type enumeration.
     */
    public enum AdviceType {
        /** Provides implementation plan/approach */
        PLAN,
        /** Suggests corrections to current approach */
        CORRECTION,
        /** Halts execution and escalates to user */
        STOP
    }
}