package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R06: git push --force detection
 */
public class R06GitPushForce implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R06_GIT_PUSH_FORCE;
    }

    @Override
    public String getName() {
        return "Git Push Force Rule";
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
        if (lowerCmd.contains("git push") &&
            (lowerCmd.contains("--force") || lowerCmd.contains("-f"))) {
            return GuardrailResult.denied(
                GuardrailConstants.R06_GIT_PUSH_FORCE,
                "Force push is not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}
