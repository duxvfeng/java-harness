package com.chachamaru.harness.collaboration.skill;

import com.chachamaru.harness.collaboration.platform.Platform;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import com.chachamaru.harness.collaboration.skill.impl.WorkSkill;
import java.util.Map;

/**
 * Manual test runner for UniversalSkill implementation.
 */
public class UniversalSkillManualTest {
    public static void main(String[] args) {
        System.out.println("=== Universal Skill Manual Test ===\n");

        // Test 1: UniversalSkill interface exists
        System.out.println("Test 1: UniversalSkill interface exists");
        try {
            assertTrue(UniversalSkill.class.isInterface(),
                      "UniversalSkill should be an interface");
            assertTrue(com.chachamaru.harness.collaboration.skill.Skill.class.isAssignableFrom(UniversalSkill.class),
                      "UniversalSkill should extend Skill interface");
            System.out.println("✓ UniversalSkill interface exists and extends Skill\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 2: Platform adaptation methods exist
        System.out.println("Test 2: Platform adaptation methods");
        try {
            UniversalSkill.class.getMethod("adaptForPlatform", Platform.class);
            UniversalSkill.class.getMethod("getSupportedPlatforms");
            UniversalSkill.class.getMethod("isPlatformSupported", Platform.class);
            System.out.println("✓ All platform adaptation methods exist\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 3: Existing skills remain compatible
        System.out.println("Test 3: Existing skills compatibility");
        try {
            WorkSkill workSkill = new WorkSkill();
            assertTrue(workSkill instanceof com.chachamaru.harness.collaboration.skill.Skill,
                      "WorkSkill should implement Skill interface");
            assertEquals("work", workSkill.getId(), "WorkSkill should have correct ID");
            System.out.println("✓ Existing skills remain compatible\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 4: Concrete UniversalSkill implementation
        System.out.println("Test 4: UniversalSkill implementation");
        try {
            TestUniversalSkill skill = new TestUniversalSkill();

            // Test platform adaptation
            UniversalSkill adapted = skill.adaptForPlatform(Platform.CLAUDE_CODE);
            assertNotNull(adapted, "Adapted skill should not be null");

            // Test supported platforms
            assertFalse(skill.getSupportedPlatforms().isEmpty(),
                       "Should support at least one platform");
            assertTrue(skill.isPlatformSupported(Platform.CLAUDE_CODE),
                      "Should support Claude Code");
            assertFalse(skill.isPlatformSupported(Platform.CODEX),
                      "Should not support Codex by default");

            // Test execution
            SkillContext context = SkillContext.createForTest("test", "Test Skill");
            SkillResult result = adapted.execute(context);

            assertTrue(result.isSuccess(), "Execution should succeed");
            assertNotNull(result.output(), "Output should not be null");

            System.out.println("✓ UniversalSkill implementation works correctly\n");
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Test 5: Unsupported platform exception
        System.out.println("Test 5: Unsupported platform handling");
        try {
            TestUniversalSkill skill = new TestUniversalSkill();

            try {
                skill.adaptForPlatform(Platform.CODEX);
                System.out.println("✗ Should have thrown exception for unsupported platform");
            } catch (IllegalArgumentException e) {
                System.out.println("✓ Correctly throws exception for unsupported platform\n");
            }
        } catch (Exception e) {
            System.out.println("✗ Failed: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("=== All Universal Skill Tests Passed ===");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " - expected: " + expected + ", actual: " + actual);
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError(message);
        }
    }

    /**
     * Test implementation of UniversalSkill.
     */
    static class TestUniversalSkill implements UniversalSkill {
        private final java.util.Set<Platform> supportedPlatforms = java.util.Set.of(Platform.CLAUDE_CODE);

        @Override
        public UniversalSkill adaptForPlatform(Platform platform) {
            if (!isPlatformSupported(platform)) {
                throw new IllegalArgumentException("Platform not supported: " + platform);
            }
            return this;
        }

        @Override
        public java.util.Set<Platform> getSupportedPlatforms() {
            return supportedPlatforms;
        }

        @Override
        public boolean isPlatformSupported(Platform platform) {
            return supportedPlatforms.contains(platform);
        }

        @Override
        public SkillResult execute(SkillContext context) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            return SkillResult.success(
                getId(),
                Map.of("platform", "adapted", "timestamp", now),
                "Test execution successful",
                now
            );
        }

        @Override
        public String getId() {
            return "test-universal";
        }

        @Override
        public String getName() {
            return "Test Universal Skill";
        }

        @Override
        public String getDescription() {
            return "Test universal skill for platform adaptation";
        }
    }
}
