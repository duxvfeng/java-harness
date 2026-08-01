package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R18: Configuration file write detection
 */
public class R18ConfigFileWrite implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R18_CONFIG_FILE_WRITE;
    }

    @Override
    public String getName() {
        return "Config File Write Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        return "Write".equals(input.toolName()) || "Edit".equals(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String filePath = (String) input.toolInput().get("file_path");
        if (filePath == null) {
            return GuardrailResult.allowed();
        }

        String lowerPath = filePath.toLowerCase();
        // Check for critical configuration files
        if (lowerPath.contains("nginx.conf") || lowerPath.contains("apache.conf") ||
            lowerPath.contains("httpd.conf") || lowerPath.contains("my.cnf") ||
            lowerPath.contains("postgresql.conf") || lowerPath.contains("redis.conf") ||
            lowerPath.contains("docker-compose.yml") || lowerPath.contains("docker-compose.yaml") ||
            lowerPath.contains("k8s") || lowerPath.contains("kubernetes") ||
            lowerPath.endsWith(".env") || lowerPath.endsWith(".env.production") ||
            lowerPath.endsWith(".env.prod") || lowerPath.contains("config.toml") ||
            lowerPath.contains("config.yml") || lowerPath.contains("config.yaml")) {
            // Check for production environment indicators
            if (lowerPath.contains("production") || lowerPath.contains("prod") ||
                lowerPath.contains("/prod/") || lowerPath.contains("/production/")) {
                return GuardrailResult.denied(
                    GuardrailConstants.R18_CONFIG_FILE_WRITE,
                    "Direct production configuration file writes are not allowed"
                );
            }
        }

        return GuardrailResult.allowed();
    }
}