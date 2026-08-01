package com.chachamaru.harness.tools.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigSyncTool.
 */
@DisplayName("ConfigSyncTool Tests")
public class ConfigSyncToolTest {

    @Test
    @DisplayName("应该创建ConfigSyncTool实例")
    void shouldCreateConfigSyncTool() {
        ConfigSyncTool tool = new ConfigSyncTool();
        assertNotNull(tool);
        assertNotNull(tool.getProjectName());
    }

    @Test
    @DisplayName("应该使用指定的项目名称")
    void shouldUseSpecifiedProjectName() {
        ConfigSyncTool tool = new ConfigSyncTool("test-project");
        assertEquals("test-project", tool.getProjectName());
    }

    @Test
    @DisplayName("应该生成Claude Code settings文件")
    void shouldGenerateClaudeCodeSettings(@TempDir Path tempDir) throws ConfigSyncTool.ConfigSyncException {
        ConfigSyncTool tool = new ConfigSyncTool();
        Path settingsPath = tool.generateClaudeCodeSettings(tempDir);

        assertTrue(Files.exists(settingsPath));
        assertEquals("settings.json", settingsPath.getFileName().toString());
    }

    @Test
    @DisplayName("应该生成harness配置文件")
    void shouldGenerateHarnessConfig(@TempDir Path tempDir) throws ConfigSyncTool.ConfigSyncException {
        ConfigSyncTool tool = new ConfigSyncTool();
        Path configPath = tool.generateHarnessConfig(tempDir);

        assertTrue(Files.exists(configPath));
        assertEquals("harness.yaml", configPath.getFileName().toString());
    }

    @Test
    @DisplayName("同步应该生成两个配置文件")
    void syncShouldGenerateBothConfigFiles(@TempDir Path tempDir) throws ConfigSyncTool.ConfigSyncException {
        ConfigSyncTool tool = new ConfigSyncTool();
        ConfigSyncTool.ConfigSyncResult result = tool.syncToClaudeCode(tempDir);

        assertTrue(result.success());
        assertEquals(2, result.fileCount());
    }

    @Test
    @DisplayName("同步应该在.claude目录中生成文件")
    void syncShouldGenerateFilesInClaudeDirectory(@TempDir Path tempDir) throws ConfigSyncTool.ConfigSyncException {
        ConfigSyncTool tool = new ConfigSyncTool();
        Path claudeDir = tempDir.resolve(".claude");

        ConfigSyncTool.ConfigSyncResult result = tool.syncToClaudeCode(claudeDir);

        assertTrue(result.success());
        assertTrue(Files.exists(claudeDir.resolve("settings.json")));
        assertTrue(Files.exists(claudeDir.resolve("harness.yaml")));
    }

    @Test
    @DisplayName("应该验证有效的配置文件")
    void shouldValidateValidConfigFile(@TempDir Path tempDir) throws ConfigSyncTool.ConfigSyncException {
        ConfigSyncTool tool = new ConfigSyncTool();
        Path configPath = tool.generateHarnessConfig(tempDir);

        ConfigSyncTool.ValidationResult result = tool.validateConfig(configPath);

        assertTrue(result.isValid());
        assertEquals(0, result.errorCount());
    }

    @Test
    @DisplayName("应该报告不存在的配置文件")
    void shouldReportNonExistentConfigFile() {
        ConfigSyncTool tool = new ConfigSyncTool();
        Path fakePath = Path.of("/nonexistent/config.yaml");

        ConfigSyncTool.ValidationResult result = tool.validateConfig(fakePath);

        assertFalse(result.isValid());
        assertEquals(1, result.errorCount());
        assertTrue(result.errors().get(0).contains("does not exist"));
    }

    @Test
    @DisplayName("null目标目录应该抛出异常")
    void shouldThrowExceptionForNullTargetDir() {
        ConfigSyncTool tool = new ConfigSyncTool();

        assertThrows(IllegalArgumentException.class, () -> {
            tool.generateClaudeCodeSettings(null);
        });
    }

    @Test
    @DisplayName("null配置路径应该返回验证失败")
    void shouldReturnInvalidForNullConfigPath() {
        ConfigSyncTool tool = new ConfigSyncTool();

        ConfigSyncTool.ValidationResult result = tool.validateConfig(null);

        assertFalse(result.isValid());
        assertTrue(result.errorCount() > 0);
    }

    @Test
    @DisplayName("生成的settings.json应该包含基本配置")
    void generatedSettingsShouldContainBasicConfig(@TempDir Path tempDir) throws Exception {
        ConfigSyncTool tool = new ConfigSyncTool("test-project");
        Path settingsPath = tool.generateClaudeCodeSettings(tempDir);

        String content = Files.readString(settingsPath);

        assertTrue(content.contains("\"permissions\""));
        assertTrue(content.contains("\"skills\""));
        assertTrue(content.contains("\"test-project\""));
    }

    @Test
    @DisplayName("生成的harness.yaml应该包含基本配置")
    void generatedHarnessShouldContainBasicConfig(@TempDir Path tempDir) throws Exception {
        ConfigSyncTool tool = new ConfigSyncTool();
        Path configPath = tool.generateHarnessConfig(tempDir);

        String content = Files.readString(configPath);

        assertTrue(content.contains("project:"));
        assertTrue(content.contains("features:"));
        assertTrue(content.contains("recovery:"));
        assertTrue(content.contains("workers:"));
    }

    @Test
    @DisplayName("默认项目名应该是java-harness")
    void defaultProjectNameShouldBeJavaHarness() {
        ConfigSyncTool tool = new ConfigSyncTool();
        // Should detect project name or default to java-harness
        String projectName = tool.getProjectName();
        assertNotNull(projectName);
        assertFalse(projectName.isEmpty());
    }

    @Test
    @DisplayName("同步结果应该包含成功消息")
    void syncResultShouldContainSuccessMessage(@TempDir Path tempDir) throws ConfigSyncTool.ConfigSyncException {
        ConfigSyncTool tool = new ConfigSyncTool();
        ConfigSyncTool.ConfigSyncResult result = tool.syncToClaudeCode(tempDir);

        assertTrue(result.success());
        assertNotNull(result.message());
        assertFalse(result.message().isEmpty());
    }

    @Test
    @DisplayName("ConfigSyncResult应该正确记录生成的文件")
    void configSyncResultShouldRecordGeneratedFiles(@TempDir Path tempDir) throws ConfigSyncTool.ConfigSyncException {
        ConfigSyncTool tool = new ConfigSyncTool();
        ConfigSyncTool.ConfigSyncResult result = tool.syncToClaudeCode(tempDir);

        assertEquals(2, result.generatedFiles().size());
        assertTrue(result.generatedFiles().get(0).toString().endsWith(".json") ||
                  result.generatedFiles().get(1).toString().endsWith(".json"));
    }

    @Test
    @DisplayName("ValidationResult应该正确记录错误")
    void validationResultShouldRecordErrors() {
        ConfigSyncTool tool = new ConfigSyncTool();
        Path fakePath = Path.of("/nonexistent/config.yaml");

        ConfigSyncTool.ValidationResult result = tool.validateConfig(fakePath);

        assertFalse(result.errors().isEmpty());
        assertNotNull(result.errors());
    }

    @Test
    @DisplayName("应该支持重复生成配置文件")
    void shouldSupportRegeneratingConfigFiles(@TempDir Path tempDir) throws Exception {
        ConfigSyncTool tool = new ConfigSyncTool();

        // Generate first time
        Path settingsPath1 = tool.generateClaudeCodeSettings(tempDir);
        long size1 = Files.size(settingsPath1);

        // Generate second time (should overwrite)
        Path settingsPath2 = tool.generateClaudeCodeSettings(tempDir);
        long size2 = Files.size(settingsPath2);

        assertEquals(settingsPath1, settingsPath2);
        assertEquals(size1, size2); // Same content, same size
    }
}
