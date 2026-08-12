package com.chachamaru.harness.hook;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

public class HookDispatcherTest {
    @Test
    void testHookDispatcherExecutes() throws Exception {
        HookDispatcher dispatcher = new HookDispatcher();
        assertDoesNotThrow(() -> dispatcher.process(new String[]{"pre-tool"}, new StringReader(""), new StringWriter()));
    }

    @Test
    void appliesRuntimeFloorThroughLegacyHookRoute() throws Exception {
        HookDispatcher dispatcher = new HookDispatcher();
        StringWriter output = new StringWriter();

        int exitCode = dispatcher.process(new String[]{"pre-tool"}, new StringReader("""
            {"session_id":"s1","tool_name":"Bash","tool_input":{"command":"curl https://example.com"},"cwd":"C:/repo"}
            """), output);

        assertEquals(2, exitCode);
        assertTrue(output.toString().contains("permissionDecision"));
        assertTrue(output.toString().contains("RUNTIME_FLOOR:egress"));
    }
}
