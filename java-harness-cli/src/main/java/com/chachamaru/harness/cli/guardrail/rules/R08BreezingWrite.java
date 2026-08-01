package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R08: Breezing write detection
 */
public class R08BreezingWrite implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R08_BREEZING_WRITE;
    }

    @Override
    public String getName() {
        return "Breezing Write Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        // Check if this is being called from Breezing mode
        String sessionId = input.sessionId();
        return sessionId != null && sessionId.contains("breezing");
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        // R08 is about detecting write operations during Breezing mode
        // For now, we allow it but log it for monitoring
        String tool = input.toolName();
        if ("Write".equals(tool) || "Edit".equals(tool)) {
            // Check for Breezing-specific patterns
            // This is a placeholder for more sophisticated detection
            return GuardrailResult.allowed();
        }

        return GuardrailResult.allowed();
    }
}
