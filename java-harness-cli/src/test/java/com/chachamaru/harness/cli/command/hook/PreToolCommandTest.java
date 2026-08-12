package com.chachamaru.harness.cli.command.hook;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PreToolCommand.
 */
class PreToolCommandTest {

    @Test
    void testExecution() throws Exception {
        PreToolCommand command = new PreToolCommand();
        assertDoesNotThrow(() -> command.execute(new StringReader(""), new StringWriter(), null));
    }

    @Test
    void blocksDangerousCommandWithCodexEnvelope() throws Exception {
        PreToolCommand command = new PreToolCommand();
        StringWriter output = new StringWriter();

        int exitCode = command.execute(new StringReader("""
            {"conversation_id":"c1","tool_name":"Bash","tool_input":{"command":"curl https://example.com"},"cwd":"C:/repo"}
            """), output, "codex");

        assertEquals(2, exitCode);
        assertTrue(output.toString().contains("permissionDecision"));
        assertTrue(output.toString().contains("RUNTIME_FLOOR:egress"));
    }

    @Test
    void allowsSafeCommandWithoutOutput() throws Exception {
        PreToolCommand command = new PreToolCommand();
        StringWriter output = new StringWriter();

        int exitCode = command.execute(new StringReader("""
            {"session_id":"s1","tool_name":"Bash","tool_input":{"command":"git status"},"cwd":"C:/repo"}
            """), output, "claude");

        assertEquals(0, exitCode);
        assertTrue(output.toString().isEmpty());
    }
}
