package com.chachamaru.harness.collaboration.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlatformDetector class.
 * Verifies platform detection capabilities for Claude Code and Codex environments.
 */
@DisplayName("Platform Detector Tests")
class PlatformDetectorTest {

    @Test
    @DisplayName("应该检测到当前平台")
    void shouldDetectCurrentPlatform() {
        PlatformDetector detector = new PlatformDetector();
        Platform platform = detector.detectCurrentPlatform();

        assertNotNull(platform, "Platform should not be null");
        assertTrue(platform == Platform.CLAUDE_CODE || platform == Platform.CODEX,
                   "Platform should be either CLAUDE_CODE or CODEX");
    }

    @Test
    @DisplayName("应该正确识别 Claude Code 环境")
    void shouldRecognizeClaudeCodeEnvironment() {
        // When running in Claude Code environment
        PlatformDetector detector = new PlatformDetector();

        // Should detect CLAUDE_CODE when CLAUDE_CODE_HARNESS env is set
        if (System.getenv("CLAUDE_CODE_HARNESS") != null) {
            assertEquals(Platform.CLAUDE_CODE, detector.detectCurrentPlatform(),
                        "Should detect Claude Code when CLAUDE_CODE_HARNESS is set");
        }
    }

    @Test
    @DisplayName("应该正确识别 Codex 环境")
    void shouldRecognizeCodexEnvironment() {
        // When running in Codex environment
        PlatformDetector detector = new PlatformDetector();

        // Should detect CODEX when CODEX_CLI env is set
        if (System.getenv("CODEX_CLI") != null) {
            assertEquals(Platform.CODEX, detector.detectCurrentPlatform(),
                        "Should detect Codex when CODEX_CLI is set");
        }
    }

    @Test
    @DisplayName("当无法检测环境时应该返回默认平台")
    void shouldReturnDefaultPlatformWhenUnknownEnvironment() {
        PlatformDetector detector = new PlatformDetector();

        // Remove environment variables temporarily
        String originalClaude = System.getenv("CLAUDE_CODE_HARNESS");
        String originalCodex = System.getenv("CODEX_CLI");

        try {
            // Simulate unknown environment by removing env vars
            if (originalClaude != null) {
                System.clearProperty("CLAUDE_CODE_HARNESS");
            }
            if (originalCodex != null) {
                System.clearProperty("CODEX_CLI");
            }

            Platform platform = detector.detectCurrentPlatform();

            assertNotNull(platform, "Should return a platform even in unknown environment");
            // Default should be CLAUDE_CODE as it's the primary platform
            assertEquals(Platform.CLAUDE_CODE, platform,
                        "Should default to CLAUDE_CODE in unknown environment");

        } finally {
            // Restore environment variables
            if (originalClaude != null) {
                System.setProperty("CLAUDE_CODE_HARNESS", originalClaude);
            }
            if (originalCodex != null) {
                System.setProperty("CODEX_CLI", originalCodex);
            }
        }
    }

    @Test
    @DisplayName("应该支持平台类型枚举")
    void shouldSupportPlatformEnum() {
        // Verify platform enum has expected values
        assertEquals(2, Platform.values().length, "Platform enum should have 2 values");

        assertTrue(java.util.Arrays.asList(Platform.values()).contains(Platform.CLAUDE_CODE),
                  "Platform enum should contain CLAUDE_CODE");
        assertTrue(java.util.Arrays.asList(Platform.values()).contains(Platform.CODEX),
                  "Platform enum should contain CODEX");
    }
}
