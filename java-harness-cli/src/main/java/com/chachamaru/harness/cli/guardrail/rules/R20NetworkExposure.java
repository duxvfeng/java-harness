package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R20: Network exposure detection
 */
public class R20NetworkExposure implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R20_NETWORK_EXPOSURE;
    }

    @Override
    public String getName() {
        return "Network Exposure Rule";
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
        // Check for network exposure operations
        if (lowerCmd.contains("iptables") || lowerCmd.contains("ufw allow") ||
            lowerCmd.contains("firewall-cmd") || lowerCmd.contains("netsh") ||
            lowerCmd.contains("nc -l") || lowerCmd.contains("netcat") ||
            lowerCmd.contains("socat") || lowerCmd.contains("port-forward") ||
            lowerCmd.contains("kubectl expose") || lowerCmd.contains("kubectl port-forward")) {
            // Check for exposure to public networks
            if (lowerCmd.contains("0.0.0.0") || lowerCmd.contains("--allow-all") ||
                lowerCmd.contains("public") || lowerCmd.contains("--permanent")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R20_NETWORK_EXPOSURE,
                    "Network exposure operations that open services to public networks are not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}