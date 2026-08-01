package com.chachamaru.harness.workflow.parser;

import com.chachamaru.harness.workflow.model.PlansDocument;
import com.chachamaru.harness.workflow.model.Status;
import com.chachamaru.harness.workflow.model.Task;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegexPlansParser.
 */
class RegexPlansParserTest {

    private final RegexPlansParser parser = new RegexPlansParser();

    @Test
    void testParseBasicTable() throws PlansParser.ParseException {
        String content = """
            # Test Plan

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1.1 | Task 1 | DoD criteria | - | cc:TODO |
            | 1.1.2 | Task 2 | DoD criteria | 1.1.1 | cc:WIP |
            """;

        PlansDocument doc = parser.parseString(content, "test.md");

        assertNotNull(doc);
        assertEquals("Test Plan", doc.title());
        assertEquals(2, doc.tasks().size());

        Task task1 = doc.tasks().get(0);
        assertEquals("1.1.1", task1.id());
        assertEquals("Task 1", task1.title());
        assertEquals(Status.CC_TODO, task1.status());
        assertTrue(task1.dependencies().isEmpty());

        Task task2 = doc.tasks().get(1);
        assertEquals("1.1.2", task2.id());
        assertEquals(Status.CC_WIP, task2.status());
        assertTrue(task2.dependsOn("1.1.1"));
    }

    @Test
    void testParseStatusMarkers() throws PlansParser.ParseException {
        String content = """
            # Status Test

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 2.1.1 | TODO task | DoD | - | cc:TODO |
            | 2.1.2 | WIP task | DoD | - | cc:WIP |
            | 2.1.3 | DONE task | DoD | - | cc:DONE |
            | 2.1.4 | Done with checkmark | DoD | - | cc:✅ |
            | 2.1.5 | Withdrawn task | DoD | - | cc:WITHDRAWN |
            """;

        PlansDocument doc = parser.parseString(content, "test.md");

        assertEquals(5, doc.tasks().size());
        assertEquals(Status.CC_TODO, doc.tasks().get(0).status());
        assertEquals(Status.CC_WIP, doc.tasks().get(1).status());
        assertEquals(Status.CC_DONE, doc.tasks().get(2).status());
        assertEquals(Status.CC_DONE, doc.tasks().get(3).status());
        assertEquals(Status.CC_WITHDRAWN, doc.tasks().get(4).status());
    }

    @Test
    void testParseMultipleDependencies() throws PlansParser.ParseException {
        String content = """
            # Deps Test

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1.1 | Task 1 | DoD | - | cc:TODO |
            | 1.1.2 | Task 2 | DoD | 1.1.1 | cc:TODO |
            | 1.1.3 | Task 3 | DoD | 1.1.1, 1.1.2 | cc:TODO |
            """;

        PlansDocument doc = parser.parseString(content, "test.md");

        assertEquals(3, doc.tasks().size());
        assertTrue(doc.tasks().get(0).dependencies().isEmpty());
        assertEquals(List.of("1.1.1"), doc.tasks().get(1).dependencies());
        assertEquals(List.of("1.1.1", "1.1.2"), doc.tasks().get(2).dependencies());
    }

    @Test
    void testParseEmptyTable() throws PlansParser.ParseException {
        String content = """
            # Empty Plan

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            """;

        PlansDocument doc = parser.parseString(content, "test.md");

        assertNotNull(doc);
        assertEquals("Empty Plan", doc.title());
        assertTrue(doc.tasks().isEmpty());
    }

    @Test
    void testParseWithSpaces() throws PlansParser.ParseException {
        String content = """
            # Space Test

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            |  1.1.1  |  Task with spaces  |  DoD  |  -  |  cc:TODO  |
            """;

        PlansDocument doc = parser.parseString(content, "test.md");

        assertEquals(1, doc.tasks().size());
        assertEquals("1.1.1", doc.tasks().get(0).id());
        assertEquals("Task with spaces", doc.tasks().get(0).title());
    }

    @Test
    void testParseFile(@TempDir Path tempDir) throws Exception {
        Path plansFile = tempDir.resolve("Plans.md");
        Files.writeString(plansFile, """
            # File Test

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1.1 | File task | DoD | - | cc:TODO |
            """);

        PlansDocument doc = parser.parse(plansFile);

        assertNotNull(doc);
        assertEquals("File Test", doc.title());
        assertEquals(1, doc.tasks().size());
    }

    @Test
    void testLaneDetermination() throws PlansParser.ParseException {
        String content = """
            # Lane Test

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1.1 | Implement feature | DoD | - | cc:TODO |
            | 1.1.2 | Review code | DoD | - | cc:TODO |
            | 1.1.3 | Release version | DoD | - | cc:TODO |
            | 1.1.4 | 验证功能 | DoD | - | cc:TODO |
            """;

        PlansDocument doc = parser.parseString(content, "test.md");

        assertEquals("implementation", doc.tasks().get(0).lane());
        assertEquals("review", doc.tasks().get(1).lane());
        assertEquals("release", doc.tasks().get(2).lane());
        assertEquals("review", doc.tasks().get(3).lane()); // Chinese "验证"
    }

    @Test
    void testParseRealPlansMd() throws PlansParser.ParseException {
        String content = """
            # Java Harness 实施计划

            **Purpose**: 将Java Harness从35-40%功能实现度扩展到与Go项目功能对等（90%+）

            ## 阶段 1：基础架构重构

            ### Phase 1.1：创建Maven父项目和多模块结构

            | Task | 内容 | DoD | Depends | Status |
            |------|------|-----|---------|--------|
            | 1.1.1 | 创建根POM文件 | 创建pom.xml，定义9个模块依赖管理 | `mvn help:effective-pom`执行成功，无错误 | - | cc:✅ 74ed47c |
            | 1.1.2 | 创建foundation模块 | 创建模块结构、DTO类 | `cd java-harness-foundation && mvn test`通过 | 1.1.1 | cc:✅ 9cf7f03 |
            | 1.1.3 | 创建protocol模块 | 创建HookEventType枚举、HookHandler接口 | `cd java-harness-protocol && mvn test`通过 | 1.1.2 | cc:✅ |
            """;

        PlansDocument doc = parser.parseString(content, "Plans.md");

        System.out.println("Title: " + doc.title());
        System.out.println("Parsed " + doc.tasks().size() + " tasks from test content");
        doc.tasks().forEach(t -> System.out.println("  - " + t.id() + ": " + t.title() + " [status=" + t.status() + ", deps=" + t.dependencies() + "]"));

        assertEquals("Java Harness 实施计划", doc.title());
        assertTrue(doc.tasks().size() >= 3);

        // Check first task (done with commit hash)
        Task task1 = doc.tasks().get(0);
        System.out.println("Task1 ID: " + task1.id() + ", Status: " + task1.status());
        assertEquals("1.1.1", task1.id());
        assertEquals(Status.CC_DONE, task1.status());

        // Check second task with dependency
        Task task2 = doc.tasks().get(1);
        System.out.println("Task2 ID: " + task2.id() + ", dependsOn(1.1.1): " + task2.dependsOn("1.1.1"));
        assertEquals("1.1.2", task2.id());
        assertTrue(task2.dependsOn("1.1.1"));
    }

    @Test
    void testParseExceptionHandling() {
        String content = """
            # Test

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | invalid row format | DoD | - | cc:TODO |
            """;

        assertDoesNotThrow(() -> parser.parseString(content, "test.md"));
        // Invalid rows are simply skipped, not exceptions
    }

    @Test
    void testGetReadyTasksAfterParsing() throws PlansParser.ParseException {
        String content = """
            # Ready Tasks Test

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1.1 | Foundation | DoD | - | cc:✅ |
            | 1.1.2 | Protocol | DoD | 1.1.1 | cc:TODO |
            | 1.1.3 | Workflow | DoD | 1.1.2 | cc:TODO |
            """;

        PlansDocument doc = parser.parseString(content, "test.md");

        List<Task> ready = doc.getReadyTasks();
        assertEquals(1, ready.size());
        assertEquals("1.1.2", ready.get(0).id());
    }
}
