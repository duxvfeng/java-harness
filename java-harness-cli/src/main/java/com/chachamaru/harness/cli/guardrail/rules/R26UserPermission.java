package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R26: User permission modification detection
 */
public class R26UserPermission implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R26_USER_PERMISSION;
    }

    @Override
    public String getName() {
        return "User Permission Rule";
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
        // Check for user permission operations
        if (lowerCmd.contains("chmod 777") || lowerCmd.contains("chmod a+w") ||
            lowerCmd.contains("chmod a+rwx") || lowerCmd.contains("chown") ||
            lowerCmd.contains("usermod") || lowerCmd.contains("groupmod") ||
            lowerCmd.contains("useradd") || lowerCmd.contains("userdel") ||
            lowerCmd.contains("passwd ") || lowerCmd.contains("sudo -i") ||
            lowerCmd.contains("sudo su")) {
            return GuardrailResult.denied(
                GuardrailConstants.R26_USER_PERMISSION,
                "User and permission modification operations are not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}