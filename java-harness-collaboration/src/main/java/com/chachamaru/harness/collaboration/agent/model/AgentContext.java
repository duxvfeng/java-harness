package com.chachamaru.harness.collaboration.agent.model;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.workflow.model.Task;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Context for agent execution.
 *
 * <p>Provides all necessary context for an agent to execute,
 * including task information, hook input, configuration, and state.</p>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public record AgentContext(
    String agentId,
    String agentName,
    AgentType agentType,
    Task task,
    HookInput hookInput,
    Map<String, Object> configuration,
    Map<String, Object> sessionState,
    LocalDateTime executionStartTime
) {
    /**
     * Creates an agent context.
     */
    public AgentContext {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId cannot be null or blank");
        }
        if (agentName == null || agentName.isBlank()) {
            throw new IllegalArgumentException("agentName cannot be null or blank");
        }
        if (agentType == null) {
            throw new IllegalArgumentException("agentType cannot be null");
        }
        if (configuration == null) {
            configuration = Map.of();
        }
        if (sessionState == null) {
            sessionState = Map.of();
        }
        if (executionStartTime == null) {
            executionStartTime = LocalDateTime.now();
        }
    }

    /**
     * Creates a minimal agent context for testing.
     */
    public static AgentContext createForTest(String agentId, String agentName, AgentType agentType) {
        return new AgentContext(
            agentId,
            agentName,
            agentType,
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of(),
            LocalDateTime.now()
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

    /**
     * Agent type enumeration.
     */
    public enum AgentType {
        /** Worker agent - executes tasks */
        WORKER,
        /** Reviewer agent - reviews code and changes */
        REVIEWER,
        /** Advisor agent - provides guidance */
        ADVISOR,
        /** Coordinator agent - coordinates other agents */
        COORDINATOR
    }
}
