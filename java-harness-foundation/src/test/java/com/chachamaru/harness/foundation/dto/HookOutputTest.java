package com.chachamaru.harness.foundation.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HookOutput DTO.
 */
@DisplayName("HookOutput Tests")
class HookOutputTest {

    @Test
    @DisplayName("Should create ALLOW output")
    void shouldCreateAllowOutput() {
        HookOutput output = HookOutput.allow("PreToolUse");

        assertEquals("PreToolUse", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.ALLOW, output.permissionDecision());
        assertEquals("Allowed", output.permissionDecisionReason());
        assertTrue(output.isAllowed());
        assertFalse(output.isDenied());
    }

    @Test
    @DisplayName("Should create DENY output")
    void shouldCreateDenyOutput() {
        HookOutput output = HookOutput.deny("PreToolUse", "Operation not permitted");

        assertEquals("PreToolUse", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.DENY, output.permissionDecision());
        assertEquals("Operation not permitted", output.permissionDecisionReason());
        assertFalse(output.isAllowed());
        assertTrue(output.isDenied());
    }

    @Test
    @DisplayName("Should create ASK output")
    void shouldCreateAskOutput() {
        HookOutput output = HookOutput.ask("PreToolUse", "User confirmation required");

        assertEquals("PreToolUse", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.ASK, output.permissionDecision());
        assertEquals("User confirmation required", output.permissionDecisionReason());
        assertFalse(output.isAllowed());
        assertFalse(output.isDenied());
    }

    @Test
    @DisplayName("Should create DEFER output")
    void shouldCreateDeferOutput() {
        HookOutput output = HookOutput.defer("PreToolUse", "Deferring to next handler");

        assertEquals("PreToolUse", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.DEFER, output.permissionDecision());
        assertEquals("Deferring to next handler", output.permissionDecisionReason());
        assertFalse(output.isAllowed());
        assertFalse(output.isDenied());
    }

    @Test
    @DisplayName("Should create output with updated input")
    void shouldCreateOutputWithUpdatedInput() {
        Object updatedInput = Map.of("file_path", "/modified/path.txt");
        HookOutput output = HookOutput.withUpdatedInput("PreToolUse", updatedInput);

        assertEquals("PreToolUse", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.ALLOW, output.permissionDecision());
        assertEquals("Input modified", output.permissionDecisionReason());
        assertEquals(updatedInput, output.updatedInput());
        assertTrue(output.isAllowed());
    }

    @Test
    @DisplayName("Should test all permission decision types")
    void shouldTestAllPermissionDecisionTypes() {
        assertEquals(4, HookOutput.PermissionDecision.values().length);
        assertEquals(HookOutput.PermissionDecision.ALLOW, HookOutput.PermissionDecision.valueOf("ALLOW"));
        assertEquals(HookOutput.PermissionDecision.DENY, HookOutput.PermissionDecision.valueOf("DENY"));
        assertEquals(HookOutput.PermissionDecision.ASK, HookOutput.PermissionDecision.valueOf("ASK"));
        assertEquals(HookOutput.PermissionDecision.DEFER, HookOutput.PermissionDecision.valueOf("DEFER"));
    }

    @Test
    @DisplayName("Should create output with null optional fields")
    void shouldCreateOutputWithNullOptionalFields() {
        HookOutput output = new HookOutput(
            "PreToolUse",
            HookOutput.PermissionDecision.ALLOW,
            null,
            null,
            null
        );

        assertEquals("PreToolUse", output.hookEventName());
        assertNull(output.permissionDecisionReason());
        assertNull(output.updatedInput());
        assertNull(output.additionalContext());
    }
}
