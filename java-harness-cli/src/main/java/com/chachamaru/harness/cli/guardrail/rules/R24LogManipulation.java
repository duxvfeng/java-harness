package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R24: Log manipulation detection
 */
public class R24LogManipulation implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R24_LOG_MANIPULATION;
    }

    @Override
    public String getName() {
        return "Log Manipulation Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        return "Bash".equals(input.toolName()) || "Write".equals(input.toolName()) ||
               "Edit".equals(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String command = (String) input.toolInput().get("command");
        String filePath = (String) input.toolInput().get("file_path");

        // Bash command checks
        if (command != null && "Bash".equals(input.toolName())) {
            String lowerCmd = command.toLowerCase();
            if (lowerCmd.contains("rm ") && lowerCmd.contains("log") ||
                lowerCmd.contains("echo '' >") && lowerCmd.contains("log") ||
                lowerCmd.contains("> /dev/null") && lowerCmd.contains("log") ||
                lowerCmd.contains("truncate") && lowerCmd.contains("log")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R24_LOG_MANIPULATION,
                    "Log deletion or manipulation operations are not allowed"
                );
            }
        }

        // File write checks for log files
        if (filePath != null && ("Write".equals(input.toolName()) || "Edit".equals(input.toolName()))) {
            String lowerPath = filePath.toLowerCase();
            if (lowerPath.endsWith(".log") || lowerPath.contains("/logs/") ||
                lowerPath.contains("/log/") || lowerPath.contains("access.log") ||
                lowerPath.contains("error.log") || lowerPath.contains("application.log")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R24_LOG_MANIPULATION,
                    "Direct log file modifications are not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}