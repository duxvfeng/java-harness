package com.chachamaru.harness.cli.guardrail;

import com.chachamaru.harness.shared.dto.GuardrailDecision;

/**
 * Guardrail evaluation result
 */
public record GuardrailResult(
    GuardrailDecision decision
) {
    public boolean isAllowed() {
        return decision == null || decision.action() == GuardrailDecision.Action.ALLOW;
    }

    public boolean isDenied() {
        return decision != null && decision.action() == GuardrailDecision.Action.DENY;
    }

    public static GuardrailResult allowed() {
        return new GuardrailResult(null);
    }

    public static GuardrailResult denied(String ruleId, String reason) {
        return new GuardrailResult(GuardrailDecision.deny(ruleId, reason));
    }
}
