package com.chachamaru.harness.workflow.integration;

import com.chachamaru.harness.workflow.parser.PlansParser;
import com.chachamaru.harness.workflow.parser.RegexPlansParser;
import com.chachamaru.harness.workflow.models.PlansDocument;
import com.chachamaru.harness.workflow.models.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端工作流集成测试
 * 验证Plans.md解析和工作流执行
 */
class EndToEndWorkflowTest {

    private PlansParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new RegexPlansParser();
    }

    @Test
    void testPlansParsing() throws IOException {
        // 1. 创建Plans.md文件
        Path plansFile = tempDir.resolve("Plans.md");
        String plansContent = """
            # 测试计划

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 1.1 | 创建基础模块 | 单元测试通过 | - | TODO |
            | 1.2 | 创建核心模块 | 单元测试通过 | 1.1 | TODO |
            | 1.3 | 集成测试 | 集成测试通过 | 1.2 | TODO |
            """;
        Files.writeString(plansFile, plansContent);

        // 2. 解析Plans.md
        PlansDocument plansDocument = parser.parse(plansFile.toString());

        // 3. 验证解析结果
        assertNotNull(plansDocument);
        assertEquals(3, plansDocument.getTasks().size());

        // 验证第一个任务
        Task task1 = plansDocument.getTasks().get(0);
        assertEquals("1.1", task1.getId());
        assertEquals("创建基础模块", task1.getContent());
        assertEquals("单元测试通过", task1.getDod());
        assertTrue(task1.getDependencies().isEmpty());
        assertEquals(Task.Status.TODO, task1.getStatus());

        // 验证依赖关系
        Task task2 = plansDocument.getTasks().get(1);
        assertEquals("1.2", task2.getId());
        assertEquals(List.of("1.1"), task2.getDependencies());
    }

    @Test
    void testWorkflowWithDependencies() throws IOException {
        // 创建有依赖关系的Plans.md
        Path plansFile = tempDir.resolve("Plans.md");
        String plansContent = """
            # 依赖测试计划

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 2.1 | 创建模块A | 单元测试通过 | - | TODO |
            | 2.2 | 创建模块B | 单元测试通过 | 2.1 | TODO |
            | 2.3 | 集成测试AB | 集成测试通过 | 2.1,2.2 | TODO |
            """;
        Files.writeString(plansFile, plansContent);

        PlansDocument plansDocument = parser.parse(plansFile.toString());

        // 验证任务数量
        assertEquals(3, plansDocument.getTasks().size());

        // 验证无依赖任务
        List<Task> rootTasks = plansDocument.getRootTasks();
        assertEquals(1, rootTasks.size());
        assertEquals("2.1", rootTasks.get(0).getId());

        // 验证依赖链
        Task task2 = plansDocument.getTasks().get(1);
        assertEquals(1, task2.getDependencies().size());
        assertTrue(task2.getDependencies().contains("2.1"));

        Task task3 = plansDocument.getTasks().get(2);
        assertEquals(2, task3.getDependencies().size());
        assertTrue(task3.getDependencies().contains("2.1"));
        assertTrue(task3.getDependencies().contains("2.2"));
    }

    @Test
    void testDifferentStatusValues() throws IOException {
        // 测试不同状态值
        Path plansFile = tempDir.resolve("Plans.md");
        String plansContent = """
            # 状态测试计划

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 3.1 | 已完成任务 | 测试通过 | - | DONE |
            | 3.2 | 进行中任务 | 测试通过 | 3.1 | IN_PROGRESS |
            | 3.3 | 待办任务 | 测试通过 | 3.2 | TODO |
            """;
        Files.writeString(plansFile, plansContent);

        PlansDocument plansDocument = parser.parse(plansFile.toString());

        assertEquals(3, plansDocument.getTasks().size());
        assertEquals(Task.Status.DONE, plansDocument.getTasks().get(0).getStatus());
        assertEquals(Task.Status.IN_PROGRESS, plansDocument.getTasks().get(1).getStatus());
        assertEquals(Task.Status.TODO, plansDocument.getTasks().get(2).getStatus());
    }

    @Test
    void testComplexMarkdownFormat() throws IOException {
        // 测试复杂Markdown格式
        Path plansFile = tempDir.resolve("Plans.md");
        String plansContent = """
            # 复杂项目计划

            ## 阶段 1: 基础设施

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 1.1 | 创建项目结构 | 结构验证通过 | - | cc:✅ abc123 |

            ## 阶段 2: 核心功能

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 2.1 | 实现核心API | API测试通过 | 1.1 | IN_PROGRESS |
            | 2.2 | 用户界面 | UI测试通过 | 2.1 | TODO |

            ### 验收标准
            - 所有测试通过
            - 性能达标
            """;
        Files.writeString(plansFile, plansContent);

        PlansDocument plansDocument = parser.parse(plansFile.toString());

        // 应该解析出所有任务
        assertTrue(plansDocument.getTasks().size() >= 2);

        // 验证能找到具体任务
        boolean foundTask = plansDocument.getTasks().stream()
            .anyMatch(t -> t.getId().equals("2.1") && t.getContent().equals("实现核心API"));
        assertTrue(foundTask, "应该找到任务2.1");
    }

    @Test
    void testEmptyAndMinimalPlans() throws IOException {
        // 测试空计划和最小计划
        Path emptyPlans = tempDir.resolve("Empty.md");
        Files.writeString(emptyPlans, "# 空计划\n\n没有任务表格");

        PlansDocument emptyDocument = parser.parse(emptyPlans.toString());
        assertNotNull(emptyDocument);
        assertTrue(emptyDocument.getTasks().isEmpty());

        // 测试最小计划
        Path minimalPlans = tempDir.resolve("Minimal.md");
        Files.writeString(minimalPlans, """
            # 最小计划

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 1.1 | 单个任务 | 完成 | - | TODO |
            """);

        PlansDocument minimalDocument = parser.parse(minimalPlans.toString());
        assertEquals(1, minimalDocument.getTasks().size());
        assertEquals("1.1", minimalDocument.getTasks().get(0).getId());
    }

    @Test
    void testSpecialCharactersInContent() throws IOException {
        // 测试特殊字符处理
        Path plansFile = tempDir.resolve("Special.md");
        String plansContent = """
            # 特殊字符测试

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 4.1 | 测试API: /api/v1/users | 验证通过 | - | TODO |
            | 4.2 | 配置文件config.yaml | 配置生效 | 4.1 | TODO |
            | 4.3 | 支持<code>代码</code>标记 | 渲染正确 | 4.2 | TODO |
            """;
        Files.writeString(plansFile, plansContent);

        PlansDocument plansDocument = parser.parse(plansFile.toString());
        assertEquals(3, plansDocument.getTasks().size());

        // 验证特殊字符被正确处理
        Task task1 = plansDocument.getTasks().get(0);
        assertTrue(task1.getContent().contains("/api/v1/users"));

        Task task2 = plansDocument.getTasks().get(1);
        assertTrue(task2.getContent().contains("config.yaml"));
    }
}