package com.chachamaru.harness.foundation.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

/**
 * Unit tests for GuardrailResult DTO.
 */
@DisplayName("GuardrailResult Tests")
class GuardrailResultTest {

    @Test
    @DisplayName("Should create ALLOW result")
    void shouldCreateAllowResult() {
        GuardrailResult result = GuardrailResult.allow("R01");

        assertEquals(GuardrailResult.Decision.ALLOW, result.decision());
        assertEquals("R01", result.ruleId());
        assertEquals("Allowed", result.reason());
        assertFalse(result.block());
        assertTrue(result.isAllowed());
        assertFalse(result.isBlocked());
    }

    @Test
    @DisplayName("Should create DENY result with blocking")
    void shouldCreateDenyResultWithBlocking() {
        GuardrailResult result = GuardrailResult.deny("R01", "Dangerous operation detected");

        assertEquals(GuardrailResult.Decision.DENY, result.decision());
        assertEquals("R01", result.ruleId());
        assertEquals("Dangerous operation detected", result.reason());
        assertTrue(result.block());
        assertFalse(result.isAllowed());
        assertTrue(result.isBlocked());
    }

    @Test
    @DisplayName("Should create ASK result")
    void shouldCreateAskResult() {
        GuardrailResult result = GuardrailResult.ask("R01", "User confirmation required");

        assertEquals(GuardrailResult.Decision.ASK, result.decision());
        assertEquals("R01", result.ruleId());
        assertEquals("User confirmation required", result.reason());
        assertFalse(result.block());
        assertFalse(result.isAllowed());
        assertFalse(result.isBlocked());
    }

    @Test
    @DisplayName("Should create WARN result")
    void shouldCreateWarnResult() {
        GuardrailResult result = GuardrailResult.warn("R01", "Warning: operation may be risky");

        assertEquals(GuardrailResult.Decision.WARN, result.decision());
        assertEquals("R01", result.ruleId());
        assertEquals("Warning: operation may be risky", result.reason());
        assertFalse(result.block());
        assertTrue(result.isAllowed());
        assertFalse(result.isBlocked());
    }

    @Test
    @DisplayName("Should test all decision types")
    void shouldTestAllDecisionTypes() {
        assertEquals(4, GuardrailResult.Decision.values().length);
        assertEquals(GuardrailResult.Decision.ALLOW, GuardrailResult.Decision.valueOf("ALLOW"));
        assertEquals(GuardrailResult.Decision.DENY, GuardrailResult.Decision.valueOf("DENY"));
        assertEquals(GuardrailResult.Decision.ASK, GuardrailResult.Decision.valueOf("ASK"));
        assertEquals(GuardrailResult.Decision.WARN, GuardrailResult.Decision.valueOf("WARN"));
    }

    @Test
    @DisplayName("Should create result with custom details")
    void shouldCreateResultWithCustomDetails() {
        Map<String, Object> details = Map.of(
            "confidence", 0.95,
            "matched_pattern", "sudo.*rm"
        );
        GuardrailResult result = GuardrailResult.deny("R01", "Dangerous pattern")
            .withDetails(details);

        assertEquals(details, result.details());
        assertTrue(result.isBlocked());
    }

    @Test
    @DisplayName("Should handle empty details")
    void shouldHandleEmptyDetails() {
        GuardrailResult result = GuardrailResult.allow("R01");
        assertTrue(result.details().isEmpty());
    }

    @Test
    @DisplayName("Should verify blocking behavior")
    void shouldVerifyBlockingBehavior() {
        GuardrailResult allowResult = GuardrailResult.allow("R01");
        GuardrailResult warnResult = GuardrailResult.warn("R01", "Warning");
        GuardrailResult denyResult = GuardrailResult.deny("R01", "Denied");

        assertFalse(allowResult.isBlocked(), "ALLOW should not block");
        assertFalse(warnResult.isBlocked(), "WARN should not block");
        assertTrue(denyResult.isBlocked(), "DENY should block");
    }

    @Test
    @DisplayName("Should create result with null optional fields")
    void shouldCreateResultWithNullOptionalFields() {
        GuardrailResult result = new GuardrailResult(
            GuardrailResult.Decision.ALLOW,
            "R01",
            null,
            false,
            null
        );

        assertEquals("R01", result.ruleId());
        assertNull(result.reason());
        assertNull(result.details());
    }
}
