package com.chachamaru.harness.workflow.skill.core;

import com.chachamaru.harness.workflow.sync.ConfigReader;
import com.chachamaru.harness.workflow.sync.DriftDetector;
import com.chachamaru.harness.workflow.sync.HooksSyncer;
import com.chachamaru.harness.workflow.sync.SettingsGenerator;
import com.chachamaru.harness.workflow.sync.SyncConfig;
import com.chachamaru.harness.workflow.skill.framework.Skill;
import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillExecutionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * SyncSkill - 同步技能主协调器
 *
 * <p>从 harness.toml（SSOT）生成 Claude Code 插件配置文件：
 * <ul>
 *   <li>.claude-plugin/hooks.json - 从 hooks/hooks.json 复制</li>
 *   <li>.claude-plugin/settings.json - 从配置生成</li>
 * </ul>
 *
 * <p>执行流程：
 * <pre>
 * 1. 读取 harness.toml → ConfigReader.parse() → SyncConfig
 * 2. 同步 hooks.json → HooksSyncer.sync() → .claude-plugin/hooks.json
 * 3. 生成 settings.json → SettingsGenerator.generate() → .claude-plugin/settings.json
 * 4. 检测配置漂移 → DriftDetector.check() → 警告列表
 * 5. 返回 SyncResult
 * </pre>
 *
 * <p>错误处理：
 * <ul>
 *   <li>harness.toml 不存在 → 失败，返回详细错误</li>
 *   <li>TOML 解析失败 → 失败，返回详细错误</li>
 *   <li>组件部分失败 → 收集所有错误，批量报告</li>
 *   <li>配置漂移 → 警告到 driftWarnings，不失败</li>
 * </ul>
 *
 * @see ConfigReader
 * @see HooksSyncer
 * @see SettingsGenerator
 * @see DriftDetector
 * @see SyncResult
 * @since 4.0.0-java
 */
public class SyncSkill implements Skill {
    private static final String SKILL_ID = "sync";
    private static final String SKILL_NAME = "Sync Skill";
    private static final String VERSION = "1.0.0-java";

    @Override
    public String getSkillId() {
        return SKILL_ID;
    }

    @Override
    public String getSkillName() {
        return SKILL_NAME;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getDescription() {
        return "从 harness.toml（SSOT）生成 Claude Code 插件配置文件";
    }

    /**
     * 执行完整的同步流程
     *
     * @param context 技能执行上下文，必须包含 projectRoot
     * @return 同步结果，包含生成的文件列表和漂移警告
     * @throws SkillExecutionException 如果同步失败（配置文件不存在、解析失败等）
     */
    @Override
    public Object execute(SkillContext context) throws SkillExecutionException {
        // 从上下文中获取项目根目录
        File projectRoot = context.getProjectRoot().toFile();

        if (projectRoot == null || !projectRoot.exists()) {
            throw new SkillExecutionException(SKILL_ID, null,
                    "项目根目录不存在: " + projectRoot, null);
        }

        SyncResult.Builder resultBuilder = SyncResult.builder();
        List<String> errors = new ArrayList<>();

        try {
            // 1. 查找并读取 harness.toml
            Path tomlPath = findHarnessToml(projectRoot);
            if (tomlPath == null) {
                throw new SkillExecutionException(SKILL_ID, null,
                    "未找到 harness.toml 文件。已搜索路径: " +
                    projectRoot.toPath().resolve("harness.toml") + ", " +
                    projectRoot.toPath().resolve("src/test/resources/harness.toml"), null);
            }

            // 2. 解析配置
            SyncConfig config = ConfigReader.parse(tomlPath.toFile());

            // 3. 同步 hooks.json
            try {
                String hooksPath = HooksSyncer.sync(projectRoot);
                resultBuilder.addGeneratedFile(hooksPath);
            } catch (IOException e) {
                errors.add("hooks.json 同步失败: " + e.getMessage());
            }

            // 4. 生成 settings.json（需要先读取内容用于漂移检测）
            Path settingsPath = projectRoot.toPath().resolve(".claude-plugin").resolve("settings.json");
            byte[] existingSettingsContent = null;
            if (Files.exists(settingsPath)) {
                existingSettingsContent = Files.readAllBytes(settingsPath);
            }

            try {
                String generatedSettingsPath = SettingsGenerator.generate(projectRoot, config);
                resultBuilder.addGeneratedFile(generatedSettingsPath);

                // 5. 读取新生成的 settings.json 内容用于漂移检测
                byte[] newSettingsContent = Files.readAllBytes(Path.of(generatedSettingsPath));

                // 检测配置漂移（使用之前读取的旧内容）
                List<String> driftWarnings = DriftDetector.check(existingSettingsContent, newSettingsContent);
                for (String warning : driftWarnings) {
                    resultBuilder.addDriftWarning(warning);
                }
            } catch (IOException e) {
                errors.add("settings.json 生成失败: " + e.getMessage());
            }

            // 6. 处理错误
            if (!errors.isEmpty()) {
                resultBuilder.success(false);
                resultBuilder.message("同步部分失败: " + String.join("; ", errors));
            } else {
                resultBuilder.message("同步成功完成");
            }

        } catch (IOException e) {
            throw new SkillExecutionException(SKILL_ID, null, "同步失败: " + e.getMessage(), e);
        }

        return resultBuilder.build();
    }

    /**
     * 查找 harness.toml 文件
     *
     * <p>搜索顺序：
     * <ol>
     *   <li>项目根目录/harness.toml</li>
     *   <li>src/test/resources/harness.toml（测试环境）</li>
     * </ol>
     *
     * @param projectRoot 项目根目录
     * @return 找到的文件路径，如果未找到则返回 null
     */
    private static Path findHarnessToml(File projectRoot) {
        Path tomlPath = projectRoot.toPath().resolve("harness.toml");
        if (Files.exists(tomlPath)) {
            return tomlPath;
        }

        // 测试环境路径
        Path testTomlPath = projectRoot.toPath().resolve("src/test/resources/harness.toml");
        if (Files.exists(testTomlPath)) {
            return testTomlPath;
        }

        return null;
    }
}
