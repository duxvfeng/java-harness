package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * R02: Protected path detection
 */
public class R02ProtectedPath implements Rule {

    private static final List<String> PROTECTED_PATTERNS = List.of(
        ".env",
        ".env.*",
        "*.pem",
        "*.key",
        "id_rsa",
        "id_ed25519",
        ".git",
        "secrets"
    );

    @Override
    public String getId() {
        return GuardrailConstants.R02_PROTECTED_PATH;
    }

    @Override
    public String getName() {
        return "Protected Path Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        String tool = input.toolName();
        return "Write".equals(tool) || "Edit".equals(tool) || "MultiEdit".equals(tool);
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String filePath = (String) input.toolInput().get("file_path");
        if (filePath == null) {
            return GuardrailResult.allowed();
        }

        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();

        // Check exact matches
        for (String pattern : PROTECTED_PATTERNS) {
            if (fileName.equals(pattern) || fileName.startsWith(".")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R02_PROTECTED_PATH,
                    "Cannot write to protected path: " + filePath
                );
            }
        }

        return GuardrailResult.allowed();
    }
}
