package com.chachamaru.harness.shared.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Guardrail decision result
 */
public record GuardrailDecision(
    @JsonProperty("action")
    Action action,

    @JsonProperty("rule_id")
    String ruleId,

    @JsonProperty("reason")
    String reason,

    @JsonProperty("details")
    java.util.Map<String, Object> details
) {
    public enum Action {
        ALLOW,
        DENY,
        DEFER,
        WARN
    }

    public static GuardrailDecision allow() {
        return new GuardrailDecision(Action.ALLOW, null, null, null);
    }

    public static GuardrailDecision deny(String ruleId, String reason) {
        return new GuardrailDecision(Action.DENY, ruleId, reason, null);
    }
}
