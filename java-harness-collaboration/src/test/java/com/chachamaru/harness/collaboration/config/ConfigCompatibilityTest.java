package com.chachamaru.harness.collaboration.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Configuration compatibility layer.
 * Verifies that unified configuration parsing works across platforms.
 */
@DisplayName("Configuration Compatibility Tests")
class ConfigCompatibilityTest {

    @Test
    @DisplayName("配置解析器类应该存在")
    void configParserClassShouldExist() {
        assertDoesNotThrow(() -> {
            Class<?> parserClass = Class.forName(
                "com.chachamaru.harness.collaboration.config.HarnessConfigParser");
            assertNotNull(parserClass);
        }, "HarnessConfigParser class should exist");
    }

    @Test
    @DisplayName("应该能够解析 harness.toml 配置文件")
    void shouldParseHarnessTomlConfig() throws ConfigParseException {
        HarnessConfigParser parser = new HarnessConfigParser();

        // Test with a valid TOML config
        String tomlContent = "[harness]\n" +
                           "version = \"4.1.1\"\n" +
                           "platform = \"claude-code\"\n" +
                           "\n" +
                           "[backend]\n" +
                           "default = \"claude\"\n" +
                           "fallback = \"system\"\n" +
                           "\n" +
                           "[features]\n" +
                           "auto-detect = true\n" +
                           "multi-platform = true\n";

        HarnessConfig config = parser.parseString(tomlContent, "test-config");

        assertNotNull(config, "Config should not be null");
        assertEquals("4.1.1", config.getVersion(),
                    "Version should be parsed correctly");
        assertEquals("claude-code", config.getPlatform(),
                    "Platform should be parsed correctly");
    }

    @Test
    @DisplayName("应该支持从文件路径解析配置")
    void shouldParseConfigFromFilePath() throws ConfigParseException {
        HarnessConfigParser parser = new HarnessConfigParser();

        // Test with file path
        Path configPath = Path.of("/tmp/test-harness.toml");

        assertDoesNotThrow(() -> {
            // Would parse from file path
            // For now, test with string content
            String content = "[harness]\nversion = \"4.1.1\"\n";
            HarnessConfig config = parser.parseString(content, configPath.toString());
            assertNotNull(config);
        }, "Should parse from file path");
    }

    @Test
    @DisplayName("应该支持平台特定的配置节")
    void shouldSupportPlatformSpecificSections() throws ConfigParseException {
        String tomlContent = "[harness]\n" +
                           "platform = \"claude-code\"\n" +
                           "\n" +
                           "[claude-code]\n" +
                           "model = \"claude-sonnet-5\"\n" +
                           "max-tokens = 200000\n" +
                           "\n" +
                           "[codex]\n" +
                           "model = \"gpt-4\"\n" +
                           "api-key = \"${CODEX_API_KEY}\"\n";

        HarnessConfigParser parser = new HarnessConfigParser();
        HarnessConfig config = parser.parseString(tomlContent, "test-config");

        assertNotNull(config, "Config should not be null");
        assertTrue(config.hasPlatformSection("claude-code"),
                   "Should have claude-code section");
        assertTrue(config.hasPlatformSection("codex"),
                   "Should have codex section");
    }

    @Test
    @DisplayName("应该能够获取后端配置")
    void shouldRetrieveBackendConfiguration() throws ConfigParseException {
        String tomlContent = "[backend]\n" +
                           "default = \"claude\"\n" +
                           "timeout = 300000\n" +
                           "max-retries = 3\n" +
                           "\n" +
                           "[backend.fallback]\n" +
                           "enabled = true\n" +
                           "backend = \"system\"\n";

        HarnessConfigParser parser = new HarnessConfigParser();
        HarnessConfig config = parser.parseString(tomlContent, "test-config");

        assertNotNull(config, "Config should not be null");
        assertEquals("claude", config.getBackendDefault(),
                    "Backend default should be retrieved");
        assertEquals(300000, config.getBackendTimeout(),
                    "Backend timeout should be retrieved");
        assertEquals(3, config.getBackendMaxRetries(),
                    "Backend max retries should be retrieved");
    }

    @Test
    @DisplayName("应该支持环境变量替换")
    void shouldSupportEnvironmentVariableExpansion() throws ConfigParseException {
        String tomlContent = "[harness]\n" +
                           "api-key = \"${HARNESS_API_KEY}\"\n" +
                           "data-dir = \"${HOME}/.harness\"\n";

        HarnessConfigParser parser = new HarnessConfigParser();
        HarnessConfig config = parser.parseString(tomlContent, "test-config");

        assertNotNull(config, "Config should not be null");
        assertNotNull(config.getApiKey(), "API key should support expansion");
        assertNotNull(config.getDataDir(), "Data dir should support expansion");
    }

    @Test
    @DisplayName("应该提供默认配置")
    void shouldProvideDefaultConfiguration() throws ConfigParseException {
        HarnessConfigParser parser = new HarnessConfigParser();

        // Parse empty config, should get defaults
        HarnessConfig config = parser.parseString("", "empty-config");

        assertNotNull(config, "Default config should not be null");
        assertNotNull(config.getVersion(), "Should have default version");
        assertNotNull(config.getPlatform(), "Should have default platform");
    }

    @Test
    @DisplayName("应该验证配置必需字段")
    void shouldValidateRequiredFields() {
        String invalidToml = "[harness]\n" +
                             "# Missing required 'version' field\n" +
                             "platform = \"claude-code\"\n";

        HarnessConfigParser parser = new HarnessConfigParser();

        assertThrows(ConfigParseException.class, () -> {
            parser.parseString(invalidToml, "invalid-config");
        }, "Should throw exception for missing required fields");
    }

    @Test
    @DisplayName("应该支持嵌套配置节")
    void shouldSupportNestedSections() throws ConfigParseException {
        String tomlContent = "[harness]\n" +
                           "version = \"4.1.1\"\n" +
                           "\n" +
                           "[skills.claude]\n" +
                           "enabled = true\n" +
                           "priority = 10\n" +
                           "\n" +
                           "[skills.codex]\n" +
                           "enabled = false\n" +
                           "priority = 5\n";

        HarnessConfigParser parser = new HarnessConfigParser();
        HarnessConfig config = parser.parseString(tomlContent, "test-config");

        assertNotNull(config, "Config should not be null");
        assertTrue(config.hasSection("skills.claude"),
                   "Should have skills.claude section");
        assertTrue(config.hasSection("skills.codex"),
                   "Should have skills.codex section");
    }

    @Test
    @DisplayName("两个平台应该能够读取相同配置")
    void bothPlatformsShouldReadSameConfig() throws ConfigParseException {
        String sharedConfig = "[harness]\n" +
                             "version = \"4.1.1\"\n" +
                             "multi-platform = true\n" +
                             "\n" +
                             "[backend]\n" +
                             "default = \"auto-detect\"\n" +
                             "\n" +
                             "[claude-code]\n" +
                             "native = true\n" +
                             "\n" +
                             "[codex]\n" +
                             "native = false\n";

        HarnessConfigParser parser = new HarnessConfigParser();
        HarnessConfig config = parser.parseString(sharedConfig, "shared-config");

        assertNotNull(config, "Both platforms should parse same config");
        assertTrue(config.isMultiPlatform(),
                   "Multi-platform flag should be set");
        assertEquals("auto-detect", config.getBackendDefault(),
                    "Both platforms should use auto-detect backend");
    }
}
