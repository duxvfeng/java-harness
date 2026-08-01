package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R15: Production deployment detection
 */
public class R15ProductionDeploy implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R15_PRODUCTION_DEPLOY;
    }

    @Override
    public String getName() {
        return "Production Deploy Rule";
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
        // Check for production deployment commands
        if (lowerCmd.contains("deploy") || lowerCmd.contains("kubectl apply") ||
            lowerCmd.contains("terraform apply") || lowerCmd.contains("ansible-playbook")) {
            // Check for production environment indicators
            if (lowerCmd.contains("production") || lowerCmd.contains("prod") ||
                lowerCmd.contains("--env=prod") || lowerCmd.contains("-e production")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R15_PRODUCTION_DEPLOY,
                    "Direct production deployment is not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}
