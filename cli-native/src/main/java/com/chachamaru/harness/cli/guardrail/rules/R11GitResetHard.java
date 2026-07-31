package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R11: git reset --hard detection
 */
public class R11GitResetHard implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R11_GIT_RESET_HARD;
    }

    @Override
    public String getName() {
        return "Git Reset Hard Rule";
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
        if (lowerCmd.contains("git reset") && lowerCmd.contains("--hard")) {
            return GuardrailResult.denied(
                GuardrailConstants.R11_GIT_RESET_HARD,
                "Hard reset is not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}
