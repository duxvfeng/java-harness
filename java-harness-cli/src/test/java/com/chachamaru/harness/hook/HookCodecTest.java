package com.chachamaru.harness.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HookCodecTest {
    @Test
    void testDecodeHookInput() throws Exception {
        String json = """
            {
              "session_id": "test-session",
              "hook_event_name": "PreToolUse",
              "tool_name": "Write",
              "tool_input": {"file_path": "/test.txt"}
            }
            """;

        HookInput input = HookCodec.decode(json);
        assertEquals("test-session", input.getSessionId());
        assertEquals("PreToolUse", input.getHookEventName());
        assertEquals("Write", input.getToolName());
    }

    @Test
    void testEncodeHookOutput() throws Exception {
        HookOutput output = HookOutput.allow();
        output.setHookEventName("PreToolUse");

        String json = HookCodec.encode(output);
        assertNotNull(json);
        assertTrue(json.contains("\"permissionDecision\":\"allow\""));
    }

    @Test
    void testRoundTrip() throws Exception {
        String originalJson = """
            {
              "session_id": "test",
              "hook_event_name": "PreToolUse",
              "tool_name": "Bash",
              "tool_input": {"command": "echo test"}
            }
            """;

        HookInput input = HookCodec.decode(originalJson);
        assertNotNull(input);
        assertEquals("test", input.getSessionId());
    }
}
