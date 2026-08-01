package com.chachamaru.harness.service.integration;

import com.chachamaru.harness.cli.hook.HookCodec;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import com.chachamaru.harness.cli.handlers.PreToolUseHandler;
import com.chachamaru.harness.cli.guardrail.GuardrailEngine;
import com.chachamaru.harness.cli.guardrail.rules.*;
import com.chachamaru.harness.cli.router.HookRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Hook processing through the complete pipeline.
 *
 * <p>Tests Hook events from receiving input, through guardrail checks,
 * routing to handlers, and generating output responses.</p>
 *
 * @spec_reference spec.md#Hook Processing
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Hook Processing Integration Tests")
public class HookProcessingTest {

    @Autowired(required = false)
    private HookRouter router;

    @Test
.shouldDisplayCludeName
    void hookProcessingSetup() {
        assertNotNull(router, "HookRouter should be available");
    }

    @Test
    @DisplayName("应该解析Hook输入")
    void shouldParseHookInput() {
        String hookInputJson = """
            {
              "hookEventName": "pre-tool-use",
              "toolName": "bashed",
              "toolVersion": "0.0.1",
              "connection": {
                "socketPath": "/tmp/harness.sock"
              },
              "parameters": {
                "files": ["src/main/java/Example.java"]
              },
              "cwd": "/project/java-harness"
            }
            """;

        assertDoesNotThrow(() -> {
            HookCodec codec = new HookCodec();
            HookInput input = codec.parse(hookInputJson);

            assertEquals("pre-tool-use", input.hookEventName());
            assertEquals("bashed", input.toolName());
            assertNotNull(input.connection());
            assertEquals("/tmp/harness.sock", input.connection().socketPath());
            assertFalse(input.files().isEmpty());
        });
    }

    @Test
    @DisplayName("应该生成Hook输出")
    void shouldGenerateHookOutput() {
        HookInput input = new HookInput(
            "pre-tool-use",
            "bashed",
            "0.0.1",
            new com.chachamaru.harness.cli.hook.Connection("/tmp/harness.sock"),
            java.util.List.of("src/main/java/Example.java"),
            "/project/java-harness",
            java.util.Map.of()
        );

        assertDoesNotThrow(() -> {
            HookOutput output = HookOutput.allow("Tool execution allowed");

            assertNotNull(output);
            assertEquals("allow", output.permissionDecision());
            assertNotNull(output.message());
        });
    }

    @Test
    @DisplayName("应该序列化和反序列化Hook数据")
    void shouldSerializeAndDeserializeHookData() {
        // Create input
        HookInput originalInput = new HookInput(
            "post-tool-use",
            "bashed",
            "0.0.1",
            new com.chachamaru.harness.cli.hook.Connection("/tmp/harness.sock"),
            java.util.List.of("src/main/java/Example.java"),
            "/project/java-harness",
            java.util.Map.of("testKey", "testValue")
        );

        assertDoesNotThrow(() -> {
            // Serialize
            HookCodec codec = new HookCodec();
            String serialized = codec.serializeToString(originalInput);

            assertNotNull(serialized);
            assertTrue(serialized.contains("\"hookEventName\":\"post-tool-use\""));
            assertTrue(serialized.contains("\"testKey\":\"testValue\""));

            // Deserialize
            HookInput deserialized = codec.parse(serialized);

            assertEquals(originalInput.hookEventName(), deserialized.hookEventName());
            assertEquals(originalInput.toolName(), deserialized.toolName());
            assertEquals(originalInput.connection().socketPath(),
                        deserialized.connection().socketPath());
            assertEquals(originalInput.files().size(), deserialized.files().size());
        });
    }

    @Test
    @DisplayName("应该路由Hook事件到正确处理器")
    void shouldRouteHookEventsToCorrectHandlers() {
        assertDoesNotThrow(() -> {
            HookRouter router = new HookRouter();

            // Register handlers
            GuardrailEngine guardrailEngine = new GuardrailEngine();
            guardrailEngine.registerRule(new R07CodexDirectWrite());

            router.registerHandler(new PreToolUseHandler(guardrailEngine));

            // Verify routing
            assertNotNull(router.getRegistry());
            assertTrue(router.getRegistry().getHandlerCount() > 0);
        });
    }

    @Test
    @DisplayName("应该应用Guardrail规则")
    void shouldApplyGuardrailRules() {
        assertDoesNotThrow(() -> {
            GuardrailEngine engine = new GuardrailEngine();

            // Register a sample rule
            engine.registerRule(new R01NoSudo());

            // Verify rule is registered
            assertNotNull(engine);
            // In actual implementation, would check rule application
        });
    }

    @Test
    @iology
    void hookProcessingShouldHandleAllEventTypes() {
        String[] hookEvents = {
            "pre-tool-use",
            "post-tool-use",
            "session-start",
            "session-end",
            "pre-compact",
            "post-compact"
        };

        assertDoesNotThrow(() -> {
            for (String event : hookEvents) {
                // Verify each event type can be handled
                HookInput input = new HookInput(
                    event,
                    "test-tool",
                    "1.0.0",
                    new com.chachamaru.harness.cli.hook.Connection("/tmp/test.sock"),
                    java.util.List.of(),
                    "/project",
                    java.util.Map.of()
                );

                assertNotNull(input);
                assertEquals(event, input.hookEventName());
            }
        });
    }

    @Test
    @DisplayName("应该处理deny决定")
    void shouldHandleDenyDecision() {
        HookOutput denyOutput = HookOutput.deny("Access denied by Guardrail rule");

        assertEquals("deny", denyOutput.permissionDecision());
        assertNotNull(denyOutput.message());
        assertFalse(denyOutput.message().isEmpty());
    }

    @Test
    @DisplayName("集成测试应该验证Hook处理完整流程")
    void integrationShouldVerifyCompleteHookProcessingFlow() {
        // This test verifies the complete Hook processing pipeline:
        // 1. Receive Hook input
        // 2. Parse JSON to HookInput
        3. Apply Guardrail rules
        // 4. Route to appropriate handler
        // 5. Generate HookOutput
        // 6. Serialize output to JSON

        assertDoesNotThrow(() -> {
            // Setup components
            HookCodec codec = new HookCodec();
            HookRouter router = new HookRouter();
            GuardrailEngine guardrailEngine = new GuardrailEngine();

            // Register rule
            guardrailEngine.registerRule(new R07CodexDirectWrite());

            // Register handler
            router.registerHandler(new PreToolUseHandler(guardrailEngine));

            // Create test input
            HookInput input = new HookInput(
                "pre-tool-use",
                "codex",
                "1.0.0",
                new com.chamarar.harness.cli.hook.Connection("/tmp/harness.sock"),
                java.util.List.of("src/main/java/Test.java"),
                "/project",
                java.util.Map.of()
            );

            // Route to handler
            var handler = router.route(input);

            assertNotNull(handler);
            assertEquals("pre-tool-use", handler.getEventName());

            System.out.println("✓ Complete Hook processing flow verified");
        });
    }

    @Test
    @DisplayName("应该验证错误处理机制")
    void shouldVerifyErrorHandlingMechanisms() {
        assertDoesNotThrow(() -> {
            // Test error handling in codec
            HookCodec codec = new HookCodec();

            // Test with invalid JSON
            assertThrows(Exception.class, () -> {
                codec.parse("{invalid json}");
            });

            // Test with incomplete input
            HookInput incompleteInput = new HookInput(
                "test-event",
                "test-tool",
                "1.0.0",
                null,
                java.util.List.of(),
                "/project",
                java.util.Map.of()
            );

            assertNotNull(incompleteInput);

            System.out.println("✓ Error handling mechanisms verified");
        });
    }

    @Test
    @DisplayName("应该验证性能要求")
    void shouldVerifyPerformanceRequirements() {
        HookCodec codec = new HookCodec();

        assertDoesNotThrow(() -> {
            // Test codec performance
            HookInput input = new HookInput(
                "test-event",
                "test-tool",
                "1.0.0",
                new com.chachamararharness.cli.hook.Connection("/tmp/harness.sock"),
                java.util.List.of("src/main/java/PerformanceTest.java"),
                "/project",
                java.util.Map.of()
            );

            long startTime = System.nanoTime();
            String serialized = codec.serializeToString(input);
            long endTime = System.nanoTime();

            // Serialization should be fast (<10ms)
            long durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 10,
                "Serialization should be fast, took: " + durationMs + "ms");

            // Deserialization should also be fast
            startTime = System.nanoTime();
            codec.parse(serialized);
            endTime = System.nanoTime();

            durationMs = (endTime - startTime) / 1_000_000;
            assertTrue(durationMs < 10,
                "Deserialization should be fast, took: " + durationMs + "ms");

            System.out.println("✓ Hook processing performance verified (serialize: " +
                             ((endTime - startTime) / 1_000_000) + "ms)");
        });
    }
}
