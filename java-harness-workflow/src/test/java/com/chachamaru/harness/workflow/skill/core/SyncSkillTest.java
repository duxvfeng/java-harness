package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillExecutionException;
import com.chachamaru.harness.workflow.sync.SyncConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SyncSkill 测试类
 *
 * <p>测试完整的同步流程，包括：
 * <ul>
 *   <li>成功场景：所有组件正常工作</li>
 *   <li>错误场景：配置文件不存在、解析失败</li>
 *   <li>漂移检测：手动编辑检测</li>
 *   <li>部分失败：某个组件失败但继续执行</li>
 * </ul>
 */
class SyncSkillTest {

    @TempDir
    Path tempDir;

    private Path projectRoot;
    private Path hooksDir;
    private Path hooksJsonPath;
    private Path tomlPath;
    private SyncSkill syncSkill;

    @BeforeEach
    void setUp() throws IOException {
        projectRoot = tempDir;
        hooksDir = projectRoot.resolve("hooks");
        hooksJsonPath = hooksDir.resolve("hooks.json");
        tomlPath = projectRoot.resolve("harness.toml");
        syncSkill = new SyncSkill();

        // 创建 hooks 目录
        Files.createDirectories(hooksDir);

        // 创建示例 hooks.json
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
        Files.writeString(hooksJsonPath, hooksJson);
    }

    @AfterEach
    void tearDown() {
        // 清理由 @TempDir 自动处理
    }

    /**
     * 创建 SkillContext
     */
    private SkillContext createContext(Path projectRoot) {
        return SkillContext.builder()
                .userIntent("sync")
                .projectRoot(projectRoot)
                .build();
    }

    /**
     * 测试完整的同步流程成功场景
     */
    @Test
    void testExecute_Success() throws Exception {
        // 准备：创建完整的 harness.toml
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"
            description = "Test project for SyncSkill"

            [agent]
            default = "claude-sonnet-5"

            [env]
            NODE_ENV = "development"
            API_ENDPOINT = "https://api.example.com"

            [safety.permissions]
            allow = ["Read", "Write"]
            deny = ["Bash"]
            ask = ["NetworkAccess"]

            [safety.sandbox]
            failIfUnavailable = true

            [safety.sandbox.network]
            deniedDomains = ["example.com", "test.com"]

            [safety.sandbox.filesystem]
            denyRead = ["/etc", "/var"]
            allowRead = ["/tmp", "./src"]
            """;
        Files.writeString(tomlPath, tomlContent);

        // 执行
        SyncResult result = (SyncResult) syncSkill.execute(createContext(projectRoot));

        // 验证
        assertTrue(result.isSuccess(), "同步应该成功");
        assertEquals("同步成功完成", result.getMessage());
        assertEquals(2, result.getGeneratedFiles().size(), "应该生成2个文件");

        // 验证生成的文件路径
        List<String> generatedFiles = result.getGeneratedFiles();
        assertTrue(generatedFiles.stream().anyMatch(f -> f.contains("hooks.json")),
            "应该包含 hooks.json");
        assertTrue(generatedFiles.stream().anyMatch(f -> f.contains("settings.json")),
            "应该包含 settings.json");

        // 验证文件实际存在
        Path settingsPath = projectRoot.resolve(".claude-plugin").resolve("settings.json");
        assertTrue(Files.exists(settingsPath), "settings.json 应该被创建");

        Path hooksTargetPath = projectRoot.resolve(".claude-plugin").resolve("hooks.json");
        assertTrue(Files.exists(hooksTargetPath), "hooks.json 应该被复制");

        // 验证 settings.json 内容（基本检查）
        String settingsContent = Files.readString(settingsPath);
        assertTrue(settingsContent.contains("claude-sonnet-5"), "应该包含 agent 配置");
        assertTrue(settingsContent.contains("NODE_ENV"), "应该包含环境变量");
    }

    /**
     * 测试 harness.toml 不存在的场景
     */
    @Test
    void testExecute_MissingToml() {
        // 准备：不创建 harness.toml

        // 执行 & 验证
        SkillExecutionException exception = assertThrows(
            SkillExecutionException.class,
            () -> syncSkill.execute(createContext(projectRoot)),
            "应该抛出 SkillExecutionException"
        );

        assertTrue(exception.getMessage().contains("未找到 harness.toml"),
            "错误消息应该提到文件未找到");
    }

    /**
     * 测试 TOML 解析失败的场景
     */
    @Test
    void testExecute_InvalidToml() throws IOException {
        // 准备：创建无效的 TOML
        String invalidToml = """
            [project
            name = "test"
            # 缺少右括号
            """;
        Files.writeString(tomlPath, invalidToml);

        // 执行 & 验证
        SkillExecutionException exception = assertThrows(
            SkillExecutionException.class,
            () -> syncSkill.execute(createContext(projectRoot)),
            "应该抛出 SkillExecutionException"
        );

        assertTrue(exception.getMessage().contains("同步失败"),
            "错误消息应该提到同步失败");
    }

    /**
     * 测试配置漂移检测场景
     */
    @Test
    void testExecute_DriftDetection() throws Exception {
        // 准备：创建 harness.toml
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"

            [safety.sandbox.network]
            deniedDomains = ["newdomain.com"]
            """;
        Files.writeString(tomlPath, tomlContent);

        // 准备：手动编辑 settings.json（模拟漂移）
        Path pluginDir = projectRoot.resolve(".claude-plugin");
        Files.createDirectories(pluginDir);

        String oldSettings = """
            {
              "$schema": "https://json.schemastore.org/claude-code-settings.json",
              "sandbox": {
                "failIfUnavailable": false,
                "network": {
                  "deniedDomains": ["olddomain.com", "another.com"]
                }
              }
            }
            """;
        Files.writeString(pluginDir.resolve("settings.json"), oldSettings);

        // 执行
        SyncResult result = (SyncResult) syncSkill.execute(createContext(projectRoot));

        // 验证：应该成功，但有漂移警告
        assertTrue(result.isSuccess(), "同步应该成功（漂移不导致失败）");
        assertFalse(result.getDriftWarnings().isEmpty(), "应该有漂移警告");

        String warning = result.getDriftWarnings().get(0);
        assertTrue(warning.contains("drift detected"), "警告应该提到漂移");
        assertTrue(warning.contains("deniedDomains"), "警告应该提到 deniedDomains");
    }

    /**
     * 测试最小配置场景
     */
    @Test
    void testExecute_MinimalConfig() throws Exception {
        // 准备：创建最小配置
        String minimalToml = """
            [project]
            name = "minimal-project"
            version = "1.0.0"
            """;
        Files.writeString(tomlPath, minimalToml);

        // 执行
        SyncResult result = (SyncResult) syncSkill.execute(createContext(projectRoot));

        // 验证
        assertTrue(result.isSuccess(), "同步应该成功");
        assertEquals(2, result.getGeneratedFiles().size());

        // 验证生成的 settings.json 只包含必要字段
        Path settingsPath = projectRoot.resolve(".claude-plugin").resolve("settings.json");
        String settingsContent = Files.readString(settingsPath);

        // 应该有 $schema 字段
        assertTrue(settingsContent.contains("$schema"), "应该包含 $schema");
    }

    /**
     * 测试项目根目录不存在
     */
    @Test
    void testExecute_ProjectRootNotExists() {
        // 准备：使用不存在的路径
        Path nonExistentDir = projectRoot.resolve("non-existent");

        // 执行 & 验证
        SkillExecutionException exception = assertThrows(
            SkillExecutionException.class,
            () -> syncSkill.execute(createContext(nonExistentDir)),
            "应该抛出 SkillExecutionException"
        );

        assertTrue(exception.getMessage().contains("项目根目录不存在"),
            "错误消息应该提到目录不存在");
    }

    /**
     * 测试部分失败场景（hooks.json 不存在）
     */
    @Test
    void testExecute_PartialFailure_HooksMissing() throws Exception {
        // 准备：创建 harness.toml，但删除 hooks.json
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"
            """;
        Files.writeString(tomlPath, tomlContent);
        Files.deleteIfExists(hooksJsonPath);

        // 执行
        SyncResult result = (SyncResult) syncSkill.execute(createContext(projectRoot));

        // 验证：应该部分失败
        assertFalse(result.isSuccess(), "同步应该部分失败");
        assertTrue(result.getMessage().contains("hooks.json 同步失败"),
            "错误消息应该提到 hooks.json 失败");

        // 但 settings.json 仍应该生成
        Path settingsPath = projectRoot.resolve(".claude-plugin").resolve("settings.json");
        assertTrue(Files.exists(settingsPath), "settings.json 仍应该被生成");
    }

    /**
     * 测试首次生成（无漂移检测）
     */
    @Test
    void testExecute_FirstTimeGeneration() throws Exception {
        // 准备：创建 harness.toml，但不存在 settings.json
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"

            [safety.sandbox.network]
            deniedDomains = ["example.com"]
            """;
        Files.writeString(tomlPath, tomlContent);

        // 执行
        SyncResult result = (SyncResult) syncSkill.execute(createContext(projectRoot));

        // 验证：应该成功，无漂移警告
        assertTrue(result.isSuccess(), "同步应该成功");
        assertTrue(result.getDriftWarnings().isEmpty(),
            "首次生成应该无漂移警告");
    }
}
