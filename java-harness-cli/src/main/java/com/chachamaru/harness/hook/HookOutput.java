package com.chachamaru.harness.hook;

/**
 * Hook output response model.
 * Represents the response to a hook event.
 */
public class HookOutput {
    private String hookEventName;
    private String permissionDecision;  // "allow" or "deny"
    private String permissionDecisionReason;
    private String additionalContext;

    public static HookOutput allow() {
        HookOutput output = new HookOutput();
        output.setPermissionDecision("allow");
        return output;
    }

    public static HookOutput deny(String reason) {
        HookOutput output = new HookOutput();
        output.setPermissionDecision("deny");
        output.setPermissionDecisionReason(reason);
        return output;
    }

    // Getters and setters
    public String getHookEventName() { return hookEventName; }
    public void setHookEventName(String hookEventName) { this.hookEventName = hookEventName; }

    public String getPermissionDecision() { return permissionDecision; }
    public void setPermissionDecision(String permissionDecision) { this.permissionDecision = permissionDecision; }

    public String getPermissionDecisionReason() { return permissionDecisionReason; }
    public void setPermissionDecisionReason(String permissionDecisionReason) { this.permissionDecisionReason = permissionDecisionReason; }

    public String getAdditionalContext() { return additionalContext; }
    public void setAdditionalContext(String additionalContext) { this.additionalContext = additionalContext; }
}
