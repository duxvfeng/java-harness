package com.chachamaru.harness.protocol;

import com.chachamaru.harness.foundation.dto.HookInput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MultiHostHookCodecTest {

    @Test
    void normalizesCursorShellPayload() throws Exception {
        String json = """
            {
              "conversation_id": "cursor-1",
              "hook_event_name": "preToolUse",
              "command": "git status",
              "workspace_roots": ["C:/repo"]
            }
            """;

        MultiHostHookCodec.NormalizedInput normalized = MultiHostHookCodec.normalize(json, "");

        assertEquals(MultiHostHookCodec.Host.CURSOR, normalized.host());
        assertEquals("cursor-1", normalized.input().sessionId());
        assertEquals("Bash", normalized.input().toolName());
        assertEquals("git status", normalized.input().toolInput().get("command"));
        assertEquals("C:/repo", normalized.input().cwd());
    }

    @Test
    void normalizesCodexPayloadAndPreservesExplicitToolInput() throws Exception {
        String json = """
            {
              "conversation_id": "codex-1",
              "tool_name": "Bash",
              "tool_input": {"command": "git diff", "timeout": 30},
              "cwd": "C:/repo"
            }
            """;

        MultiHostHookCodec.NormalizedInput normalized = MultiHostHookCodec.normalize(json, "codex");

        assertEquals(MultiHostHookCodec.Host.CODEX, normalized.host());
        assertEquals("codex-1", normalized.input().sessionId());
        assertEquals("git diff", normalized.input().toolInput().get("command"));
        assertEquals(30, normalized.input().toolInput().get("timeout"));
    }

    @Test
    void rendersHostSpecificDenyOutputs() throws Exception {
        String claude = MultiHostHookCodec.denyOutput(MultiHostHookCodec.Host.CLAUDE, "blocked");
        String codex = MultiHostHookCodec.denyOutput(MultiHostHookCodec.Host.CODEX, "blocked");
        String cursor = MultiHostHookCodec.denyOutput(MultiHostHookCodec.Host.CURSOR, "blocked");

        assertTrue(claude.contains("\"hookSpecificOutput\""));
        assertTrue(claude.contains("\"permissionDecision\" : \"deny\"")
            || claude.contains("\"permissionDecision\":\"deny\""));
        assertTrue(codex.contains("\"permissionDecisionReason\""));
        assertTrue(cursor.contains("\"permission\""));
        assertTrue(cursor.contains("\"agent_message\""));
    }

    @Test
    void rejectsPayloadWithoutAction() {
        MultiHostHookCodec.HookCodecException error = assertThrows(
            MultiHostHookCodec.HookCodecException.class,
            () -> MultiHostHookCodec.normalize("{\"session_id\":\"s\"}", "claude"));

        assertTrue(error.getMessage().contains("tool_name"));
    }
}
