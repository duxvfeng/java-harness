package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R19: Executable download detection
 */
public class R19ExecutableDownload implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R19_EXECUTABLE_DOWNLOAD;
    }

    @Override
    public String getName() {
        return "Executable Download Rule";
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
        // Check for executable download patterns
        boolean isDownload = lowerCmd.contains("curl") || lowerCmd.contains("wget") ||
                           lowerCmd.contains("download") || lowerCmd.contains("fetch");

        boolean isExecutable = lowerCmd.contains(".exe") || lowerCmd.contains(".bin") ||
                              lowerCmd.contains(".sh") || lowerCmd.contains(".dll") ||
                              lowerCmd.contains(".so") || lowerCmd.contains(".dylib") ||
                              lowerCmd.contains("chmod +x") || lowerCmd.contains("chmod 755");

        if (isDownload && isExecutable) {
            return GuardrailResult.denied(
                GuardrailConstants.R19_EXECUTABLE_DOWNLOAD,
                "Downloading executable files from external sources is not allowed"
            );
        }

        return GuardrailResult.allowed();
    }
}