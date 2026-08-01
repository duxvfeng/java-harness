package com.chachamaru.harness.foundation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Hook output DTO - represents output from hook event handlers.
 *
 * <p>This is the primary output format for all hook event processing.
 * It contains permission decisions, modified inputs, and additional context.</p>
 *
 * @spec_reference spec.md#Data Models
 */
public record HookOutput(
    @JsonProperty("hook_event_name")
    String hookEventName,

    @JsonProperty("permission_decision")
    PermissionDecision permissionDecision,

    @JsonProperty("permission_decision_reason")
    String permissionDecisionReason,

    @JsonProperty("updated_input")
    Object updatedInput,

    @JsonProperty("additional_context")
    String additionalContext
) {
    /**
     * Permission decision types.
     */
    public enum PermissionDecision {
        /** Allow the tool to execute */
        ALLOW,

        /** Deny the tool execution */
        DENY,

        /** Ask the user for confirmation */
        ASK,

        /** Defer the decision to another handler */
        DEFER
    }

    /**
     * Creates a HookOutput with ALLOW decision.
     */
    public static HookOutput allow(String hookEventName) {
        return new HookOutput(hookEventName, PermissionDecision.ALLOW, "Allowed", null, null);
    }

    /**
     * Creates a HookOutput with DENY decision.
     */
    public static HookOutput deny(String hookEventName, String reason) {
        return new HookOutput(hookEventName, PermissionDecision.DENY, reason, null, null);
    }

    /**
     * Creates a HookOutput with ASK decision.
     */
    public static HookOutput ask(String hookEventName, String reason) {
        return new HookOutput(hookEventName, PermissionDecision.ASK, reason, null, null);
    }

    /**
     * Creates a HookOutput with DEFER decision.
     */
    public static HookOutput defer(String hookEventName, String reason) {
        return new HookOutput(hookEventName, PermissionDecision.DEFER, reason, null, null);
    }

    /**
     * Creates a HookOutput with modified input.
     */
    public static HookOutput withUpdatedInput(String hookEventName, Object updatedInput) {
        return new HookOutput(hookEventName, PermissionDecision.ALLOW, "Input modified", updatedInput, null);
    }

    /**
     * Checks if the decision is ALLOW.
     */
    public boolean isAllowed() {
        return permissionDecision == PermissionDecision.ALLOW;
    }

    /**
     * Checks if the decision is DENY.
     */
    public boolean isDenied() {
        return permissionDecision == PermissionDecision.DENY;
    }
}
