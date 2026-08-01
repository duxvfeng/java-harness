package com.chachamaru.harness.workflow.execution;

import com.chachamaru.harness.workflow.model.Task;
import com.chachamaru.harness.workflow.model.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParallelExecutor and CompletableFutureExecutor.
 */
class ParallelExecutorTest {

    private final CompletableFutureExecutor executor = new CompletableFutureExecutor(4);

    @Test
    void testExecuteParallel_EmptyList() throws ParallelExecutor.ExecutionException {
        CompletableFuture<com.chachamaru.harness.workflow.orchestration.ExecutionResult> future =
            executor.executeParallel(List.of(), (Function<Task, String>) task -> task.id());

        assertDoesNotThrow(() -> future.get());
        com.chachamaru.harness.workflow.orchestration.ExecutionResult result = future.join();
        assertEquals(0, result.metrics().totalTasks());
    }

    @Test
    void testExecuteParallel_SingleTask() throws ParallelExecutor.ExecutionException {
        Task task = new Task("1.1.1", "Task 1", "Description", Status.CC_TODO, "DoD", List.of(), "implementation");

        CompletableFuture<com.chachamaru.harness.workflow.orchestration.ExecutionResult> future =
            executor.executeParallel(List.of(task), t -> {
                try {
                    Thread.sleep(50);  // Reduced from 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return t.id();
            });

        assertDoesNotThrow(() -> future.get());
        com.chachamaru.harness.workflow.orchestration.ExecutionResult result = future.join();

        assertEquals(1, result.metrics().totalTasks());
        assertEquals(1, result.metrics().completedTasks());
        assertEquals(0, result.metrics().failedTasks());
    }

    @Test
    void testExecuteParallel_MultipleTasks() throws ParallelExecutor.ExecutionException {
        List<Task> tasks = List.of(
            new Task("1.1.1", "Task 1", "Description", Status.CC_TODO, "DoD", List.of(), "implementation"),
            new Task("1.1.2", "Task 2", "Description", Status.CC_TODO, "DoD", List.of(), "implementation"),
            new Task("1.1.3", "Task 3", "Description", Status.CC_TODO, "DoD", List.of(), "implementation")
        );

        CompletableFuture<com.chachamaru.harness.workflow.orchestration.ExecutionResult> future =
            executor.executeParallel(tasks, t -> {
                try {
                    Thread.sleep(30);  // Reduced from 50ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return t.id();
            });

        assertDoesNotThrow(() -> future.get());
        com.chachamaru.harness.workflow.orchestration.ExecutionResult result = future.join();

        assertEquals(3, result.metrics().totalTasks());
        assertEquals(3, result.metrics().completedTasks());
        assertTrue(result.isSuccess());
    }

    @Test
    void testExecuteParallel_WithDependencies() throws ParallelExecutor.ExecutionException {
        List<Task> tasks = List.of(
            new Task("1.1.1", "Task 1", "Description", Status.CC_TODO, "DoD", List.of(), "implementation"),
            new Task("1.1.2", "Task 2", "Description", Status.CC_TODO, "DoD", List.of("1.1.1"), "implementation"),
            new Task("1.1.3", "Task 3", "Description", Status.CC_TODO, "DoD", List.of("1.1.2"), "implementation")
        );

        List<String> executionOrder = new ArrayList<>();

        CompletableFuture<com.chachamaru.harness.workflow.orchestration.ExecutionResult> future =
            executor.executeParallel(tasks, t -> {
                executionOrder.add(t.id());
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return t.id();
            });

        assertDoesNotThrow(() -> future.get());
        com.chachamaru.harness.workflow.orchestration.ExecutionResult result = future.join();

        assertEquals(3, result.metrics().completedTasks());
        // Task 1.1.1 should execute before 1.1.2, and 1.1.2 before 1.1.3
        int index1 = executionOrder.indexOf("1.1.1");
        int index2 = executionOrder.indexOf("1.1.2");
        int index3 = executionOrder.indexOf("1.1.3");

        assertTrue(index1 < index2, "Task 1.1.1 should execute before 1.1.2");
        assertTrue(index2 < index3, "Task 1.1.2 should execute before 1.1.3");
    }

    @Test
    void testExecuteSequential() throws ParallelExecutor.ExecutionException {
        List<Task> tasks = List.of(
            new Task("1.1.1", "Task 1", "Description", Status.CC_TODO, "DoD", List.of(), "implementation"),
            new Task("1.1.2", "Task 2", "Description", Status.CC_TODO, "DoD", List.of(), "implementation")
        );

        com.chachamaru.harness.workflow.orchestration.ExecutionResult result =
            executor.executeSequential(tasks, t -> t.id());

        assertEquals(2, result.metrics().totalTasks());
        assertEquals(2, result.metrics().completedTasks());
        assertEquals(0.0, result.metrics().parallelizationEfficiency());
    }

    @Test
    void testGetMaxConcurrency() {
        assertEquals(4, executor.getMaxConcurrency());
    }

    @Test
    void testSetMaxConcurrency() {
        executor.setMaxConcurrency(8);
        assertEquals(8, executor.getMaxConcurrency());
    }

    @Test
    void testSetMaxConcurrency_Invalid() {
        assertThrows(IllegalArgumentException.class, () -> executor.setMaxConcurrency(0));
        assertThrows(IllegalArgumentException.class, () -> executor.setMaxConcurrency(-1));
    }

    @Test
    @Timeout(15)
    void testParallelIsFasterThanSequential() throws ParallelExecutor.ExecutionException {
        // Create 6 tasks, each taking 50ms (reduced from 100ms for faster test)
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            tasks.add(new Task(
                "1.1." + i,
                "Task " + i,
                "Description",
                Status.CC_TODO,
                "DoD",
                List.of(),
                "implementation"
            ));
        }

        // Execute sequentially
        long sequentialStart = System.currentTimeMillis();
        com.chachamaru.harness.workflow.orchestration.ExecutionResult sequentialResult =
            executor.executeSequential(tasks, t -> {
                try {
                    Thread.sleep(50);  // Reduced from 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return t.id();
            });
        long sequentialDuration = System.currentTimeMillis() - sequentialStart;

        // Execute in parallel
        long parallelStart = System.currentTimeMillis();
        CompletableFuture<com.chachamaru.harness.workflow.orchestration.ExecutionResult> parallelFuture =
            executor.executeParallel(tasks, t -> {
                try {
                    Thread.sleep(50);  // Reduced from 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return t.id();
            });

        assertDoesNotThrow(() -> parallelFuture.get());
        long parallelDuration = System.currentTimeMillis() - parallelStart;

        // Parallel should be > 2x faster than sequential
        double speedup = (double) sequentialDuration / parallelDuration;
        System.out.println("Sequential duration: " + sequentialDuration + "ms");
        System.out.println("Parallel duration: " + parallelDuration + "ms");
        System.out.println("Speedup: " + speedup + "x");

        assertTrue(speedup > 2.0,
            "Parallel execution should be >2x faster than sequential, but was only " + speedup + "x faster");

        assertEquals(6, sequentialResult.metrics().completedTasks());
        assertEquals(6, parallelFuture.join().metrics().completedTasks());
    }

    @Test
    void testExecuteParallel_WithConcurrencyLimit() throws ParallelExecutor.ExecutionException {
        List<Task> tasks = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            tasks.add(new Task(
                "1.1." + i,
                "Task " + i,
                "Description",
                Status.CC_TODO,
                "DoD",
                List.of(),
                "implementation"
            ));
        }

        // Limit to 2 concurrent tasks, reduced from 10 tasks to 6 for speed
        List<Task> limitedTasks = tasks.subList(0, 6);
        CompletableFuture<com.chachamaru.harness.workflow.orchestration.ExecutionResult> future =
            executor.executeParallel(limitedTasks, t -> {
                try {
                    Thread.sleep(30);  // Reduced from 50ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return t.id();
            }, 2);

        assertDoesNotThrow(() -> future.get());
        com.chachamaru.harness.workflow.orchestration.ExecutionResult result = future.join();

        assertEquals(6, result.metrics().completedTasks());
        assertTrue(result.isSuccess());
    }

    @Test
    void testExecuteParallel_TaskFailure() throws ParallelExecutor.ExecutionException {
        List<Task> tasks = List.of(
            new Task("1.1.1", "Task 1", "Description", Status.CC_TODO, "DoD", List.of(), "implementation"),
            new Task("1.1.2", "Failing Task", "Description", Status.CC_TODO, "DoD", List.of(), "implementation"),
            new Task("1.1.3", "Task 3", "Description", Status.CC_TODO, "DoD", List.of(), "implementation")
        );

        CompletableFuture<com.chachamaru.harness.workflow.orchestration.ExecutionResult> future =
            executor.executeParallel(tasks, t -> {
                if (t.title().contains("Failing")) {
                    throw new RuntimeException("Task failed!");
                }
                return t.id();
            });

        assertDoesNotThrow(() -> future.get());
        com.chachamaru.harness.workflow.orchestration.ExecutionResult result = future.join();

        assertEquals(3, result.metrics().totalTasks());
        assertEquals(2, result.metrics().completedTasks());
        assertEquals(1, result.metrics().failedTasks());
    }
}
