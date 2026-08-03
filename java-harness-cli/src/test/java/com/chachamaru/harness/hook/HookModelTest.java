package com.chachamaru.harness.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HookModelTest {
    @Test
    void testHookInputSettersGetters() {
        HookInput input = new HookInput();
        input.setSessionId("test-session");
        input.setHookEventName("PreToolUse");
        input.setToolName("Write");

        assertEquals("test-session", input.getSessionId());
        assertEquals("PreToolUse", input.getHookEventName());
        assertEquals("Write", input.getToolName());
    }

    @Test
    void testHookOutputAllow() {
        HookOutput output = HookOutput.allow();
        assertEquals("allow", output.getPermissionDecision());
        assertNull(output.getPermissionDecisionReason());
    }

    @Test
    void testHookOutputDeny() {
        HookOutput output = HookOutput.deny("Test reason");
        assertEquals("deny", output.getPermissionDecision());
        assertEquals("Test reason", output.getPermissionDecisionReason());
    }
}
