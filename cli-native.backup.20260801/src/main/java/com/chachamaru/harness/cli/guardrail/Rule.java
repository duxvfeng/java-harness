package com.chachamaru.harness.cli.guardrail;

import com.chachamaru.harness.cli.hook.HookInput;

/**
 * Guardrail rule interface
 */
public interface Rule {

    /**
     * Get rule identifier (e.g., "R01")
     */
    String getId();

    /**
     * Get rule name
     */
    String getName();

    /**
     * Check if this rule matches the input
     */
    boolean matches(HookInput input);

    /**
     * Evaluate the rule and return decision
     */
    GuardrailResult evaluate(HookInput input);
}
