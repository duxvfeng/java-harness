package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R03: Redirection bypass detection
 */
public class R03RedirectionBypass implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R03_REDIRECTION_BYPASS;
    }

    @Override
    public String getName() {
        return "Redirection Bypass Rule";
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
        // Check for shell redirection bypass attempts
        if (lowerCmd.contains(">/dev/") && lowerCmd.contains("2>&1")) {
            return GuardrailResult.denied(
                GuardrailConstants.R03_REDIRECTION_BYPASS,
                "Shell redirection bypass is not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}
