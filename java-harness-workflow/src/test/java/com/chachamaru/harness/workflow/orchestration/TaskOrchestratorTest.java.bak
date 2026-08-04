package com.chachamaru.harness.workflow.orchestration;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TaskOrchestrator interface contract.
 */
class TaskOrchestratorTest {

    @Test
    void testInterfaceExists() {
        // Test that the interface can be instantiated
        TaskOrchestrator orchestrator = new TaskOrchestrator() {
            @Override
            public OrchestrationPlan createPlan(com.chachamaru.harness.workflow.model.PlansDocument plans) throws OrchestrationException {
                return null;
            }

            @Override
            public ExecutionResult execute(OrchestrationPlan plan) throws OrchestrationException {
                return null;
            }

            @Override
            public void pause(String executionId) throws OrchestrationException {
            }

            @Override
            public ExecutionResult resume(String executionId) throws OrchestrationException {
                return null;
            }

            @Override
            public void cancel(String executionId) throws OrchestrationException {
            }
        };

        assertNotNull(orchestrator);
    }

    @Test
    void testOrchestrationException() {
        var ex = new TaskOrchestrator.OrchestrationException("Test failure", "exec-1", "plan-1");

        assertEquals("Test failure", ex.getMessage());
        assertEquals("exec-1", ex.getExecutionId());
        assertEquals("plan-1", ex.getPlanId());
    }

    @Test
    void testOrchestrationException_WithCause() {
        Throwable cause = new RuntimeException("Inner error");
        var ex = new TaskOrchestrator.OrchestrationException("Test failure", cause, "exec-1", "plan-1");

        assertEquals("Test failure", ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals("exec-1", ex.getExecutionId());
    }

    @Test
    void testExecutionStrategyEnum() {
        assertEquals(3, OrchestrationPlan.ExecutionStrategy.values().length);

        assertTrue(List.of(OrchestrationPlan.ExecutionStrategy.values()).contains(OrchestrationPlan.ExecutionStrategy.SEQUENTIAL));
        assertTrue(List.of(OrchestrationPlan.ExecutionStrategy.values()).contains(OrchestrationPlan.ExecutionStrategy.PARALLEL));
        assertTrue(List.of(OrchestrationPlan.ExecutionStrategy.values()).contains(OrchestrationPlan.ExecutionStrategy.HYBRID));
    }

    @Test
    void testExecutionStatusEnum() {
        assertEquals(5, ExecutionResult.ExecutionStatus.values().length);

        assertTrue(List.of(ExecutionResult.ExecutionStatus.values()).contains(ExecutionResult.ExecutionStatus.RUNNING));
        assertTrue(List.of(ExecutionResult.ExecutionStatus.values()).contains(ExecutionResult.ExecutionStatus.SUCCESS));
        assertTrue(List.of(ExecutionResult.ExecutionStatus.values()).contains(ExecutionResult.ExecutionStatus.FAILED));
        assertTrue(List.of(ExecutionResult.ExecutionStatus.values()).contains(ExecutionResult.ExecutionStatus.CANCELLED));
        assertTrue(List.of(ExecutionResult.ExecutionStatus.values()).contains(ExecutionResult.ExecutionStatus.PAUSED));
    }
}
