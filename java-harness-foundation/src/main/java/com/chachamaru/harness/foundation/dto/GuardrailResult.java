package com.chachamaru.harness.foundation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Guardrail evaluation result DTO.
 *
 * <p>Represents the result of evaluating a guardrail rule against a hook input.
 * Contains the decision, rule identifier, reason, and optional blocking flag.</p>
 *
 * @spec_reference spec.md#Data Models
 */
public record GuardrailResult(
    @JsonProperty("decision")
    Decision decision,

    @JsonProperty("rule_id")
    String ruleId,

    @JsonProperty("reason")
    String reason,

    @JsonProperty("block")
    boolean block,

    @JsonProperty("details")
    Map<String, Object> details
) {
    /**
     * Guardrail decision types.
     */
    public enum Decision {
        /** Allow the action to proceed */
        ALLOW,

        /** Deny the action */
        DENY,

        /** Ask the user for confirmation */
        ASK,

        /** Warn but allow */
        WARN
    }

    /**
     * Creates an ALLOW result.
     */
    public static GuardrailResult allow(String ruleId) {
        return new GuardrailResult(Decision.ALLOW, ruleId, "Allowed", false, Map.of());
    }

    /**
     * Creates a DENY result with blocking.
     */
    public static GuardrailResult deny(String ruleId, String reason) {
        return new GuardrailResult(Decision.DENY, ruleId, reason, true, Map.of());
    }

    /**
     * Creates an ASK result.
     */
    public static GuardrailResult ask(String ruleId, String reason) {
        return new GuardrailResult(Decision.ASK, ruleId, reason, false, Map.of());
    }

    /**
     * Creates a WARN result.
     */
    public static GuardrailResult warn(String ruleId, String reason) {
        return new GuardrailResult(Decision.WARN, ruleId, reason, false, Map.of());
    }

    /**
     * Checks if the result allows the action.
     * ALLOW and WARN decisions allow the action to proceed.
     */
    public boolean isAllowed() {
        return decision == Decision.ALLOW || decision == Decision.WARN;
    }

    /**
     * Checks if the result blocks the action.
     */
    public boolean isBlocked() {
        return block || decision == Decision.DENY;
    }

    /**
     * Creates a result with custom details.
     */
    public GuardrailResult withDetails(Map<String, Object> details) {
        return new GuardrailResult(decision, ruleId, reason, block, details);
    }
}
