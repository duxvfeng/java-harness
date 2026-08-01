package com.chachamaru.harness.cli.hook;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

class HookCodecTest {

    @Test
    void testParseHookInput() throws Exception {
        String json = """
            {
                "session_id": "test-session",
                "transcript_path": "/path/to/transcript",
                "cwd": "/project",
                "permission_mode": "default",
                "hook_event_name": "PreToolUse",
                "tool_name": "Write",
                "tool_input": {
                    "file_path": "/project/test.txt",
                    "content": "hello"
                },
                "plugin_root": "/plugin"
            }
            """;

        HookCodec codec = new HookCodec();
        HookInput input = codec.parse(new StringReader(json));

        assertEquals("test-session", input.sessionId());
        assertEquals("PreToolUse", input.hookEventName());
        assertEquals("Write", input.toolName());
        assertEquals("/project/test.txt", input.toolInput().get("file_path"));
    }

    @Test
    void testSerializeHookOutput() throws Exception {
        HookOutput output = new HookOutput(
            "PreToolUse",
            "allow",
            null,
            null
        );

        HookCodec codec = new HookCodec();
        StringWriter writer = new StringWriter();
        codec.serialize(output, writer);

        String json = writer.toString();
        assertTrue(json.contains("\"permissionDecision\":\"allow\""));
        assertTrue(json.contains("\"hookEventName\":\"PreToolUse\""));
    }
}
