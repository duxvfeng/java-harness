package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R01: No sudo commands
 */
public class R01NoSudo implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R01_NO_SUDO;
    }

    @Override
    public String getName() {
        return "No Sudo Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        return "Bash".equals(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String command = (String) input.toolInput().get("command");
        if (command != null && command.toLowerCase().contains("sudo")) {
            return GuardrailResult.denied(
                GuardrailConstants.R01_NO_SUDO,
                "commands requiring elevation are not allowed"
            );
        }
        return GuardrailResult.allowed();
    }
}
