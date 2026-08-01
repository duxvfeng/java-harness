package com.chachamaru.harness.collaboration.agent.message;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Request for advisor consultation.
 *
 * <p>Message sent by Worker agent when it needs guidance on implementation approach.
 * The Lead agent routes this to an Advisor agent and returns the response.</p>
 *
 * @spec_reference spec.md#Breezing Mode Advisor Protocol
 * @since 4.2.0
 */
public record AdvisorRequestV1(
    String requestId,
    String workerId,
    String taskId,
    String question,
    QuestionType questionType,
    String context,
    Map<String, Object> metadata,
    Instant timestamp
) {
    /**
     * Creates an advisor request.
     */
    public AdvisorRequestV1 {
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    /**
     * Creates an advisor request with generated ID and current timestamp.
     */
    public static AdvisorRequestV1 create(String workerId, String taskId, String question, QuestionType questionType, String context) {
        return new AdvisorRequestV1(
            UUID.randomUUID().toString(),
            workerId,
            taskId,
            question,
            questionType,
            context,
            Map.of(),
            Instant.now()
        );
    }

    /**
     * Type of question being asked.
     */
    public enum QuestionType {
        /** Question about implementation approach */
        IMPLEMENTATION,
        /** Question about architectural decisions */
        ARCHITECTURE,
        /** Question about debugging an issue */
        DEBUGGING,
        /** Question about best practices */
        BEST_PRACTICE,
        /** General question */
        GENERAL
    }
}