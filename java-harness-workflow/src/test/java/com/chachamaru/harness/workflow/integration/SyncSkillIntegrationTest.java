package com.chachamaru.harness.workflow.integration;

import com.chachamaru.harness.workflow.skill.core.SyncSkill;
import com.chachamaru.harness.workflow.skill.core.SyncResult;
import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SyncSkill 端到端集成测试
 * <p>
 * 验证完整的同步流程，包括：
 * <ul>
 *   <li>配置文件解析</li>
 *   <li>Hooks 复制</li>
 *   <li>Settings 生成</li>
 *   <li>配置漂移检测</li>
 * </ul>
 */
class SyncSkillIntegrationTest {

    @TempDir
    Path tempDir;

    private File projectRoot;
    private SyncSkill syncSkill;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        projectRoot = tempDir.toFile();
        syncSkill = new SyncSkill();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        // 清理由 @TempDir 自动处理
    }

    /**
     * 测试完整配置的端到端同步
     */
    @Test
    void testEndToEndSync_WithFullConfig() throws Exception {
        // 准备：创建完整的测试环境
        createFullTestEnvironment();

        // 执行：通过 SkillContext 执行同步
        SkillContext context = SkillContext.builder()
            .userIntent("同步配置")
            .projectRoot(tempDir)
            .build();

        SyncResult result = (SyncResult) syncSkill.execute(context);

        // 验证：同步成功
        assertTrue(result.isSuccess(), "同步应该成功");
        assertEquals("同步成功完成", result.getMessage());
        assertEquals(2, result.getGeneratedFiles().size(), "应该生成2个文件");

        // 验证：hooks.json 已复制
        Path hooksTargetPath = tempDir.resolve(".claude-plugin").resolve("hooks.json");
        assertTrue(Files.exists(hooksTargetPath), "hooks.json 应该被创建");

        JsonNode hooksJson = objectMapper.readTree(hooksTargetPath.toFile());
        assertTrue(hooksJson.has("preCommit"), "hooks.json 应该包含 preCommit");
        assertTrue(hooksJson.has("prePush"), "hooks.json 应该包含 prePush");

        // 验证：settings.json 已生成
        Path settingsPath = tempDir.resolve(".claude-plugin").resolve("settings.json");
        assertTrue(Files.exists(settingsPath), "settings.json 应该被创建");

        JsonNode settingsJson = objectMapper.readTree(settingsPath.toFile());

        // 验证 agent 配置
        assertEquals("claude-sonnet-5", settingsJson.get("agent").asText(),
            "agent 应该设置为 claude-sonnet-5");

        // 验证 env 配置
        assertTrue(settingsJson.has("env"), "settings.json 应该包含 env");
        assertEquals("development", settingsJson.get("env").get("NODE_ENV").asText());
        assertEquals("https://api.example.com", settingsJson.get("env").get("API_ENDPOINT").asText());

        // 验证 permissions 配置
        assertTrue(settingsJson.has("permissions"), "settings.json 应该包含 permissions");
        JsonNode permissions = settingsJson.get("permissions");
        assertTrue(permissions.has("allow"));
        assertTrue(permissions.has("deny"));
        assertTrue(permissions.has("ask"));

        // 验证 sandbox 配置
        assertTrue(settingsJson.has("sandbox"), "settings.json 应该包含 sandbox");
        JsonNode sandbox = settingsJson.get("sandbox");
        assertTrue(sandbox.get("failIfUnavailable").asBoolean());

        // 验证 sandbox.network 配置
        assertTrue(sandbox.has("network"));
        JsonNode network = sandbox.get("network");
        assertTrue(network.has("deniedDomains"));

        // 验证 sandbox.filesystem 配置
        assertTrue(sandbox.has("filesystem"));
        JsonNode filesystem = sandbox.get("filesystem");
        assertTrue(filesystem.has("denyRead"));
        assertTrue(filesystem.has("allowRead"));
    }

    /**
     * 测试最小配置的端到端同步
     */
    @Test
    void testEndToEndSync_WithMinimalConfig() throws Exception {
        // 准备：创建最小配置
        createMinimalTestEnvironment();

        // 执行
        SkillContext context = SkillContext.builder()
            .userIntent("同步配置")
            .projectRoot(tempDir)
            .build();

        SyncResult result = (SyncResult) syncSkill.execute(context);

        // 验证
        assertTrue(result.isSuccess());
        assertEquals(2, result.getGeneratedFiles().size());

        // 验证生成的 settings.json 只包含必要字段
        Path settingsPath = tempDir.resolve(".claude-plugin").resolve("settings.json");
        JsonNode settingsJson = objectMapper.readTree(settingsPath.toFile());

        // 应该有 $schema 字段
        assertTrue(settingsJson.has("$schema"), "应该包含 $schema");

        // 最小配置可能没有 agent 字段
        // 验证 JSON 结构基本正确
        assertNotNull(settingsJson);
    }

    /**
     * 测试配置漂移检测
     */
    @Test
    void testEndToEndSync_DriftDetection() throws Exception {
        // 准备：创建测试环境和手动编辑的 settings.json
        createFullTestEnvironment();

        // 手动创建旧的 settings.json
        Path pluginDir = tempDir.resolve(".claude-plugin");
        Files.createDirectories(pluginDir);

        String oldSettings = """
            {
              "$schema": "https://json.schemastore.org/claude-code-settings.json",
              "sandbox": {
                "failIfUnavailable": false,
                "network": {
                  "deniedDomains": ["olddomain.com", "another.com", "extra.com"]
                }
              }
            }
            """;
        Files.writeString(pluginDir.resolve("settings.json"), oldSettings);

        // 执行
        SkillContext context = SkillContext.builder()
            .userIntent("同步配置")
            .projectRoot(tempDir)
            .build();

        SyncResult result = (SyncResult) syncSkill.execute(context);

        // 验证：同步成功但有漂移警告
        assertTrue(result.isSuccess(), "同步应该成功（漂移不导致失败）");
        assertFalse(result.getDriftWarnings().isEmpty(), "应该有漂移警告");

        // 验证漂移警告内容
        String warning = result.getDriftWarnings().get(0);
        assertTrue(warning.contains("drift detected"), "警告应该提到漂移");
        assertTrue(warning.contains("deniedDomains"), "警告应该提到 deniedDomains");
    }

    /**
     * 测试错误处理 - 缺少 harness.toml
     */
    @Test
    void testErrorHandling_MissingToml() {
        // 准备：只创建 hooks，不创建 toml
        try {
            Path hooksDir = tempDir.resolve("hooks");
            Files.createDirectories(hooksDir);
            Files.writeString(hooksDir.resolve("hooks.json"), "{}");
        } catch (Exception e) {
            fail("准备测试环境失败: " + e.getMessage());
        }

        // 执行 & 验证
        SkillContext context = SkillContext.builder()
            .userIntent("同步配置")
            .projectRoot(tempDir)
            .build();

        assertThrows(com.chachamaru.harness.workflow.skill.framework.SkillExecutionException.class,
            () -> syncSkill.execute(context),
            "应该抛出 SkillExecutionException");
    }

    /**
     * 测试错误处理 - hooks.json 不存在
     */
    @Test
    void testErrorHandling_MissingHooks() throws Exception {
        // 准备：创建 toml，但不创建 hooks
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), tomlContent);

        // 执行
        SkillContext context = SkillContext.builder()
            .userIntent("同步配置")
            .projectRoot(tempDir)
            .build();

        SyncResult result = (SyncResult) syncSkill.execute(context);

        // 验证：部分失败
        assertFalse(result.isSuccess(), "同步应该部分失败");
        assertTrue(result.getMessage().contains("hooks.json 同步失败"),
            "错误消息应该提到 hooks.json 失败");

        // 但 settings.json 仍应该生成
        Path settingsPath = tempDir.resolve(".claude-plugin").resolve("settings.json");
        assertTrue(Files.exists(settingsPath), "settings.json 仍应该被生成");
    }

    /**
     * 测试首次生成（无漂移检测）
     */
    @Test
    void testFirstTimeGeneration_NoDrift() throws Exception {
        // 准备：创建 toml，但不创建 settings.json
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"

            [safety.sandbox.network]
            deniedDomains = ["example.com"]
            """;
        Files.writeString(tempDir.resolve("harness.toml"), tomlContent);

        // 创建 hooks
        Path hooksDir = tempDir.resolve("hooks");
        Files.createDirectories(hooksDir);
        Files.writeString(hooksDir.resolve("hooks.json"), "{}");

        // 执行
        SkillContext context = SkillContext.builder()
            .userIntent("同步配置")
            .projectRoot(tempDir)
            .build();

        SyncResult result = (SyncResult) syncSkill.execute(context);

        // 验证：成功，无漂移警告
        assertTrue(result.isSuccess());
        assertTrue(result.getDriftWarnings().isEmpty(),
            "首次生成应该无漂移警告");
    }

    /**
     * 创建完整测试环境
     */
    private void createFullTestEnvironment() throws Exception {
        // 创建 hooks 目录和文件
        Path hooksDir = tempDir.resolve("hooks");
        Files.createDirectories(hooksDir);

        String hooksJson = """
            {
              "preCommit": {
                "run": ["npm run lint", "npm test"]
              },
              "prePush": {
                "run": ["npm run build"]
              }
            }
            """;
        Files.writeString(hooksDir.resolve("hooks.json"), hooksJson);

        // 创建完整配置的 harness.toml
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"
            description = "Test project for integration testing"
            author_name = "Test Author"
            author_url = "https://example.com"
            homepage = "https://example.com"
            repository = "https://github.com/example/test"
            license = "MIT"
            keywords = ["test", "example"]
            output_styles = ["json", "text"]

            [agent]
            default = "claude-sonnet-5"

            [env]
            NODE_ENV = "development"
            API_ENDPOINT = "https://api.example.com"
            DEBUG = "true"

            [safety.permissions]
            allow = ["Read", "Write"]
            deny = ["Bash", "NetworkAccess"]
            ask = ["Execute"]

            [safety.sandbox]
            fail_if_unavailable = true

            [safety.sandbox.network]
            denied_domains = ["example.com", "malicious.com"]

            [safety.sandbox.filesystem]
            deny_read = ["/etc", "/var"]
            allow_read = ["/tmp", "./src"]
            """;
        Files.writeString(tempDir.resolve("harness.toml"), tomlContent);
    }

    /**
     * 创建最小测试环境
     */
    private void createMinimalTestEnvironment() throws Exception {
        // 创建 hooks 目录和文件
        Path hooksDir = tempDir.resolve("hooks");
        Files.createDirectories(hooksDir);
        Files.writeString(hooksDir.resolve("hooks.json"), "{}");

        // 创建最小配置的 harness.toml
        String tomlContent = """
            [project]
            name = "minimal-project"
            version = "1.0.0"
            """;
        Files.writeString(tempDir.resolve("harness.toml"), tomlContent);
    }
}
