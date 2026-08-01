package com.chachamaru.harness.cli.router;

import com.chachamaru.harness.cli.handlers.HookHandler;
import com.chachamaru.harness.cli.handlers.PreToolUseHandler;
import com.chachamaru.harness.cli.handlers.SessionStartHandler;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.guardrail.GuardrailEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

class HookRouterTest {

    private HookRouter router;

    @BeforeEach
    void setUp() {
        router = new HookRouter();
        GuardrailEngine guardrailEngine = new GuardrailEngine();
        router.registerHandler(new PreToolUseHandler(guardrailEngine));
        router.registerHandler(new SessionStartHandler());
    }

    @Test
    void testRoutePreToolUse() {
        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            "/project",
            "default",
            "PreToolUse",
            "Write",
            Map.of("file_path", "/project/test.txt"),
            "/plugin"
        );

        HookHandler handler = router.route(input);
        assertNotNull(handler);
        assertEquals("PreToolUseHandler", handler.getClass().getSimpleName());
    }

    @Test
    void testRouteSessionStart() {
        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            "/project",
            "default",
            "SessionStart",
            null,
            Map.of(),
            "/plugin"
        );

        HookHandler handler = router.route(input);
        assertNotNull(handler);
        assertEquals("SessionStartHandler", handler.getClass().getSimpleName());
    }

    @Test
    void testRouteUnknownEvent() {
        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            "/project",
            "default",
            "UnknownEvent",
            "SomeTool",
            Map.of(),
            "/plugin"
        );

        assertThrows(IllegalStateException.class, () -> router.route(input));
    }
}
