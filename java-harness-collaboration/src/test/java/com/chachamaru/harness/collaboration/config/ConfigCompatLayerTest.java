package com.chachamaru.harness.collaboration.config;

import com.chachamaru.harness.collaboration.platform.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConfigCompatLayer.
 *
 * <p>Tests configuration loading from multiple sources:
 * <ul>
 *   <li>Platform-specific configuration files</li>
 *   <li>Standard harness.toml</li>
 *   <li>Platform-specific defaults</li>
 *   <li>Universal fallback values</li>
 * </ul>
 *
 * @spec_reference Phase 7: Dual Platform Support - Task 7.5
 */
class ConfigCompatLayerTest {

    @TempDir
    Path tempDir;

    private ConfigCompatLayer configLayer;

    @BeforeEach
    void setUp() {
        // Reset to temp directory for each test
        System.setProperty("user.dir", tempDir.toString());
    }

    @Test
    void testLoadStandardHarnessToml() throws IOException {
        // Create a standard harness.toml
        String tomlContent = """
            [harness]
            version = "5.0.0-java"
            backend = "claude"

            [plan]
            enable = true
            auto_save = true

            [work]
            default_effort = "high"
            """;

        Path tomlPath = tempDir.resolve("harness.toml");
        Files.writeString(tomlPath, tomlContent);

        // Load configuration with base path
        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        // Verify values
        assertEquals("5.0.0-java", configLayer.getString("harness.version").orElse(null));
        assertEquals("claude", configLayer.getString("harness.backend").orElse(null));
        assertTrue(configLayer.getBoolean("plan.enable"));
        assertTrue(configLayer.getBoolean("plan.auto_save"));
        assertEquals("high", configLayer.getString("work.default_effort").orElse(null));
    }

    @Test
    void testLoadCodexPlatformConfig() throws IOException {
        // Create .codex directory and config.toml
        Path codexDir = tempDir.resolve(".codex");
        Files.createDirectories(codexDir);

        String codexConfig = """
            [harness]
            version = "5.0.0-java"
            backend = "codex"

            [work]
            default_effort = "max"
            """;

        Path configPath = codexDir.resolve("config.toml");
        Files.writeString(configPath, codexConfig);

        // Load configuration as Codex platform with base path
        configLayer = new ConfigCompatLayer(Platform.CODEX, tempDir);

        // Verify Codex-specific values
        assertEquals("codex", configLayer.getString("harness.backend").orElse(null));
        assertEquals("max", configLayer.getString("work.default_effort").orElse(null));
        assertTrue(configLayer.getBoolean("work.enable")); // From defaults
    }

    @Test
    void testLoadClaudePlatformConfig() throws IOException {
        // Create .claude directory and config.toml
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        String claudeConfig = """
            [harness]
            version = "5.0.0-java"
            backend = "claude"

            [review]
            strict_mode = true
            """;

        Path configPath = claudeDir.resolve("config.toml");
        Files.writeString(configPath, claudeConfig);

        // Load configuration as Claude platform with base path
        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        // Verify Claude-specific values
        assertEquals("claude", configLayer.getString("harness.backend").orElse(null));
        assertTrue(configLayer.getBoolean("review.strict_mode"));
        assertTrue(configLayer.getBoolean("work.enable")); // From defaults
    }

    @Test
    void testPlatformSpecificDefaults() {
        // No config files, just test defaults
        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        // Verify Claude defaults
        assertEquals("claude", configLayer.getString("harness.backend").orElse(null));
        assertTrue(configLayer.getBoolean("plan.enable"));
        assertTrue(configLayer.getBoolean("work.enable"));
        assertTrue(configLayer.getBoolean("review.enable"));

        // Test Codex defaults
        ConfigCompatLayer codexConfig = new ConfigCompatLayer(Platform.CODEX, tempDir);
        assertEquals("codex", codexConfig.getString("harness.backend").orElse(null));
        assertTrue(codexConfig.getBoolean("plan.enable"));
        assertTrue(codexConfig.getBoolean("work.enable"));
    }

    @Test
    void testPriorityPlatformConfigOverStandard() throws IOException {
        // Create both platform-specific and standard config
        Path codexDir = tempDir.resolve(".codex");
        Files.createDirectories(codexDir);

        String codexConfig = """
            [harness]
            backend = "codex"
            version = "5.0.0-codex"
            """;
        Files.writeString(codexDir.resolve("config.toml"), codexConfig);

        String standardConfig = """
            [harness]
            backend = "claude"
            version = "5.0.0-java"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), standardConfig);

        // Platform config should take priority
        configLayer = new ConfigCompatLayer(Platform.CODEX, tempDir);

        assertEquals("codex", configLayer.getString("harness.backend").orElse(null));
        assertEquals("5.0.0-codex", configLayer.getString("harness.version").orElse(null));
    }

    @Test
    void testMissingKeyReturnsOptionalEmpty() {
        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        Optional<String> missing = configLayer.getString("nonexistent.key");
        assertTrue(missing.isEmpty());
    }

    @Test
    void testBooleanConfiguration() throws IOException {
        String tomlContent = """
            [features]
            feature_a = true
            feature_b = false
            """;

        Files.writeString(tempDir.resolve("harness.toml"), tomlContent);
        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        assertTrue(configLayer.getBoolean("features.feature_a"));
        assertFalse(configLayer.getBoolean("features.feature_b"));
        assertFalse(configLayer.getBoolean("features.nonexistent")); // Default to false
    }

    @Test
    void testIntegerConfiguration() throws IOException {
        String tomlContent = """
            [limits]
            max_retries = 3
            timeout = 30
            """;

        Files.writeString(tempDir.resolve("harness.toml"), tomlContent);
        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);

        assertEquals(3, configLayer.getInt("limits.max_retries"));
        assertEquals(30, configLayer.getInt("limits.timeout"));
        assertEquals(0, configLayer.getInt("limits.nonexistent")); // Default to 0
    }

    @Test
    void testGetCurrentPlatform() {
        ConfigCompatLayer claudeLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);
        assertEquals(Platform.CLAUDE_CODE, claudeLayer.getCurrentPlatform());

        ConfigCompatLayer codexLayer = new ConfigCompatLayer(Platform.CODEX, tempDir);
        assertEquals(Platform.CODEX, codexLayer.getCurrentPlatform());
    }

    @Test
    void testGetCompleteConfig() {
        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);
        Map<String, Object> fullConfig = configLayer.getConfig();

        assertNotNull(fullConfig);
        assertFalse(fullConfig.isEmpty());

        // Verify some expected keys exist
        assertTrue(fullConfig.containsKey("harness"));
    }

    @Test
    void testReloadConfiguration() throws IOException {
        // Create initial config
        String initialConfig = """
            [harness]
            version = "1.0.0"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), initialConfig);

        configLayer = new ConfigCompatLayer(Platform.CLAUDE_CODE, tempDir);
        assertEquals("1.0.0", configLayer.getString("harness.version").orElse(null));

        // Modify config file
        String updatedConfig = """
            [harness]
            version = "2.0.0"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), updatedConfig);

        // Reload and verify
        configLayer.reload();
        assertEquals("2.0.0", configLayer.getString("harness.version").orElse(null));
    }
}
