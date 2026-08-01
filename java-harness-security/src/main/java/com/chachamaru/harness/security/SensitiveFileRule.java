package com.chachamaru.harness.security;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.protocol.HookEventType;

import java.util.Map;

/**
 * Example guardrail rule that denies access to sensitive file operations.
 *
 * @since 4.1.0
 */
public class SensitiveFileRule implements GuardrailRule {

    private static final String[] SENSITIVE_PATTERNS = {
        "/etc/passwd", "/etc/shadow", "/etc/hosts",
        "password", "credential", "secret", "key"
    };

    @Override
    public String getId() {
        return "R01";
    }

    @Override
    public String getDescription() {
        return "Prevents access to sensitive files and credentials";
    }

    @Override
    public boolean matches(HookEventType eventType) {
        // Apply to all hook types
        return true;
    }

    @Override
    public GuardrailResult evaluate(HookEventType eventType, String toolName, Map<String, Object> toolInput) {
        if (toolInput == null) {
            return GuardrailResult.allow(getId());
        }

        // Check tool name for file operations
        if (toolName != null && (toolName.contains("Write") || toolName.contains("Edit") ||
                toolName.contains("Delete") || toolName.contains("Read"))) {

            // Check input parameters for sensitive patterns
            for (Map.Entry<String, Object> entry : toolInput.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString().toLowerCase() : "";

                for (String pattern : SENSITIVE_PATTERNS) {
                    if (value.contains(pattern.toLowerCase())) {
                        return GuardrailResult.deny(getId(),
                                String.format("Potential sensitive file access detected: '%s'", pattern));
                    }
                }
            }
        }

        return GuardrailResult.allow(getId());
    }

    @Override
    public int getPriority() {
        return 100; // High priority
    }
}
