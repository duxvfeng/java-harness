package com.chachamaru.harness.foundation.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for HookInput DTO.
 */
@DisplayName("HookInput Tests")
class HookInputTest {

    @Test
    @DisplayName("Should create HookInput with all fields")
    void shouldCreateHookInputWithAllFields() {
        HookInput input = new HookInput(
            "session-123",
            "/tmp/transcript.json",
            "/workspace",
            "bypassPermissions",
            "PreToolUse",
            "Write",
            Map.of("file_path", "/tmp/test.txt"),
            "/plugin/root"
        );

        assertEquals("session-123", input.sessionId());
        assertEquals("/tmp/transcript.json", input.transcriptPath());
        assertEquals("/workspace", input.cwd());
        assertEquals("bypassPermissions", input.permissionMode());
        assertEquals("PreToolUse", input.hookEventName());
        assertEquals("Write", input.toolName());
    }

    @Test
    @DisplayName("Should throw exception when sessionId is null")
    void shouldThrowExceptionWhenSessionIdIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new HookInput(
            null,
            "/tmp/transcript.json",
            "/workspace",
            "bypassPermissions",
            "PreToolUse",
            "Write",
            Map.of(),
            "/plugin/root"
        ));
    }

    @Test
    @DisplayName("Should throw exception when sessionId is blank")
    void shouldThrowExceptionWhenSessionIdIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new HookInput(
            "   ",
            "/tmp/transcript.json",
            "/workspace",
            "bypassPermissions",
            "PreToolUse",
            "Write",
            Map.of(),
            "/plugin/root"
        ));
    }

    @Test
    @DisplayName("Should throw exception when hookEventName is null")
    void shouldThrowExceptionWhenHookEventNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> new HookInput(
            "session-123",
            "/tmp/transcript.json",
            "/workspace",
            "bypassPermissions",
            null,
            "Write",
            Map.of(),
            "/plugin/root"
        ));
    }

    @Test
    @DisplayName("Should throw exception when hookEventName is blank")
    void shouldThrowExceptionWhenHookEventNameIsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new HookInput(
            "session-123",
            "/tmp/transcript.json",
            "/workspace",
            "bypassPermissions",
            "",
            "Write",
            Map.of(),
            "/plugin/root"
        ));
    }

    @Test
    @DisplayName("Should create test HookInput successfully")
    void shouldCreateTestHookInputSuccessfully() {
        HookInput input = HookInput.createForTest("PreToolUse", "Write");

        assertEquals("test-session", input.sessionId());
        assertEquals("PreToolUse", input.hookEventName());
        assertEquals("Write", input.toolName());
        assertEquals("/tmp", input.cwd());
    }

    @Test
    @DisplayName("Should allow null values for optional fields")
    void shouldAllowNullValuesForOptionalFields() {
        HookInput input = new HookInput(
            "session-123",
            null,
            null,
            null,
            "PreToolUse",
            null,
            null,
            null
        );

        assertEquals("session-123", input.sessionId());
        assertEquals("PreToolUse", input.hookEventName());
        assertNull(input.transcriptPath());
        assertNull(input.cwd());
    }
}
