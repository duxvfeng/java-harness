package com.chachamaru.harness.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HookEventType enum.
 */
class HookEventTypeTest {

    @Test
    void testEnumValues() {
        // Test that all expected enum values exist
        HookEventType[] types = HookEventType.values();
        assertEquals(8, types.length);
    }

    @Test
    void testEnumValueOf() {
        assertEquals(HookEventType.PRE_HOOK, HookEventType.valueOf("PRE_HOOK"));
        assertEquals(HookEventType.POST_HOOK, HookEventType.valueOf("POST_HOOK"));
        assertEquals(HookEventType.GUARDRAIL_HOOK, HookEventType.valueOf("GUARDRAIL_HOOK"));
        assertEquals(HookEventType.PRE_COMMIT_HOOK, HookEventType.valueOf("PRE_COMMIT_HOOK"));
        assertEquals(HookEventType.POST_COMMIT_HOOK, HookEventType.valueOf("POST_COMMIT_HOOK"));
        assertEquals(HookEventType.PRE_REVIEW_HOOK, HookEventType.valueOf("PRE_REVIEW_HOOK"));
        assertEquals(HookEventType.POST_REVIEW_HOOK, HookEventType.valueOf("POST_REVIEW_HOOK"));
        assertEquals(HookEventType.ERROR_HOOK, HookEventType.valueOf("ERROR_HOOK"));
    }

    @Test
    void testEnumConstants() {
        // Test enum constants for presence
        assertNotNull(HookEventType.PRE_HOOK);
        assertNotNull(HookEventType.POST_HOOK);
        assertNotNull(HookEventType.GUARDRAIL_HOOK);
        assertNotNull(HookEventType.PRE_COMMIT_HOOK);
        assertNotNull(HookEventType.POST_COMMIT_HOOK);
        assertNotNull(HookEventType.PRE_REVIEW_HOOK);
        assertNotNull(HookEventType.POST_REVIEW_HOOK);
        assertNotNull(HookEventType.ERROR_HOOK);
    }
}
