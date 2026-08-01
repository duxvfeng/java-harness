package com.chachamaru.harness.cli.guardrail;

/**
 * Extended rule interface with priority support
 */
public interface PrioritizedRule extends Rule {

    /**
     * Get rule priority (higher number = higher priority)
     * Default priority is 0. Built-in rules typically have priority 0-99.
     * Custom rules can have priority 100+ to override built-in rules.
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Check if this rule should override lower priority rules
     */
    default boolean isOverride() {
        return getPriority() >= 100;
    }
}