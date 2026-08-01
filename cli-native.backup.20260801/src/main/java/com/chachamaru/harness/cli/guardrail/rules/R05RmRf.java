package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R05: rm -rf detection
 */
public class R05RmRf implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R05_RM_RF;
    }

    @Override
    public String getName() {
        return "Rm Rf Rule";
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
        if (lowerCmd.contains("rm -rf") || lowerCmd.contains("rm --recursive")) {
            return GuardrailResult.denied(
                GuardrailConstants.R05_RM_RF,
                "Recursive deletion is not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}
