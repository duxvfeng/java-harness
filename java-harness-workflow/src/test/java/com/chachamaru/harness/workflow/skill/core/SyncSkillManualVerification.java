package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillExecutionException;
import com.chachamaru.harness.workflow.sync.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * SyncSkill 手动验证脚本
 * 用于验证 SyncSkill 的基本功能
 */
public class SyncSkillManualVerification {
    public static void main(String[] args) {
        System.out.println("=== SyncSkill 手动验证 ===\n");

        boolean allPassed = true;

        // 测试1：验证 SyncSkill 类存在并可实例化
        System.out.println("测试1：验证 SyncSkill 类存在...");
        try {
            Class<?> syncSkillClass = Class.forName("com.chachamaru.harness.workflow.skill.core.SyncSkill");
            System.out.println("✅ SyncSkill 类加载成功");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ SyncSkill 类不存在: " + e.getMessage());
            allPassed = false;
        }

        // 测试2：验证 execute() 方法签名（现在接受 SkillContext）
        System.out.println("\n测试2：验证 execute() 方法签名...");
        try {
            java.lang.reflect.Method executeMethod = SyncSkill.class.getMethod("execute", SkillContext.class);
            System.out.println("✅ execute() 方法存在，签名: " + executeMethod);
        } catch (NoSuchMethodException e) {
            System.out.println("❌ execute() 方法不存在: " + e.getMessage());
            allPassed = false;
        }

        // 测试3：验证 Skill 接口实现
        System.out.println("\n测试3：验证 Skill 接口实现...");
        try {
            SyncSkill skill = new SyncSkill();
            System.out.println("✅ getSkillId() = " + skill.getSkillId());
            System.out.println("✅ getSkillName() = " + skill.getSkillName());
            System.out.println("✅ getVersion() = " + skill.getVersion());
            System.out.println("✅ getDescription() = " + skill.getDescription());
        } catch (Exception e) {
            System.out.println("❌ Skill 接口验证失败: " + e.getMessage());
            allPassed = false;
        }

        // 测试4：验证组件集成
        System.out.println("\n测试4：验证组件集成...");
        try {
            // 验证依赖的组件类存在
            Class.forName("com.chachamaru.harness.workflow.sync.ConfigReader");
            Class.forName("com.chachamaru.harness.workflow.sync.HooksSyncer");
            Class.forName("com.chachamaru.harness.workflow.sync.SettingsGenerator");
            Class.forName("com.chachamaru.harness.workflow.sync.DriftDetector");
            Class.forName("com.chachamaru.harness.workflow.sync.SyncConfig");
            System.out.println("✅ 所有依赖组件存在");
        } catch (ClassNotFoundException e) {
            System.out.println("❌ 依赖组件缺失: " + e.getMessage());
            allPassed = false;
        }

        // 测试5：创建临时测试环境并执行同步
        System.out.println("\n测试5：端到端功能测试...");
        try {
            Path tempDir = Files.createTempDirectory("sync-test-");

            // 创建测试目录结构
            Path hooksDir = tempDir.resolve("hooks");
            Files.createDirectories(hooksDir);

            // 创建测试 hooks.json
            String hooksJson = """
                {
                  "preCommit": {
                    "run": ["echo test"]
                  }
                }
                """;
            Files.writeString(hooksDir.resolve("hooks.json"), hooksJson);

            // 创建测试 harness.toml
            String tomlContent = """
                [project]
                name = "test-project"
                version = "1.0.0"
                description = "Test project"

                [agent]
                default = "claude-sonnet-5"

                [safety.permissions]
                allow = ["Read"]

                [safety.sandbox.network]
                deniedDomains = ["example.com"]
                """;
            Files.writeString(tempDir.resolve("harness.toml"), tomlContent);

            // 执行同步
            SyncSkill skill = new SyncSkill();
            SkillContext context = SkillContext.builder()
                    .userIntent("sync")
                    .projectRoot(tempDir)
                    .build();
            SyncResult result = (SyncResult) skill.execute(context);

            // 验证结果
            if (result.isSuccess()) {
                System.out.println("✅ 同步执行成功");
                System.out.println("   生成的文件: " + result.getGeneratedFiles());
                System.out.println("   消息: " + result.getMessage());
            } else {
                System.out.println("❌ 同步执行失败: " + result.getMessage());
                allPassed = false;
            }

            // 验证文件是否实际创建
            Path settingsPath = tempDir.resolve(".claude-plugin").resolve("settings.json");
            Path hooksTargetPath = tempDir.resolve(".claude-plugin").resolve("hooks.json");

            if (Files.exists(settingsPath)) {
                System.out.println("✅ settings.json 已创建");
            } else {
                System.out.println("❌ settings.json 未创建");
                allPassed = false;
            }

            if (Files.exists(hooksTargetPath)) {
                System.out.println("✅ hooks.json 已复制");
            } else {
                System.out.println("❌ hooks.json 未复制");
                allPassed = false;
            }

            // 清理
            deleteDirectory(tempDir);

        } catch (Exception e) {
            System.out.println("❌ 端到端测试失败: " + e.getMessage());
            e.printStackTrace();
            allPassed = false;
        }

        // 测试6：错误处理 - 缺少 harness.toml
        System.out.println("\n测试6：错误处理 - 缺少 harness.toml...");
        try {
            Path tempDir = Files.createTempDirectory("sync-error-test-");

            // 只创建 hooks，不创建 toml
            Path hooksDir = tempDir.resolve("hooks");
            Files.createDirectories(hooksDir);
            Files.writeString(hooksDir.resolve("hooks.json"), "{}");

            try {
                SyncSkill skill = new SyncSkill();
                SkillContext context = SkillContext.builder()
                        .userIntent("sync")
                        .projectRoot(tempDir)
                        .build();
                SyncResult result = (SyncResult) skill.execute(context);
                System.out.println("❌ 应该抛出 SkillExecutionException，但没有");
                allPassed = false;
            } catch (SkillExecutionException e) {
                if (e.getMessage().contains("未找到 harness.toml")) {
                    System.out.println("✅ 正确抛出 SkillExecutionException: " + e.getMessage());
                } else {
                    System.out.println("⚠️ 抛出 SkillExecutionException 但消息不正确: " + e.getMessage());
                }
            }

            deleteDirectory(tempDir);

        } catch (Exception e) {
            System.out.println("❌ 错误处理测试失败: " + e.getMessage());
            allPassed = false;
        }

        // 最终结果
        System.out.println("\n" + "=".repeat(40));
        if (allPassed) {
            System.out.println("✅ 所有验证通过");
            System.exit(0);
        } else {
            System.out.println("❌ 部分验证失败");
            System.exit(1);
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // 忽略
                    }
                });
        }
    }
}
