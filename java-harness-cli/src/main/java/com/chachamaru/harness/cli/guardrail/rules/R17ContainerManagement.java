package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R17: Container management operation detection
 */
public class R17ContainerManagement implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R17_CONTAINER_MANAGEMENT;
    }

    @Override
    public String getName() {
        return "Container Management Rule";
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
        // Check for container management commands
        if (lowerCmd.contains("docker rm") || lowerCmd.contains("docker rmi") ||
            lowerCmd.contains("docker stop") || lowerCmd.contains("docker kill") ||
            lowerCmd.contains("kubectl delete") || lowerCmd.contains("kubectl drain") ||
            lowerCmd.contains("podman rm") || lowerCmd.contains("podman rmi") ||
            lowerCmd.contains("docker-compose down") || lowerCmd.contains("docker compose down")) {
            // Check for production environment indicators
            if (lowerCmd.contains("production") || lowerCmd.contains("prod") ||
                lowerCmd.contains("--env=prod") || lowerCmd.contains("-e production") ||
                lowerCmd.contains("prod-cluster") || lowerCmd.contains("production-cluster")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R17_CONTAINER_MANAGEMENT,
                    "Direct production container management operations are not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}