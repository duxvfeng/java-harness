package com.chachamaru.harness.security;

import com.chachamaru.harness.foundation.dto.GuardrailResult;
import com.chachamaru.harness.protocol.HookEventType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for GuardrailRule interface (using SensitiveFileRule implementation).
 */
class GuardrailRuleTest {

    @Test
    void testSensitiveFileRuleId() {
        GuardrailRule rule = new SensitiveFileRule();
        assertEquals("R01", rule.getId());
    }

    @Test
    void testSensitiveFileRuleDescription() {
        GuardrailRule rule = new SensitiveFileRule();
        assertNotNull(rule.getDescription());
        assertFalse(rule.getDescription().isBlank());
    }

    @Test
    void testSensitiveFileRuleMatches() {
        GuardrailRule rule = new SensitiveFileRule();
        assertTrue(rule.matches(HookEventType.PRE_HOOK));
        assertTrue(rule.matches(HookEventType.POST_HOOK));
    }

    @Test
    void testSensitiveFileRulePriority() {
        GuardrailRule rule = new SensitiveFileRule();
        assertEquals(100, rule.getPriority());
    }

    @Test
    void testSensitiveFileRuleEnabled() {
        GuardrailRule rule = new SensitiveFileRule();
        assertTrue(rule.isEnabled());
    }

    @Test
    void testSensitiveFileRuleEvaluateDeniesPasswordAccess() {
        GuardrailRule rule = new SensitiveFileRule();
        GuardrailResult result = rule.evaluate(
                HookEventType.PRE_HOOK,
                "Write",
                Map.of("file_path", "/etc/password")
        );

        assertNotNull(result);
        assertTrue(result.isBlocked());
        assertTrue(result.reason().contains("sensitive"));
    }

    @Test
    void testSensitiveFileRuleEvaluateAllowsNormalAccess() {
        GuardrailRule rule = new SensitiveFileRule();
        GuardrailResult result = rule.evaluate(
                HookEventType.PRE_HOOK,
                "Write",
                Map.of("file_path", "/tmp/normal.txt")
        );

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testSensitiveFileRuleEvaluateWithNullInput() {
        GuardrailRule rule = new SensitiveFileRule();
        GuardrailResult result = rule.evaluate(
                HookEventType.PRE_HOOK,
                "Write",
                null
        );

        assertNotNull(result);
        assertTrue(result.isAllowed());
    }

    @Test
    void testSensitiveFileRuleEvaluateWithNonFileTool() {
        GuardrailRule rule = new SensitiveFileRule();
        GuardrailResult result = rule.evaluate(
                HookEventType.PRE_HOOK,
                "SomeOtherTool",
                Map.of("sensitive", "password")
        );

        assertNotNull(result);
        assertTrue(result.isAllowed()); // Non-file tool, so rule doesn't apply
    }
}
