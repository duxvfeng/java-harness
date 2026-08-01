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

    public String ruleId() {
        return decision != null ? decision.ruleId() : null;
    }

    public String reason() {
        return decision != null ? decision.reason() : null;
    }

    public static GuardrailResult allowed() {
        return new GuardrailResult(null);
    }

    public static GuardrailResult allowed(String reason) {
        return new GuardrailResult(GuardrailDecision.allow(reason));
    }

    public static GuardrailResult denied(String ruleId, String reason) {
        return new GuardrailResult(GuardrailDecision.deny(ruleId, reason));
    }
}
