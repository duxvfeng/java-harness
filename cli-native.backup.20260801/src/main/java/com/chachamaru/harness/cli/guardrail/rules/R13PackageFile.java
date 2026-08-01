package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

import java.nio.file.Paths;
import java.util.List;

/**
 * R13: Package file detection
 */
public class R13PackageFile implements Rule {

    private static final List<String> PACKAGE_FILES = List.of(
        "package.json",
        "pom.xml",
        "build.gradle",
        "Cargo.toml",
        "go.mod",
        "requirements.txt",
        "Gemfile",
        "composer.json"
    );

    @Override
    public String getId() {
        return GuardrailConstants.R13_PACKAGE_FILE;
    }

    @Override
    public String getName() {
        return "Package File Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        String tool = input.toolName();
        return "Write".equals(tool) || "Edit".equals(tool);
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String filePath = (String) input.toolInput().get("file_path");
        if (filePath == null) {
            return GuardrailResult.allowed();
        }

        String fileName = Paths.get(filePath).getFileName().toString();
        if (PACKAGE_FILES.contains(fileName)) {
            // Check if this is a dependency modification vs package config change
            // For now, we allow package file edits but could add more sophisticated checks
            return GuardrailResult.allowed();
        }

        return GuardrailResult.allowed();
    }
}
