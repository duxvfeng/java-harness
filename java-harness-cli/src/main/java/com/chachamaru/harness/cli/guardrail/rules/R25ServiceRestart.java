package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R25: Service restart detection
 */
public class R25ServiceRestart implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R25_SERVICE_RESTART;
    }

    @Override
    public String getName() {
        return "Service Restart Rule";
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
        // Check for service restart operations
        if (lowerCmd.contains("systemctl restart") || lowerCmd.contains("service restart") ||
            lowerCmd.contains("systemctl reload") || lowerCmd.contains("service reload") ||
            lowerCmd.contains("systemctl start") && lowerCmd.contains("systemctl stop") ||
            lowerCmd.contains("restart") || lowerCmd.contains("reload")) {
            // Check for critical services
            if (lowerCmd.contains("nginx") || lowerCmd.contains("apache") ||
                lowerCmd.contains("httpd") || lowerCmd.contains("mysql") ||
                lowerCmd.contains("postgresql") || lowerCmd.contains("redis") ||
                lowerCmd.contains("mongodb") || lowerCmd.contains("docker") ||
                lowerCmd.contains("kubelet") || lowerCmd.contains("network")) {
                // Check for production environment
                if (lowerCmd.contains("production") || lowerCmd.contains("prod")) {
                    return GuardrailResult.denied(
                        GuardrailConstants.R25_SERVICE_RESTART,
                        "Production critical service restart operations are not allowed"
                    );
                }
            }
        }

        return GuardrailResult.allowed();
    }
}