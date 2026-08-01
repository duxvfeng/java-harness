package com.chachamaru.harness.workflow.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlansDocument model.
 */
class PlansDocumentTest {

    @Test
    void testEmptyDocument() {
        PlansDocument doc = PlansDocument.empty("Test Plan");

        assertEquals("Test Plan", doc.title());
        assertTrue(doc.tasks().isEmpty());
        assertNotNull(doc.lastModified());
    }

    @Test
    void testValidation_NullTitle() {
        assertThrows(IllegalArgumentException.class, () ->
            new PlansDocument(null, null, LocalDateTime.now(), List.of())
        );
    }

    @Test
    void testValidation_BlankTitle() {
        assertThrows(IllegalArgumentException.class, () ->
            new PlansDocument("", null, LocalDateTime.now(), List.of())
        );
    }

    @Test
    void testGetTasksByStatus() {
        Task todoTask = Task.createTodo("1.1.1", "TODO", "Desc");
        Task doneTask = Task.createTodo("1.1.2", "DONE", "Desc").markAsDone();
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(todoTask, doneTask));

        List<Task> todoTasks = doc.getTasksByStatus(Status.CC_TODO);
        List<Task> doneTasks = doc.getTasksByStatus(Status.CC_DONE);

        assertEquals(1, todoTasks.size());
        assertEquals(1, doneTasks.size());
        assertEquals("1.1.1", todoTasks.get(0).id());
        assertEquals("1.1.2", doneTasks.get(0).id());
    }

    @Test
    void testGetTasksByLane() {
        Task implTask = Task.create("1.1.1", "Impl", "D", "DoD", List.of(), "implementation");
        Task reviewTask = Task.create("1.1.2", "Review", "D", "DoD", List.of(), "review");
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(implTask, reviewTask));

        List<Task> implTasks = doc.getTasksByLane("implementation");
        List<Task> reviewTasks = doc.getTasksByLane("review");

        assertEquals(1, implTasks.size());
        assertEquals(1, reviewTasks.size());
    }

    @Test
    void testGetReadyTasks_NoDependencies() {
        Task task = Task.createTodo("1.1.1", "Task", "Desc");
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(task));

        List<Task> ready = doc.getReadyTasks();
        assertEquals(1, ready.size());
        assertEquals("1.1.1", ready.get(0).id());
    }

    @Test
    void testGetReadyTasks_WithSatisfiedDependencies() {
        Task dep = Task.createTodo("1.0.1", "Dep", "D").markAsDone();
        Task task = Task.create("1.1.1", "Task", "Desc", "DoD", List.of("1.0.1"), "implementation");
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(dep, task));

        List<Task> ready = doc.getReadyTasks();
        assertEquals(1, ready.size());
        assertEquals("1.1.1", ready.get(0).id());
    }

    @Test
    void testGetReadyTasks_WithUnsatisfiedDependencies() {
        Task dep = Task.createTodo("1.0.1", "Dep", "D"); // Not done, but no dependencies
        Task task = Task.create("1.1.1", "Task", "Desc", "DoD", List.of("1.0.1"), "implementation");
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(dep, task));

        List<Task> ready = doc.getReadyTasks();
        // dep should be ready (TODO with no dependencies), but task should not
        assertEquals(1, ready.size());
        assertEquals("1.0.1", ready.get(0).id());
    }

    @Test
    void testGetTaskById() {
        Task task1 = Task.createTodo("1.1.1", "Task 1", "D1");
        Task task2 = Task.createTodo("1.1.2", "Task 2", "D2");
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(task1, task2));

        assertEquals(task1, doc.getTaskById("1.1.1"));
        assertEquals(task2, doc.getTaskById("1.1.2"));
        assertNull(doc.getTaskById("1.1.3"));
    }

    @Test
    void testIsComplete_AllDone() {
        Task task1 = Task.createTodo("1.1.1", "Task 1", "D1").markAsDone();
        Task task2 = Task.createTodo("1.1.2", "Task 2", "D2").markAsDone();
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(task1, task2));

        assertTrue(doc.isComplete());
    }

    @Test
    void testIsComplete_SomeNotDone() {
        Task task1 = Task.createTodo("1.1.1", "Task 1", "D1").markAsDone();
        Task task2 = Task.createTodo("1.1.2", "Task 2", "D2"); // Not done
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(task1, task2));

        assertFalse(doc.isComplete());
    }

    @Test
    void testWithUpdatedTask() {
        Task original = Task.createTodo("1.1.1", "Task", "Desc");
        PlansDocument doc = new PlansDocument("Test", null, LocalDateTime.now(), List.of(original));

        Task updated = original.markAsWip();
        PlansDocument updatedDoc = doc.withUpdatedTask(updated);

        assertEquals(Status.CC_WIP, updatedDoc.getTaskById("1.1.1").status());
        assertEquals(Status.CC_TODO, doc.getTaskById("1.1.1").status()); // Original unchanged
    }
}
