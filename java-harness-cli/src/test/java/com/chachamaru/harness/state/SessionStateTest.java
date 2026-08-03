package com.chachamaru.harness.state;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class SessionStateTest {
    @Test
    void testCreateSessionState() {
        SessionState state = new SessionState();
        assertNotNull(state);
        assertNotNull(state.getSessionId());
        assertNotNull(state.getStartTime());
    }

    @Test
    void testSetAndGetAttributes() {
        SessionState state = new SessionState();
        state.setAttribute("cwd", "/test/path");
        state.setAttribute("backend", "codex");

        assertEquals("/test/path", state.getAttribute("cwd"));
        assertEquals("codex", state.getAttribute("backend"));
    }

    @Test
    void testSessionDuration() {
        SessionState state = new SessionState();
        // Just test that duration can be calculated
        long duration = state.getDuration();
        assertTrue(duration >= 0);
    }

    @Test
    void testToJson() {
        SessionState state = new SessionState();
        state.setAttribute("test_key", "test_value");

        String json = state.toJson();
        assertNotNull(json);
        assertTrue(json.contains("test_key"));
        assertTrue(json.contains("test_value"));
    }

    @Test
    void testFromJson() {
        SessionState original = new SessionState();
        original.setAttribute("cwd", "/test");

        String json = original.toJson();
        SessionState restored = SessionState.fromJson(json);

        assertEquals(original.getSessionId(), restored.getSessionId());
        assertEquals("/test", restored.getAttribute("cwd"));
    }

    @Test
    void testIsActive() {
        SessionState state = new SessionState();
        assertTrue(state.isActive());

        state.close();
        assertFalse(state.isActive());
    }
}
