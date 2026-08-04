package com.chachamaru.harness.workflow.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DriftDetector 测试")
class DriftDetectorTest {

    @Test
    @DisplayName("当文件内容相同时应该返回空列表")
    void testCheckDrift_NoDrift(@TempDir Path tempDir) throws Exception {
        Path settingsPath = tempDir.resolve(".claude-plugin");
        Files.createDirectories(settingsPath);
        Files.writeString(settingsPath.resolve("settings.json"), "{\"agent\":\"claude-opus-5\"}");

        byte[] newContent = "{\"agent\":\"claude-opus-5\"}".getBytes();

        List<String> warnings = DriftDetector.check(tempDir.toFile(), newContent);

        assertTrue(warnings.isEmpty(), "无漂移时应该返回空列表");
    }

    @Test
    @DisplayName("当文件内容不同时应该检测到漂移")
    void testCheckDrift_Detected(@TempDir Path tempDir) throws Exception {
        Path settingsPath = tempDir.resolve(".claude-plugin");
        Files.createDirectories(settingsPath);
        Files.writeString(settingsPath.resolve("settings.json"), "{\"agent\":\"claude-opus-5\"}");

        byte[] newContent = "{\"agent\":\"claude-sonnet-5\"}".getBytes();

        List<String> warnings = DriftDetector.check(tempDir.toFile(), newContent);

        assertEquals(1, warnings.size(), "应该检测到漂移");
        assertTrue(warnings.get(0).contains("drift detected"), "警告应该包含 'drift detected'");
    }

    @Test
    @DisplayName("当现有文件不存在时应该返回空列表")
    void testCheckDrift_NoExistingFile(@TempDir Path tempDir) throws Exception {
        byte[] newContent = "{\"agent\":\"claude-opus-5\"}".getBytes();

        List<String> warnings = DriftDetector.check(tempDir.toFile(), newContent);

        assertTrue(warnings.isEmpty(), "无现有文件时应该返回空列表（首次生成）");
    }

    @Test
    @DisplayName("应该检测 deniedDomains 数量变化")
    void testCheckDrift_DeniedDomainsChanged(@TempDir Path tempDir) throws Exception {
        Path settingsPath = tempDir.resolve(".claude-plugin");
        Files.createDirectories(settingsPath);

        // 现有文件有 2 个 deniedDomains
        String existingJson = """
            {
              "sandbox": {
                "network": {
                  "deniedDomains": ["169.254.169.254", "metadata.google.internal"]
                }
              }
            }
            """;
        Files.writeString(settingsPath.resolve("settings.json"), existingJson);

        // 新内容只有 1 个 deniedDomain
        String newJson = """
            {
              "sandbox": {
                "network": {
                  "deniedDomains": ["169.254.169.254"]
                }
              }
            }
            """;
        byte[] newContent = newJson.getBytes();

        List<String> warnings = DriftDetector.check(tempDir.toFile(), newContent);

        assertEquals(1, warnings.size(), "应该检测到漂移");
        assertTrue(warnings.get(0).contains("drift detected"), "警告应该包含 'drift detected'");
        assertTrue(warnings.get(0).contains("2 -> 1"), "应该显示 deniedDomains 数量变化");
        assertTrue(warnings.get(0).contains("REMOVED"), "应该提示条目被移除");
    }

    @Test
    @DisplayName("当 deniedDomains 增加时应该提示添加")
    void testCheckDrift_DeniedDomainsAdded(@TempDir Path tempDir) throws Exception {
        Path settingsPath = tempDir.resolve(".claude-plugin");
        Files.createDirectories(settingsPath);

        // 现有文件有 1 个 deniedDomain
        String existingJson = """
            {
              "sandbox": {
                "network": {
                  "deniedDomains": ["169.254.169.254"]
                }
              }
            }
            """;
        Files.writeString(settingsPath.resolve("settings.json"), existingJson);

        // 新内容有 2 个 deniedDomains
        String newJson = """
            {
              "sandbox": {
                "network": {
                  "deniedDomains": ["169.254.169.254", "metadata.google.internal"]
                }
              }
            }
            """;
        byte[] newContent = newJson.getBytes();

        List<String> warnings = DriftDetector.check(tempDir.toFile(), newContent);

        assertEquals(1, warnings.size(), "应该检测到漂移");
        assertTrue(warnings.get(0).contains("1 -> 2"), "应该显示 deniedDomains 数量增加");
    }

    @Test
    @DisplayName("当没有 deniedDomains 时应该使用通用警告")
    void testCheckDrift_NoDeniedDomains(@TempDir Path tempDir) throws Exception {
        Path settingsPath = tempDir.resolve(".claude-plugin");
        Files.createDirectories(settingsPath);
        Files.writeString(settingsPath.resolve("settings.json"), "{\"agent\":\"claude-opus-5\"}");

        byte[] newContent = "{\"agent\":\"claude-sonnet-5\",\"env\":{}}".getBytes();

        List<String> warnings = DriftDetector.check(tempDir.toFile(), newContent);

        assertEquals(1, warnings.size(), "应该检测到漂移");
        assertTrue(warnings.get(0).contains("git diff"), "应该建议使用 git diff 查看");
    }

    @Test
    @DisplayName("应该忽略空白字符的差异")
    void testCheckDrift_IgnoreWhitespace(@TempDir Path tempDir) throws Exception {
        Path settingsPath = tempDir.resolve(".claude-plugin");
        Files.createDirectories(settingsPath);

        // 现有文件有额外空白
        String existingJson = "{\n  \"agent\" : \"claude-opus-5\"\n}";
        Files.writeString(settingsPath.resolve("settings.json"), existingJson);

        // 新内容空白不同但语义相同
        byte[] newContent = "{\"agent\":\"claude-opus-5\"}".getBytes();

        List<String> warnings = DriftDetector.check(tempDir.toFile(), newContent);

        assertTrue(warnings.isEmpty(), "应该忽略空白字符差异");
    }
}
