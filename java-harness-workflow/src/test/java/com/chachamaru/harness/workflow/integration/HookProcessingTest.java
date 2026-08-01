package com.chachamaru.harness.workflow.integration;

import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.foundation.dto.HookOutput;
import com.chachamaru.harness.protocol.HookEventType;
import com.chachamaru.harness.protocol.JacksonHookCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hook处理集成测试
 * 验证Hook编解码和基本处理功能
 */
class HookProcessingTest {

    private JacksonHookCodec codec;

    @BeforeEach
    void setUp() {
        codec = new JacksonHookCodec();
    }

    @Test
    void testHookInputEncoding() throws Exception {
        // 创建Hook输入
        HookInput input = new HookInput(
            "test-session-123",
            "/path/to/transcript.jsonl",
            "/project/java-harness",
            HookInput.PermissionMode.DEFAULT,
            "Bash",
            "{\"command\": \"echo test\"}",
            "/plugin/root"
        );

        // 编码测试
        String jsonInput = codec.encodeInput(input);

        assertNotNull(jsonInput);
        assertTrue(jsonInput.contains("Bash"));
        assertTrue(jsonInput.contains("test-session-123"));
        assertTrue(jsonInput.contains("echo test"));
    }

    @Test
    void testHookInputDecoding() throws Exception {
        // 创建JSON输入
        String jsonInput = """
            {
              "session_id": "test-session-456",
              "transcript_path": "/path/to/transcript.jsonl",
              "cwd": "/project/java-harness",
              "permission_mode": "AUTO",
              "tool_name": "Write",
              "tool_input": {"file_path": "/test.txt"},
              "plugin_root": "/plugin"
            }
            """;

        // 解码测试
        HookInput decodedInput = codec.decodeInput(jsonInput);

        assertNotNull(decodedInput);
        assertEquals("test-session-456", decodedInput.sessionId());
        assertEquals("Write", decodedInput.toolName());
        assertEquals(HookInput.PermissionMode.AUTO, decodedInput.permissionMode());
    }

    @Test
    void testHookOutputCreation() {
        // 创建Hook输出
        HookOutput output = HookOutput.allow("PRE_HOOK");

        assertNotNull(output);
        assertEquals("PRE_HOOK", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.ALLOW, output.permissionDecision());
        assertTrue(output.isAllowed());
        assertFalse(output.isDenied());
    }

    @Test
    void testHookOutputDeny() {
        // 创建拒绝输出
        HookOutput output = HookOutput.deny("PRE_HOOK", "Security rule violation");

        assertNotNull(output);
        assertEquals("PRE_HOOK", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.DENY, output.permissionDecision());
        assertEquals("Security rule violation", output.permissionDecisionReason());
        assertFalse(output.isAllowed());
        assertTrue(output.isDenied());
    }

    @Test
    void testHookOutputAsk() {
        // 创建询问输出
        HookOutput output = HookOutput.ask("GUARDRAIL_HOOK", "User confirmation needed");

        assertNotNull(output);
        assertEquals("GUARDRAIL_HOOK", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.ASK, output.permissionDecision());
        assertEquals("User confirmation needed", output.permissionDecisionReason());
    }

    @Test
    void testHookOutputWithUpdatedInput() {
        // 创建带修改输入的输出
        Object updatedInput = "{\"command\": \"echo safe-command\"}";
        HookOutput output = HookOutput.withUpdatedInput("PRE_HOOK", updatedInput);

        assertNotNull(output);
        assertEquals("PRE_HOOK", output.hookEventName());
        assertEquals(HookOutput.PermissionDecision.ALLOW, output.permissionDecision());
        assertEquals("Input modified", output.permissionDecisionReason());
        assertEquals(updatedInput, output.updatedInput());
    }

    @Test
    void testOutputEncoding() throws Exception {
        // 创建输出
        HookOutput output = new HookOutput(
            "POST_HOOK",
            HookOutput.PermissionDecision.DENY,
            "Security rule violation",
            null,
            "Blocked by guardrail R01"
        );

        // 编码
        String jsonOutput = codec.encodeOutput(output);

        assertNotNull(jsonOutput);
        assertTrue(jsonOutput.contains("DENY"));
        assertTrue(jsonOutput.contains("Security rule violation"));
        assertTrue(jsonOutput.contains("Blocked by guardrail R01"));
    }

    @Test
    void testOutputDecoding() throws Exception {
        // JSON输出
        String jsonOutput = """
            {
              "hook_event_name": "ERROR_HOOK",
              "permission_decision": "ALLOW",
              "permission_decision_reason": null,
              "updated_input": null,
              "additional_context": "Error handled successfully"
            }
            """;

        // 解码
        HookOutput decodedOutput = codec.decodeOutput(jsonOutput);

        assertNotNull(decodedOutput);
        assertEquals("ERROR_HOOK", decodedOutput.hookEventName());
        assertEquals(HookOutput.PermissionDecision.ALLOW, decodedOutput.permissionDecision());
        assertNull(decodedOutput.permissionDecisionReason());
        assertEquals("Error handled successfully", decodedOutput.additionalContext());
    }

    @Test
    void testAllHookEventTypes() throws Exception {
        // 测试所有Hook事件类型
        HookEventType[] eventTypes = {
            HookEventType.PRE_HOOK,
            HookEventType.POST_HOOK,
            HookEventType.GUARDRAIL_HOOK,
            HookEventType.PRE_COMMIT_HOOK,
            HookEventType.POST_COMMIT_HOOK,
            HookEventType.PRE_REVIEW_HOOK,
            HookEventType.POST_REVIEW_HOOK,
            HookEventType.ERROR_HOOK
        };

        for (HookEventType eventType : eventTypes) {
            HookOutput output = HookOutput.allow(eventType.name());
            assertEquals(eventType.name(), output.hookEventName());
            assertTrue(output.isAllowed());
        }
    }

    @Test
    void testPermissionModes() throws Exception {
        // 测试所有权限模式
        HookInput.PermissionMode[] modes = {
            HookInput.PermissionMode.DEFAULT,
            HookInput.PermissionMode.AUTO,
            HookInput.PermissionMode.BYPASS_PERMISSIONS
        };

        for (HookInput.PermissionMode mode : modes) {
            HookInput input = new HookInput(
                "test-session",
                "/path/to/transcript.jsonl",
                "/project/java-harness",
                mode,
                "Bash",
                "{\"command\": \"echo test\"}",
                "/plugin/root"
            );

            String encoded = codec.encodeInput(input);
            HookInput decoded = codec.decodeInput(encoded);

            assertEquals(mode, decoded.permissionMode());
        }
    }

    @Test
    void testComplexToolInput() throws Exception {
        // 测试复杂的tool_input
        String complexToolInput = """
            {
              "file_path": "/project/test.txt",
              "content": "Hello World\\nLine 2\\nLine 3",
              "encoding": "utf-8",
              "options": {
                "create_parent_dirs": true,
                "overwrite": true
              }
            }
            """;

        HookInput input = new HookInput(
            "test-session",
            "/path/to/transcript.jsonl",
            "/project/java-harness",
            HookInput.PermissionMode.DEFAULT,
            "Write",
            complexToolInput,
            "/plugin/root"
        );

        String encoded = codec.encodeInput(input);
        HookInput decoded = codec.decodeInput(encoded);

        assertEquals("Write", decoded.toolName());
        assertTrue(decoded.toolInput().contains("Hello World"));
        assertTrue(decoded.toolInput().contains("create_parent_dirs"));
    }

    @Test
    void testErrorHandling() {
        // 测试错误处理
        assertThrows(Exception.class, () -> {
            codec.decodeInput("invalid json");
        });

        assertThrows(Exception.class, () -> {
            codec.decodeInput("{\"incomplete\": ");
        });
    }

    @Test
    void testUnicodeAndSpecialCharacters() throws Exception {
        // 测试Unicode和特殊字符
        HookInput input = new HookInput(
            "test-session-unicode-测试",
            "/path/测试/transcript.jsonl",
            "/project/项目",
            HookInput.PermissionMode.DEFAULT,
            "Bash",
            "{\"command\": \"echo 'Hello 世界 🌍'\"}",
            "/plugin/root"
        );

        String encoded = codec.encodeInput(input);
        HookInput decoded = codec.decodeInput(encoded);

        assertEquals("test-session-unicode-测试", decoded.sessionId());
        assertTrue(decoded.toolInput().contains("世界"));
        assertTrue(decoded.toolInput().contains("🌍"));
    }

    @Test
    void testAllPermissionDecisions() {
        // 测试所有权限决策类型
        HookOutput allowOutput = HookOutput.allow("PRE_HOOK");
        assertEquals(HookOutput.PermissionDecision.ALLOW, allowOutput.permissionDecision());

        HookOutput denyOutput = HookOutput.deny("PRE_HOOK", "Denied");
        assertEquals(HookOutput.PermissionDecision.DENY, denyOutput.permissionDecision());

        HookOutput askOutput = HookOutput.ask("PRE_HOOK", "Ask user");
        assertEquals(HookOutput.PermissionDecision.ASK, askOutput.permissionDecision());

        HookOutput deferOutput = HookOutput.defer("PRE_HOOK", "Defer to other");
        assertEquals(HookOutput.PermissionDecision.DEFER, deferOutput.permissionDecision());
    }
}