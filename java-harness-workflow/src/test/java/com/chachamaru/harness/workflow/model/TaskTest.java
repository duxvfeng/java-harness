package com.chachamaru.harness.workflow.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Task model.
 */
class TaskTest {

    @Test
    void testCreateTodo() {
        Task task = Task.createTodo("1.1.1", "Test Task", "Description");

        assertEquals("1.1.1", task.id());
        assertEquals("Test Task", task.title());
        assertEquals("Description", task.description());
        assertEquals(Status.CC_TODO, task.status());
        assertEquals("implementation", task.lane());
        assertTrue(task.dependencies().isEmpty());
    }

    @Test
    void testCreateWithDependencies() {
        Task task = Task.create(
            "1.1.1",
            "Test Task",
            "Description",
            "DoD criteria",
            List.of("1.0.1", "1.0.2"),
            "implementation"
        );

        assertEquals(2, task.dependencies().size());
        assertTrue(task.dependsOn("1.0.1"));
        assertTrue(task.dependsOn("1.0.2"));
        assertFalse(task.dependsOn("1.0.3"));
    }

    @Test
    void testValidation_NullId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Task(null, "Title", "Description", Status.CC_TODO, null, List.of(), "implementation")
        );
    }

    @Test
    void testValidation_BlankId() {
        assertThrows(IllegalArgumentException.class, () ->
            new Task("", "Title", "Description", Status.CC_TODO, null, List.of(), "implementation")
        );
    }

    @Test
    void testValidation_NullTitle() {
        assertThrows(IllegalArgumentException.class, () ->
            new Task("1.1.1", null, "Description", Status.CC_TODO, null, List.of(), "implementation")
        );
    }

    @Test
    void testValidation_NullStatus() {
        assertThrows(IllegalArgumentException.class, () ->
            new Task("1.1.1", "Title", "Description", null, null, List.of(), "implementation")
        );
    }

    @Test
    void testWithStatus() {
        Task task = Task.createTodo("1.1.1", "Test", "Desc");
        Task wipTask = task.withStatus(Status.CC_WIP);

        assertEquals(Status.CC_WIP, wipTask.status());
        assertEquals(task.id(), wipTask.id());
    }

    @Test
    void testMarkAsWip() {
        Task task = Task.createTodo("1.1.1", "Test", "Desc");
        Task wipTask = task.markAsWip();

        assertEquals(Status.CC_WIP, wipTask.status());
    }

    @Test
    void testMarkAsDone() {
        Task task = Task.createTodo("1.1.1", "Test", "Desc");
        Task doneTask = task.markAsDone();

        assertEquals(Status.CC_DONE, doneTask.status());
    }

    @Test
    void testAreDependenciesSatisfied_AllMet() {
        Task dep1 = Task.createTodo("1.0.1", "Dep 1", "D1").markAsDone();
        Task dep2 = Task.createTodo("1.0.2", "Dep 2", "D2").markAsDone();
        Task task = Task.create("1.1.1", "Task", "Desc", "DoD", List.of("1.0.1", "1.0.2"), "implementation");

        assertTrue(task.areDependenciesSatisfied(List.of(dep1, dep2, task)));
    }

    @Test
    void testAreDependenciesSatisfied_Unmet() {
        Task dep1 = Task.createTodo("1.0.1", "Dep 1", "D1"); // Not done
        Task task = Task.create("1.1.1", "Task", "Desc", "DoD", List.of("1.0.1"), "implementation");

        assertFalse(task.areDependenciesSatisfied(List.of(dep1, task)));
    }

    @Test
    void testAreDependenciesSatisfied_NoDependencies() {
        Task task = Task.createTodo("1.1.1", "Task", "Desc");
        assertTrue(task.areDependenciesSatisfied(List.of(task)));
    }
}
