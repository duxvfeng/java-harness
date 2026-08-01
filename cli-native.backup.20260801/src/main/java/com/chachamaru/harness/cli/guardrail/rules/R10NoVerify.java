package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R10: No verify detection (bypassing security checks)
 */
public class R10NoVerify implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R10_NO_VERIFY;
    }

    @Override
    public String getName() {
        return "No Verify Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        return "Bash".equals(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String command = (String) input.toolInput().get("command");
        if (command == null) {
            return GuardrailResult.allowed();
        }

        String lowerCmd = command.toLowerCase();
        // Check for various --no-verify flags
        if (lowerCmd.contains("--no-verify") ||
            lowerCmd.contains("--skip-verify") ||
            lowerCmd.contains("--disable-verify")) {
            return GuardrailResult.denied(
                GuardrailConstants.R10_NO_VERIFY,
                "Bypassing security verification is not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}
