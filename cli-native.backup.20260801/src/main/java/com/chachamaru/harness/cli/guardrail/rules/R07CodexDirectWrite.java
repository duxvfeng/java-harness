package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R07: Codex direct write detection
 */
public class R07CodexDirectWrite implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R07_CODEX_DIRECT_WRITE;
    }

    @Override
    public String getName() {
        return "Codex Direct Write Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        // Check if this is being called from Codex
        String permissionMode = input.permissionMode();
        return "default".equals(permissionMode) || "bypass".equals(permissionMode);
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        // R07 is about detecting direct write attempts from Codex
        // For now, we allow it but log it
        String tool = input.toolName();
        if ("Write".equals(tool) || "Edit".equals(tool)) {
            // Check for Codex-specific patterns in the context
            // This is a placeholder for more sophisticated detection
            return GuardrailResult.allowed();
        }

        return GuardrailResult.allowed();
    }
}
