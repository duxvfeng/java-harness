package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SyncSkill 注册测试
 * 验证 SyncSkill 可以正确注册到 SkillFramework 并执行
 */
class SyncSkillRegistrationTest {

    private SkillFramework skillFramework;

    @BeforeEach
    void setUp() {
        skillFramework = new SkillFramework();
        skillFramework.initialize();
    }

    @Test
    void testSyncSkillCanBeRegistered() {
        // 创建并注册 SyncSkill
        SyncSkill syncSkill = new SyncSkill();
        skillFramework.registerSkill(syncSkill);

        // 验证技能已注册
        assertTrue(skillFramework.isSkillRegistered("sync"),
            "SyncSkill 应该被注册到 SkillFramework");
        assertEquals(1, skillFramework.getSkillCount(),
            "SkillFramework 应该有1个注册的技能");
    }

    @Test
    void testSyncSkillCanBeFound() {
        // 注册 SyncSkill
        SyncSkill syncSkill = new SyncSkill();
        skillFramework.registerSkill(syncSkill);

        // 验证技能可以通过 ID 找到
        assertTrue(skillFramework.findSkill("sync").isPresent(),
            "应该能够通过 'sync' ID 找到 SyncSkill");

        assertEquals(syncSkill, skillFramework.findSkill("sync").get(),
            "找到的技能应该是注册的 SyncSkill 实例");
    }

    @Test
    void testSyncSkillMetadata() {
        // 注册 SyncSkill
        SyncSkill syncSkill = new SyncSkill();
        skillFramework.registerSkill(syncSkill);

        // 验证技能元数据
        assertTrue(skillFramework.getSkillMetadata("sync").isPresent(),
            "应该能够获取 SyncSkill 的元数据");

        var metadata = skillFramework.getSkillMetadata("sync").get();
        assertEquals("sync", metadata.getSkillId());
        assertEquals("Sync Skill", metadata.getSkillName());
        assertEquals("1.0.0-java", metadata.getVersion());
        assertEquals("从 harness.toml（SSOT）生成 Claude Code 插件配置文件",
            metadata.getDescription());
    }

    @Test
    void testSyncSkillImplementsSkillInterface() {
        // 验证 SyncSkill 实现了 Skill 接口
        SyncSkill syncSkill = new SyncSkill();

        assertDoesNotThrow(() -> {
            // 调用 Skill 接口的方法
            syncSkill.getSkillId();
            syncSkill.getSkillName();
            syncSkill.getVersion();
            syncSkill.getDescription();
        }, "SyncSkill 应该实现所有 Skill 接口的方法");

        // 验证返回值
        assertEquals("sync", syncSkill.getSkillId());
        assertEquals("Sync Skill", syncSkill.getSkillName());
        assertEquals("1.0.0-java", syncSkill.getVersion());
    }

    @Test
    void testSyncSkillCanExecuteViaFramework(@TempDir Path tempDir) throws Exception {
        // 注册 SyncSkill
        SyncSkill syncSkill = new SyncSkill();
        skillFramework.registerSkill(syncSkill);

        // 创建测试环境
        createTestEnvironment(tempDir);

        // 创建 SkillContext
        SkillContext context = SkillContext.builder()
            .userIntent("同步配置")
            .projectRoot(tempDir)
            .build();

        // 通过框架执行技能
        SkillResult result = skillFramework.executeSkill("sync", context);

        // 验证结果
        assertNotNull(result, "执行结果不应为 null");
        assertEquals("sync", result.getSkillId(), "结果应该包含技能ID");
    }

    /**
     * 创建测试环境
     */
    private void createTestEnvironment(Path tempDir) throws Exception {
        // 创建 hooks 目录和 hooks.json
        Path hooksDir = tempDir.resolve("hooks");
        java.nio.file.Files.createDirectories(hooksDir);

        String hooksJson = """
            {
              "preCommit": {
                "run": ["echo test"]
              }
            }
            """;
        java.nio.file.Files.writeString(hooksDir.resolve("hooks.json"), hooksJson);

        // 创建 harness.toml
        String tomlContent = """
            [project]
            name = "test-project"
            version = "1.0.0"

            [safety.permissions]
            allow = ["Read"]
            """;
        java.nio.file.Files.writeString(tempDir.resolve("harness.toml"), tomlContent);
    }
}
