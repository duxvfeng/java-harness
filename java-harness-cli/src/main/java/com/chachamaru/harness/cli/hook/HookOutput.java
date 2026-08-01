package com.chachamaru.harness.cli.hook;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Hook output to Claude Code (stdout)
 */
public record HookOutput(
    @JsonProperty("hookEventName")
    String hookEventName,

    @JsonProperty("permissionDecision")
    String permissionDecision,

    @JsonProperty("permissionDecisionReason")
    String permissionDecisionReason,

    @JsonProperty("additionalContext")
    String additionalContext
) {
    public static HookOutput allow() {
        return new HookOutput(null, "allow", null, null);
    }

    public static HookOutput deny(String reason) {
        return new HookOutput(null, "deny", reason, null);
    }

    public static HookOutput defer(String reason) {
        return new HookOutput(null, "defer", reason, null);
    }
}
