package com.chachamaru.harness.collaboration.skill;

import com.chachamaru.harness.collaboration.platform.Platform;
import com.chachamaru.harness.collaboration.platform.PlatformDetector;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import com.chachamaru.harness.collaboration.skill.impl.WorkSkill;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UniversalSkill interface and platform adaptation.
 * Verifies that the unified skill interface supports platform-specific adaptations
 * while maintaining backward compatibility with existing skills.
 */
@DisplayName("Universal Skill Interface Tests")
class UniversalSkillTest {

    @Test
    @DisplayName("UniversalSkill 接口应该存在")
    void universalSkillInterfaceShouldExist() {
        // UniversalSkill should be an interface that extends Skill
        assertTrue(UniversalSkill.class.isInterface(),
                   "UniversalSkill should be an interface");
        assertTrue(Skill.class.isAssignableFrom(UniversalSkill.class),
                   "UniversalSkill should extend Skill interface");
    }

    @Test
    @DisplayName("UniversalSkill 应该支持平台适配方法")
    void universalSkillShouldSupportPlatformAdaptation() {
        // UniversalSkill should have platform adaptation methods
        try {
            UniversalSkill.class.getMethod("adaptForPlatform", Platform.class);
            UniversalSkill.class.getMethod("getSupportedPlatforms");
            UniversalSkill.class.getMethod("isPlatformSupported", Platform.class);
        } catch (NoSuchMethodException e) {
            fail("UniversalSkill should have platform adaptation methods: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("现有技能应该保持兼容性")
    void existingSkillsShouldRemainCompatible() {
        // Existing skills should still implement Skill interface
        WorkSkill workSkill = new WorkSkill();

        assertTrue(workSkill instanceof Skill,
                   "WorkSkill should implement Skill interface");
        assertEquals("work", workSkill.getId(),
                    "WorkSkill should have correct ID");
        assertEquals("Work Skill", workSkill.getName(),
                    "WorkSkill should have correct name");
    }

    @Test
    @DisplayName("UniversalSkill 实现应该能够适配不同平台")
    void universalSkillImplementationShouldAdaptToPlatforms() {
        // A concrete UniversalSkill should be able to adapt to different platforms
        TestUniversalSkill skill = new TestUniversalSkill();

        // Should adapt to Claude Code (supported platform)
        SkillContext claudeContext = createTestContext();

        assertDoesNotThrow(() -> {
            SkillResult claudeResult = skill.adaptForPlatform(Platform.CLAUDE_CODE)
                .execute(claudeContext);

            assertNotNull(claudeResult, "Result should not be null for Claude Code");
            assertTrue(claudeResult.isSuccess(),
                      "Claude Code execution should succeed");
        });

        // Should throw exception for unsupported platform
        assertThrows(IllegalArgumentException.class, () -> {
            skill.adaptForPlatform(Platform.CODEX);
        }, "Should throw exception for unsupported platform");
    }

    @Test
    @DisplayName("UniversalSkill 应该报告支持的平台")
    void universalSkillShouldReportSupportedPlatforms() {
        TestUniversalSkill skill = new TestUniversalSkill();

        assertFalse(skill.getSupportedPlatforms().isEmpty(),
                   "Skill should support at least one platform");

        assertTrue(skill.getSupportedPlatforms().contains(Platform.CLAUDE_CODE),
                   "Skill should support Claude Code by default");
    }

    @Test
    @DisplayName("UniversalSkill 应该能够检查平台支持")
    void universalSkillShouldCheckPlatformSupport() {
        TestUniversalSkill skill = new TestUniversalSkill();

        assertTrue(skill.isPlatformSupported(Platform.CLAUDE_CODE),
                  "Should support Claude Code");
        // Test implementation only supports CLAUDE_CODE, not CODEX
        assertFalse(skill.isPlatformSupported(Platform.CODEX),
                   "Test skill should not support Codex by default");
    }

    @Test
    @DisplayName("适配后的技能应该保持原始技能特性")
    void adaptedSkillShouldMaintainOriginalCharacteristics() {
        TestUniversalSkill skill = new TestUniversalSkill();

        UniversalSkill adapted = skill.adaptForPlatform(Platform.CLAUDE_CODE);

        assertEquals("test-universal", adapted.getId(),
                    "Adapted skill should maintain original ID");
        assertEquals("Test Universal Skill", adapted.getName(),
                    "Adapted skill should maintain original name");
    }

    // Helper methods and test implementation

    private SkillContext createTestContext() {
        // Create a minimal test context
        return SkillContext.createForTest("test", "Test Skill");
    }

    /**
     * Test implementation of UniversalSkill for testing purposes.
     */
    private static class TestUniversalSkill implements UniversalSkill {
        private final java.util.Set<Platform> supportedPlatforms = java.util.Set.of(Platform.CLAUDE_CODE);
        private java.time.LocalDateTime executionStartTime;

        @Override
        public UniversalSkill adaptForPlatform(Platform platform) {
            if (!isPlatformSupported(platform)) {
                throw new IllegalArgumentException("Platform not supported: " + platform);
            }
            // Return self for simplicity in test
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
            executionStartTime = java.time.LocalDateTime.now();
            return SkillResult.success(
                getId(),
                Map.of("platform", "adapted", "timestamp", executionStartTime),
                "Test execution successful",
                executionStartTime
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
