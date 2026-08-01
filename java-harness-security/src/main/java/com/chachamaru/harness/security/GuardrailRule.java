package com.chachamaru.harness.security;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.protocol.HookEventType;

/**
 * Interface for guardrail rules that evaluate hook events for security/policy compliance.
 *
 * <p>Guardrail rules evaluate incoming hook events against security policies and
 * return decisions to allow, deny, or modify the execution.</p>
 *
 * @since 4.1.0
 */
public interface GuardrailRule {

    /**
     * Returns the unique identifier for this rule.
     *
     * @return the rule ID (e.g., "R01", "R02")
     */
    String getId();

    /**
     * Returns a human-readable description of this rule.
     *
     * @return the rule description
     */
    String getDescription();

    /**
     * Determines if this rule should evaluate the given event type.
     *
     * @param eventType the hook event type
     * @return true if this rule applies to the event type
     */
    boolean matches(HookEventType eventType);

    /**
     * Evaluates the hook input against this rule.
     *
     * @param eventType the hook event type
     * @param toolName the name of the tool being invoked
     * @param toolInput the input parameters for the tool
     * @return the guardrail result decision
     */
    GuardrailResult evaluate(HookEventType eventType, String toolName, java.util.Map<String, Object> toolInput);

    /**
     * Returns the priority of this rule (higher values = higher priority).
     *
     * @return the priority value
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Determines if this rule is enabled.
     *
     * @return true if enabled, false otherwise
     */
    default boolean isEnabled() {
        return true;
    }
}
