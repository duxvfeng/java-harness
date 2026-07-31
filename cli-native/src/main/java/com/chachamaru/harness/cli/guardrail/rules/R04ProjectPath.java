package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

import java.nio.file.Paths;

/**
 * R04: Project path detection
 */
public class R04ProjectPath implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R04_PROJECT_PATH;
    }

    @Override
    public String getName() {
        return "Project Path Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        String tool = input.toolName();
        return "Write".equals(tool) || "Edit".equals(tool) || "MultiEdit".equals(tool);
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String filePath = (String) input.toolInput().get("file_path");
        if (filePath == null || input.cwd() == null) {
            return GuardrailResult.allowed();
        }

        // Check if file is outside project directory
        var projectPath = Paths.get(input.cwd()).toAbsolutePath();
        var targetPath = Paths.get(filePath).toAbsolutePath();

        if (!targetPath.startsWith(projectPath)) {
            return GuardrailResult.denied(
                GuardrailConstants.R04_PROJECT_PATH,
                "Cannot write files outside project directory"
            );
        }

        return GuardrailResult.allowed();
    }
}
