package com.chachamaru.harness.service.integration;

import com.chachamaru.harness.cli.guardrail.GuardrailEngine;
import com.chachamaru.harness.cli.guardrail.rules.R01NoSudo;
import com.chachamaru.harness.cli.guardrail.rules.R07CodexDirectWrite;
import com.chachamaru.harness.cli.handlers.PreToolUseHandler;
import com.chachamaru.harness.cli.hook.HookCodec;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import com.chachamaru.harness.cli.router.HookRouter;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the current CLI hook processing contracts.
 */
class HookProcessingTest {

    @Test
    void hookInputCanBeParsedFromJson() throws Exception {
        String json = """
            {
              "session_id": "session-1",
              "hook_event_name": "pre-tool-use",
              "tool_name": "Bash",
              "tool_input": {"command": "mvn test"},
              "cwd": "/project"
            }
            """;

        HookInput input = new HookCodec().parse(new StringReader(json));

        assertEquals("pre-tool-use", input.hookEventName());
        assertEquals("Bash", input.toolName());
        assertEquals("mvn test", input.toolInput().get("command"));
        assertTrue(input.isValid());
    }

    @Test
    void hookOutputCanBeSerialized() throws Exception {
        StringWriter writer = new StringWriter();

        new HookCodec().serialize(HookOutput.allow(), writer);

        assertTrue(writer.toString().contains("allow"));
    }

    @Test
    void guardrailRulesCanBeRegisteredAndRouted() throws Exception {
        GuardrailEngine engine = new GuardrailEngine();
        engine.registerRule(new R01NoSudo());
        engine.registerRule(new R07CodexDirectWrite());

        HookRouter router = new HookRouter();
        router.registerHandler(new PreToolUseHandler(engine));

        assertTrue(router.getRegistry().getHandlerCount() > 0);
        assertEquals("PreToolUse", router.route(validInput()).getEventName());
        assertNotNull(router.route(validInput()).handle(validInput()));
    }

    @Test
    void denyOutputContainsTheReason() {
        HookOutput output = HookOutput.deny("Access denied");

        assertEquals("deny", output.permissionDecision());
        assertEquals("Access denied", output.permissionDecisionReason());
    }

    @Test
    void invalidJsonIsRejected() {
        assertThrows(Exception.class,
            () -> new HookCodec().parse(new StringReader("{invalid json}")));
    }

    @Test
    void sudoCommandIsDeniedByGuardrail() {
        GuardrailEngine engine = new GuardrailEngine();
        engine.registerRule(new R01NoSudo());

        var result = engine.evaluate(new HookInput(
            "session-1", null, "/project", null, "pre-tool-use", "Bash",
            Map.of("command", "sudo rm -rf /"), null));

        assertTrue(result.isDenied());
        assertFalse(result.decision().reason().isBlank());
    }

    private HookInput validInput() {
        return new HookInput(
            "session-1", null, "/project", null, "PreToolUse", "Bash",
            Map.of("command", "mvn test"), null);
    }
}
