package com.chachamaru.harness.workflow.orchestration;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExecutionResult model.
 */
class ExecutionResultTest {

    @Test
    void testCreation() {
        var outcomes = List.of(
            new ExecutionResult.TaskOutcome("1.1.1", ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS, "OK", 100, LocalDateTime.now())
        );

        var metrics = new ExecutionResult.ExecutionMetrics(1, 1, 0, 0, 1000, 0.8);

        var result = new ExecutionResult(
            "exec-1",
            "plan-1",
            ExecutionResult.ExecutionStatus.SUCCESS,
            outcomes,
            metrics,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null
        );

        assertEquals("exec-1", result.executionId());
        assertEquals("plan-1", result.planId());
        assertTrue(result.isSuccess());
    }

    @Test
    void testValidation_NullExecutionId() {
        assertThrows(IllegalArgumentException.class, () ->
            new ExecutionResult(null, "plan-1", ExecutionResult.ExecutionStatus.RUNNING, List.of(), null, LocalDateTime.now(), null, null)
        );
    }

    @Test
    void testValidation_NullPlanId() {
        assertThrows(IllegalArgumentException.class, () ->
            new ExecutionResult("exec-1", null, ExecutionResult.ExecutionStatus.RUNNING, List.of(), null, LocalDateTime.now(), null, null)
        );
    }

    @Test
    void testValidation_InvalidParallelizationEfficiency() {
        assertThrows(IllegalArgumentException.class, () ->
            new ExecutionResult.ExecutionMetrics(1, 1, 0, 0, 100, 1.5)
        );
    }

    @Test
    void testSuccessFactory() {
        var outcomes = List.of(new ExecutionResult.TaskOutcome("1.1.1", ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS, "OK", 100, LocalDateTime.now()));
        var metrics = new ExecutionResult.ExecutionMetrics(1, 1, 0, 0, 100, 1.0);

        var result = ExecutionResult.success("exec-1", "plan-1", outcomes, metrics, LocalDateTime.now());

        assertTrue(result.isSuccess());
        assertEquals(ExecutionResult.ExecutionStatus.SUCCESS, result.status());
        assertNotNull(result.endTime());
    }

    @Test
    void testFailureFactory() {
        var result = ExecutionResult.failure("exec-1", "plan-1", "Task failed", LocalDateTime.now());

        assertFalse(result.isSuccess());
        assertEquals(ExecutionResult.ExecutionStatus.FAILED, result.status());
        assertEquals("Task failed", result.failureReason());
    }

    @Test
    void testSuccessRate() {
        var outcomes = List.of(
            new ExecutionResult.TaskOutcome("1.1.1", ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS, "OK", 100, LocalDateTime.now()),
            new ExecutionResult.TaskOutcome("1.1.2", ExecutionResult.TaskOutcome.TaskOutcomeStatus.FAILED, "Error", 50, LocalDateTime.now()),
            new ExecutionResult.TaskOutcome("1.1.3", ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS, "OK", 100, LocalDateTime.now())
        );

        var result = new ExecutionResult(
            "exec-1", "plan-1", ExecutionResult.ExecutionStatus.SUCCESS,
            outcomes, null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertEquals(2.0 / 3.0, result.successRate(), 0.01);
    }

    @Test
    void testSuccessRate_Empty() {
        var result = new ExecutionResult(
            "exec-1", "plan-1", ExecutionResult.ExecutionStatus.SUCCESS,
            List.of(), null, LocalDateTime.now(), LocalDateTime.now(), null
        );

        assertEquals(0.0, result.successRate());
    }

    @Test
    void testTaskOutcomeRecord() {
        var outcome = new ExecutionResult.TaskOutcome(
            "1.1.1",
            ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS,
            "Completed successfully",
            1500,
            LocalDateTime.now()
        );

        assertEquals("1.1.1", outcome.taskId());
        assertEquals(ExecutionResult.TaskOutcome.TaskOutcomeStatus.SUCCESS, outcome.status());
        assertEquals(1500, outcome.durationMs());
    }
}
