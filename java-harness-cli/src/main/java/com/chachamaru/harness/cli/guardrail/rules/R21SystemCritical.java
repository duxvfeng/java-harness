package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R21: System critical operation detection
 */
public class R21SystemCritical implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R21_SYSTEM_CRITICAL;
    }

    @Override
    public String getName() {
        return "System Critical Rule";
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
        // Check for system critical operations
        if (lowerCmd.contains("shutdown") || lowerCmd.contains("reboot") ||
            lowerCmd.contains("halt") || lowerCmd.contains("poweroff") ||
            lowerCmd.contains("systemctl stop") || lowerCmd.contains("service stop") ||
            lowerCmd.contains("init 0") || lowerCmd.contains("telinit 0") ||
            lowerCmd.contains("killall") || lowerCmd.contains("pkill -9")) {
            return GuardrailResult.denied(
                GuardrailConstants.R21_SYSTEM_CRITICAL,
                "System critical operations like shutdown/reboot/kill-all are not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}