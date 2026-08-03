package com.chachamaru.harness.parser;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for PlansParser.
 * Following TDD approach: write failing tests first, then implement.
 */
public class PlansParserTest {

    @Test
    void testParseSimpleTaskList() {
        String markdown = """
            # Phase 1: Test Phase

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1 | Task one | Test complete | - | todo |
            | 1.2 | Task two | Test complete | 1.1 | done |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        assertEquals("1.1", tasks.get(0).getId());
        assertEquals("Task one", tasks.get(0).getContent());
        assertEquals("Test complete", tasks.get(0).getDod());
        assertEquals("-", tasks.get(0).getDepends());
        assertEquals("todo", tasks.get(0).getStatus());
    }

    @Test
    void testParseWithCheckboxes() {
        String markdown = """
            ## TASK-001: First Task
            - [x] Subtask 1
            - [ ] Subtask 2

            ## TASK-002: Second Task
            Depends on: TASK-001
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        assertEquals("TASK-001", tasks.get(0).getId());
        assertEquals("First Task", tasks.get(0).getTitle());
    }

    @Test
    void testParseWithDependencyGraph() {
        String markdown = """
            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 8.1.1 | Hook one | Hook test | - | cc:completed ✅ |
            | 8.1.2 | Hook two | Hook test | 8.1.1 | cc:completed ✅ |
            | 8.1.3 | Hook three | Hook test | 8.1.2 | cc:TODO |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        List<TaskDependency> dependencies = PlansParser.extractDependencies(tasks);

        assertNotNull(dependencies);
        assertEquals(2, dependencies.size()); // Only 8.1.2 and 8.1.3 have dependencies
        assertEquals("8.1.2", dependencies.get(0).getTaskId());
        assertEquals("8.1.1", dependencies.get(0).getDependsOn());
        assertEquals("8.1.3", dependencies.get(1).getTaskId());
        assertEquals("8.1.2", dependencies.get(1).getDependsOn());
    }

    @Test
    void testParsePhaseStructure() {
        String markdown = """
            ## Phase 8.1: Hook System Implementation

            ### Goal
            Implement all 13+ hooks from Go version

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 8.1.1 | Implement PreToolUse | Hook test | - | cc:completed ✅ |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertEquals("8.1.1", tasks.get(0).getId());
        assertTrue(tasks.get(0).getContent().contains("PreToolUse"));
    }

    @Test
    void testParseEmptyContent() {
        String markdown = "";

        List<Task> tasks = PlansParser.parse(markdown);
        assertNotNull(tasks);
        assertTrue(tasks.isEmpty());
    }

    @Test
    void testParseWithComplexStatus() {
        String markdown = """
            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1 | Task one | Test | - | cc:completed ✅ 99c8b8c |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        assertNotNull(tasks);
        assertEquals(1, tasks.size());
        assertEquals("cc:completed ✅ 99c8b8c", tasks.get(0).getStatus());
    }

    @Test
    void testExtractTaskMetadata() {
        String markdown = """
            ## Phase 1: Command Redesign

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1 | Create Main.java | Entry point | - | done |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        Optional<Task> task = PlansParser.findTaskById(tasks, "1.1");

        assertTrue(task.isPresent());
        assertEquals("Create Main.java", task.get().getContent());
        assertEquals("Entry point", task.get().getDod());
    }

    @Test
    void testValidateTaskDependencies() {
        String markdown = """
            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1 | Base task | Base | - | done |
            | 1.2 | Dependent task | Dep | 1.1 | todo |
            | 1.3 | Another dependent | Dep | 1.2 | todo |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        boolean isValid = PlansParser.validateDependencies(tasks);

        assertTrue(isValid);
    }

    @Test
    void testDetectCircularDependencies() {
        String markdown = """
            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 1.1 | Task one | One | 1.3 | todo |
            | 1.2 | Task two | Two | 1.1 | todo |
            | 1.3 | Task three | Three | 1.2 | todo |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        boolean isValid = PlansParser.validateDependencies(tasks);

        assertFalse(isValid); // Should detect circular dependency
    }

    @Test
    void testParseRealPlansMdFormat() {
        // This should match the actual Plans.md format in the project
        String markdown = """
            ## Phase 8.1: Hook System Implementation（4 weeks）

            ### Goal
            Implement all 13+ hooks from Go version

            | Task | Content | DoD | Depends | Status |
            |------|---------|-----|---------|--------|
            | 8.1.1 | Implement PreToolUse hook | pre-tool validation | - | cc:completed ✅ |
            | 8.1.2 | Implement PostToolUse hook | post-tool detection | 8.1.1 | cc:completed ✅ |
            """;

        List<Task> tasks = PlansParser.parse(markdown);
        assertNotNull(tasks);
        assertEquals(2, tasks.size());
        assertEquals("8.1.1", tasks.get(0).getId());
        assertEquals("Implement PreToolUse hook", tasks.get(0).getContent());
        assertEquals("pre-tool validation", tasks.get(0).getDod());
        assertEquals("-", tasks.get(0).getDepends());
        assertEquals("cc:completed ✅", tasks.get(0).getStatus());
    }
}