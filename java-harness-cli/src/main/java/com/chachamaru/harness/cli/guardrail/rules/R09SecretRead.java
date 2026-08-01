package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

import java.nio.file.Paths;
import java.util.List;

/**
 * R09: Secret read detection
 */
public class R09SecretRead implements Rule {

    private static final List<String> SECRET_PATTERNS = List.of(
        ".env",
        "secrets",
        "*.pem",
        "*.key",
        "password",
        "credentials",
        "token"
    );

    @Override
    public String getId() {
        return GuardrailConstants.R09_SECRET_READ;
    }

    @Override
    public String getName() {
        return "Secret Read Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        String tool = input.toolName();
        return "Read".equals(tool) || "Bash".equals(tool);
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String filePath = null;

        if ("Read".equals(input.toolName())) {
            filePath = (String) input.toolInput().get("file_path");
        } else if ("Bash".equals(input.toolName())) {
            String command = (String) input.toolInput().get("command");
            if (command != null) {
                // Check if command contains sensitive file patterns
                String lowerCmd = command.toLowerCase();
                for (String pattern : SECRET_PATTERNS) {
                    if (lowerCmd.contains(pattern.replace("*", ""))) {
                        return GuardrailResult.denied(
                            GuardrailConstants.R09_SECRET_READ,
                            "Reading secret files is not allowed"
                        );
                    }
                }
                return GuardrailResult.allowed();
            }
        }

        if (filePath == null) {
            return GuardrailResult.allowed();
        }

        String fileName = Paths.get(filePath).getFileName().toString().toLowerCase();
        for (String pattern : SECRET_PATTERNS) {
            if (pattern.contains("*")) {
                if (fileName.endsWith(pattern.replace("*", ""))) {
                    return GuardrailResult.denied(
                        GuardrailConstants.R09_SECRET_READ,
                        "Reading secret files is not allowed"
                    );
                }
            } else if (fileName.contains(pattern)) {
                return GuardrailResult.denied(
                    GuardrailConstants.R09_SECRET_READ,
                    "Reading secret files is not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}
