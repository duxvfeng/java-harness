package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R12: Protected branch push detection
 */
public class R12ProtectedBranchPush implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R12_PROTECTED_BRANCH_PUSH;
    }

    @Override
    public String getName() {
        return "Protected Branch Push Rule";
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
        if (lowerCmd.contains("git push")) {
            // Check if pushing to protected branch
            for (String protectedBranch : GuardrailConstants.PROTECTED_BRANCHES) {
                if (lowerCmd.contains("origin " + protectedBranch) ||
                    lowerCmd.contains("origin/" + protectedBranch)) {
                    return GuardrailResult.denied(
                        GuardrailConstants.R12_PROTECTED_BRANCH_PUSH,
                        "Push to protected branch '" + protectedBranch + "' is not allowed"
                    );
                }
            }
        }

        return GuardrailResult.allowed();
    }
}
