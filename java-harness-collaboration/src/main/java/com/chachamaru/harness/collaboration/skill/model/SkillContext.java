package com.chachamaru.harness.collaboration.skill.model;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.protocol.model.Task;

import java.util.Map;

/**
 * Context for skill execution.
 *
 * <p>Provides all necessary context for a skill to execute,
 * including task information, hook input, and configuration parameters.</p>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public record SkillContext(
    String skillId,
    String skillName,
    Task task,
    HookInput hookInput,
    Map<String, Object> configuration,
    Map<String, Object> sessionState
) {
    /**
     * Creates a skill context.
     */
    public SkillContext {
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId cannot be null or blank");
        }
        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException("skillName cannot be null or blank");
        }
        if (configuration == null) {
            configuration = Map.of();
        }
        if (sessionState == null) {
            sessionState = Map.of();
        }
    }

    /**
     * Creates a minimal skill context for testing.
     */
    public static SkillContext createForTest(String skillId, String skillName) {
        return new SkillContext(
            skillId,
            skillName,
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of()
        );
    }

    /**
     * Gets a configuration value.
     */
    public <T> T getConfiguration(String key, Class<T> type) {
        Object value = configuration.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }

    /**
     * Gets a session state value.
     */
    public <T> T getSessionState(String key, Class<T> type) {
        Object value = sessionState.get(key);
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
}
