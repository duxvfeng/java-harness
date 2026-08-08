package com.chachamaru.harness.collaboration.integration;

import com.chachamaru.harness.collaboration.config.ConfigCompatLayer;
import com.chachamaru.harness.collaboration.platform.Platform;
import com.chachamaru.harness.collaboration.platform.PlatformDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for dual platform support.
 *
 * <p>Tests the complete integration of:
 * <ul>
 *   <li>Platform detection mechanism</li>
 *   <li>Configuration parsing and compatibility layer</li>
 *   <li>Backend selection strategy</li>
 *   <li>Functional equivalence across platforms</li>
 * </ul>
 *
 * @spec_reference Phase 7: Dual Platform Support - Task 7.7
 */
class DualPlatformIntegrationTest {

    @TempDir
    Path tempDir;

    /**
     * Test end-to-end platform detection and configuration loading.
     */
    @Test
    void testPlatformDetectionAndConfigLoading() throws IOException {
        // Create platform-specific config for Claude Code
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        String claudeConfig = """
            [harness]
            version = "5.0.0-java"
            backend = "claude"

            [work]
            default_effort = "high"

            [plan]
            enable = true
            """;
        Files.writeString(claudeDir.resolve("config.toml"), claudeConfig);

        // Load configuration for Claude Code
        ConfigCompatLayer claudeConfigLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        // Verify platform detection
        PlatformDetector detector = new PlatformDetector();
        assertEquals(Platform.CLAUDE_CODE, detector.detectCurrentPlatform());

        // Verify configuration loading
        assertEquals("claude", claudeConfigLayer.getString("harness.backend").orElse(null));
        assertEquals("high", claudeConfigLayer.getString("work.default_effort").orElse(null));
        assertTrue(claudeConfigLayer.getBoolean("plan.enable"));

        // Verify platform-specific config path
        assertEquals(Platform.CLAUDE_CODE, claudeConfigLayer.getCurrentPlatform());
    }

    /**
     * Test end-to-end configuration loading for Codex platform.
     */
    @Test
    void testCodexPlatformConfigurationLoading() throws IOException {
        // Create platform-specific config for Codex
        Path codexDir = tempDir.resolve(".codex");
        Files.createDirectories(codexDir);

        String codexConfig = """
            [harness]
            version = "5.0.0-java"
            backend = "codex"

            [work]
            default_effort = "max"

            [plan]
            enable = true
            """;
        Files.writeString(codexDir.resolve("config.toml"), codexConfig);

        // Load configuration for Codex
        ConfigCompatLayer codexConfigLayer = new ConfigCompatLayer(Platform.CODEX, tempDir);

        // Verify configuration loading
        assertEquals("codex", codexConfigLayer.getString("harness.backend").orElse(null));
        assertEquals("max", codexConfigLayer.getString("work.default_effort").orElse(null));
        assertTrue(codexConfigLayer.getBoolean("plan.enable"));

        // Verify platform detection
        assertEquals(Platform.CODEX, codexConfigLayer.getCurrentPlatform());
    }

    /**
     * Test fallback from platform-specific config to standard harness.toml.
     */
    @Test
    void testConfigFallbackChain() throws IOException {
        // Create standard harness.toml
        String standardConfig = """
            [harness]
            version = "5.0.0-java"
            backend = "auto"

            [work]
            default_effort = "medium"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), standardConfig);

        // Load configuration for Claude Code (no platform-specific config)
        ConfigCompatLayer configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        // Verify fallback to standard config
        assertEquals("auto", configLayer.getString("harness.backend").orElse(null));
        assertEquals("medium", configLayer.getString("work.default_effort").orElse(null));

        // Verify platform defaults are applied
        assertTrue(configLayer.getBoolean("plan.enable")); // From platform defaults
        assertTrue(configLayer.getBoolean("review.enable")); // From platform defaults
    }

    /**
     * Test functional equivalence between platforms.
     */
    @Test
    void testFunctionalEquivalence() throws IOException {
        // Create identical configurations for both platforms
        String harnessConfig = """
            [harness]
            version = "5.0.0-java"

            [plan]
            enable = true
            auto_save = true

            [work]
            enable = true
            default_effort = "medium"

            [review]
            enable = true
            strict_mode = false
            """;

        // Create platform-specific configs
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.writeString(claudeDir.resolve("config.toml"), harnessConfig);

        Path codexDir = tempDir.resolve(".codex");
        Files.createDirectories(codexDir);
        Files.writeString(codexDir.resolve("config.toml"), harnessConfig);

        // Load configurations for both platforms
        ConfigCompatLayer claudeConfig = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);
        ConfigCompatLayer codexConfig = new ConfigCompatLayer(Platform.CODEX, tempDir);

        // Verify functional equivalence - same configuration values
        assertEquals(
            claudeConfig.getString("harness.version"),
            codexConfig.getString("harness.version")
        );
        assertEquals(
            claudeConfig.getBoolean("plan.enable"),
            codexConfig.getBoolean("plan.enable")
        );
        assertEquals(
            claudeConfig.getBoolean("work.enable"),
            codexConfig.getBoolean("work.enable")
        );
        assertEquals(
            claudeConfig.getBoolean("review.enable"),
            codexConfig.getBoolean("review.enable")
        );
    }

    /**
     * Test platform-specific backend defaults.
     */
    @Test
    void testPlatformSpecificDefaults() {
        // No config files, just platform defaults
        ConfigCompatLayer claudeConfig = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);
        ConfigCompatLayer codexConfig = new ConfigCompatLayer(Platform.CODEX, tempDir);

        // Verify platform-specific backend defaults
        assertEquals("claude", claudeConfig.getString("harness.backend").orElse(null));
        assertEquals("codex", codexConfig.getString("harness.backend").orElse(null));

        // Verify common defaults are applied to both
        assertTrue(claudeConfig.getBoolean("plan.enable"));
        assertTrue(codexConfig.getBoolean("plan.enable"));
        assertTrue(claudeConfig.getBoolean("work.enable"));
        assertTrue(codexConfig.getBoolean("work.enable"));
    }

    /**
     * Test configuration reload functionality.
     */
    @Test
    void testConfigurationReload() throws IOException {
        // Create initial config
        String initialConfig = """
            [harness]
            version = "1.0.0"
            backend = "claude"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), initialConfig);

        ConfigCompatLayer configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);
        assertEquals("1.0.0", configLayer.getString("harness.version").orElse(null));

        // Update config file
        String updatedConfig = """
            [harness]
            version = "2.0.0"
            backend = "codex"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), updatedConfig);

        // Reload and verify changes
        configLayer.reload();
        assertEquals("2.0.0", configLayer.getString("harness.version").orElse(null));
        assertEquals("codex", configLayer.getString("harness.backend").orElse(null));
    }

    /**
     * Test configuration priority: platform-specific > standard > defaults.
     */
    @Test
    void testConfigurationPriority() throws IOException {
        // Create both platform-specific and standard configs
        Path codexDir = tempDir.resolve(".codex");
        Files.createDirectories(codexDir);

        String platformConfig = """
            [harness]
            backend = "codex"
            priority = "platform"
            """;
        Files.writeString(codexDir.resolve("config.toml"), platformConfig);

        String standardConfig = """
            [harness]
            backend = "claude"
            priority = "standard"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), standardConfig);

        // Load configuration - platform config should take priority
        ConfigCompatLayer configLayer = new ConfigCompatLayer(Platform.CODEX, tempDir);

        assertEquals("codex", configLayer.getString("harness.backend").orElse(null));
        assertEquals("platform", configLayer.getString("harness.priority").orElse(null));

        // Verify standard config value is NOT used
        assertNotEquals("standard", configLayer.getString("harness.priority").orElse(null));
    }

    /**
     * Test complete configuration map access.
     */
    @Test
    void testCompleteConfigAccess() {
        ConfigCompatLayer configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        var completeConfig = configLayer.getConfig();

        assertNotNull(completeConfig);
        assertFalse(completeConfig.isEmpty());

        // Verify top-level sections exist
        assertTrue(completeConfig.containsKey("harness"));
        assertTrue(completeConfig.containsKey("plan"));
        assertTrue(completeConfig.containsKey("work"));
        assertTrue(completeConfig.containsKey("review"));
    }

    /**
     * Test configuration value types and parsing.
     */
    @Test
    void testConfigurationValueTypes() throws IOException {
        String configContent = """
            [types]
            string_val = "hello"
            bool_val = true
            int_val = 42
            float_val = 3.14
            """;
        Files.writeString(tempDir.resolve("harness.toml"), configContent);

        ConfigCompatLayer configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        // Verify different value types are parsed correctly
        assertEquals("hello", configLayer.getString("types.string_val").orElse(null));
        assertTrue(configLayer.getBoolean("types.bool_val"));
        assertEquals(42, configLayer.getInt("types.int_val"));
    }

    /**
     * Test missing configuration keys return appropriate defaults.
     */
    @Test
    void testMissingConfigurationKeys() {
        ConfigCompatLayer configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        // Test missing key returns Optional.empty for getString
        assertTrue(configLayer.getString("nonexistent.key").isEmpty());

        // Test missing key returns false for getBoolean
        assertFalse(configLayer.getBoolean("nonexistent.bool.key"));

        // Test missing key returns 0 for getInt
        assertEquals(0, configLayer.getInt("nonexistent.int.key"));
    }

    /**
     * Test platform detection with different environment scenarios.
     */
    @Test
    void testPlatformDetectionScenarios() {
        PlatformDetector detector = new PlatformDetector();

        // Test default platform (Claude Code) when no env vars set
        Platform detectedPlatform = detector.detectCurrentPlatform();
        // In test environment, should default to CLAUDE_CODE
        assertNotNull(detectedPlatform);
        assertTrue(detectedPlatform == Platform.CLAUDE_CODE || detectedPlatform == Platform.CODEX);
    }
}
