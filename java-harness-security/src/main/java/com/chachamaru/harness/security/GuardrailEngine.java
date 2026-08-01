package com.chachamaru.harness.security;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.protocol.HookEventType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Engine for evaluating guardrail rules against hook events.
 *
 * <p>The GuardrailEngine manages a registry of rules and evaluates hook events
 * against them to determine if actions should be allowed, denied, or modified.</p>
 *
 * @since 4.1.0
 */
public interface GuardrailEngine {

    /**
     * Registers a guardrail rule with this engine.
     *
     * @param rule the rule to register
     */
    void registerRule(GuardrailRule rule);

    /**
     * Unregisters a guardrail rule.
     *
     * @param ruleId the ID of the rule to unregister
     * @return true if the rule was found and removed, false otherwise
     */
    boolean unregisterRule(String ruleId);

    /**
     * Evaluates a hook event against all registered rules (synchronous).
     *
     * <p>Evaluates rules in priority order (highest first). Returns the first
     * deny result, or allow if no rules deny.</p>
     *
     * @param eventType the type of hook event
     * @param toolName the name of the tool being invoked
     * @param toolInput the input parameters for the tool
     * @return the guardrail result
     */
    GuardrailResult evaluate(HookEventType eventType, String toolName, Map<String, Object> toolInput);

    /**
     * Evaluates a hook event asynchronously.
     *
     * <p>This method allows for complex or long-running evaluations to be
     * performed asynchronously, potentially delegating to external services.</p>
     *
     * @param eventType the type of hook event
     * @param toolName the name of the tool being invoked
     * @param toolInput the input parameters for the tool
     * @return CompletableFuture containing the guardrail result
     */
    CompletableFuture<GuardrailResult> evaluateAsync(HookEventType eventType, String toolName, Map<String, Object> toolInput);

    /**
     * Returns the number of registered rules.
     *
     * @return the rule count
     */
    int getRuleCount();

    /**
     * Clears all registered rules.
     */
    void clearRules();

    /**
     * Shuts down the engine and releases resources.
     */
    void shutdown();
}
