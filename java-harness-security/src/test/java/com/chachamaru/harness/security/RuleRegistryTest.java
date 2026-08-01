package com.chachamaru.harness.security;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.protocol.HookEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for RuleRegistry.
 */
class RuleRegistryTest {

    private RuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RuleRegistry();
    }

    @Test
    void testRegisterRule() {
        GuardrailRule rule = new SensitiveFileRule();
        registry.registerRule(rule);

        assertEquals(1, registry.getRuleCount());
    }

    @Test
    void testRegisterNullRule() {
        assertThrows(IllegalArgumentException.class, () -> {
            registry.registerRule(null);
        });
    }

    @Test
    void testUnregisterRule() {
        GuardrailRule rule = new SensitiveFileRule();
        registry.registerRule(rule);

        assertTrue(registry.unregisterRule("R01"));
        assertEquals(0, registry.getRuleCount());
    }

    @Test
    void testClearRules() {
        registry.registerRule(new SensitiveFileRule());
        registry.registerRule(new SensitiveFileRule());

        registry.clearRules();
        assertEquals(0, registry.getRuleCount());
    }

    @Test
    void testEvaluateWithNoRules() {
        GuardrailResult result = registry.evaluate(
                HookEventType.PRE_HOOK,
                "test-tool",
                Map.of("param", "value")
        );

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testEvaluateWithSensitiveFile() {
        registry.registerRule(new SensitiveFileRule());

        GuardrailResult result = registry.evaluate(
                HookEventType.PRE_HOOK,
                "Write",
                Map.of("file_path", "/etc/passwd")
        );

        assertNotNull(result);
        assertTrue(result.isBlocked());
        assertTrue(result.reason().contains("sensitive file access"));
    }

    @Test
    void testEvaluateWithNormalFile() {
        registry.registerRule(new SensitiveFileRule());

        GuardrailResult result = registry.evaluate(
                HookEventType.PRE_HOOK,
                "Write",
                Map.of("file_path", "/tmp/test.txt")
        );

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testEvaluateAsync() {
        registry.registerRule(new SensitiveFileRule());

        var future = registry.evaluateAsync(
                HookEventType.PRE_HOOK,
                "Write",
                Map.of("file_path", "/tmp/test.txt")
        );

        assertNotNull(future);
        assertDoesNotThrow(() -> future.get());
    }

    @Test
    void testShutdown() {
        registry.registerRule(new SensitiveFileRule());
        assertDoesNotThrow(() -> registry.shutdown());
        assertEquals(0, registry.getRuleCount());
    }
}
