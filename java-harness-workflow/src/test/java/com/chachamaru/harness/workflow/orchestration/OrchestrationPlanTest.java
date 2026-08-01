package com.chachamaru.harness.workflow.orchestration;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OrchestrationPlan model.
 */
class OrchestrationPlanTest {

    @Test
    void testCreation() {
        var plan = new OrchestrationPlan(
            "plan-1",
            "Plans.md",
            List.of(),
            List.of(),
            List.of(),
            OrchestrationPlan.ExecutionStrategy.SEQUENTIAL
        );

        assertEquals("plan-1", plan.planId());
        assertEquals(OrchestrationPlan.ExecutionStrategy.SEQUENTIAL, plan.strategy());
    }

    @Test
    void testValidation_NullPlanId() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrchestrationPlan(null, "Plans.md", List.of(), List.of(), List.of(), null)
        );
    }

    @Test
    void testValidation_BlankPlanId() {
        assertThrows(IllegalArgumentException.class, () ->
            new OrchestrationPlan("  ", "Plans.md", List.of(), List.of(), List.of(), null)
        );
    }

    @Test
    void testDefaultStrategy() {
        var plan = new OrchestrationPlan(
            "plan-1",
            "Plans.md",
            List.of(),
            List.of(),
            List.of(),
            null
        );

        assertEquals(OrchestrationPlan.ExecutionStrategy.SEQUENTIAL, plan.strategy());
    }

    @Test
    void testTotalTaskCount() {
        var plan = new OrchestrationPlan(
            "plan-1",
            "Plans.md",
            List.of(mockTask("1.1.1"), mockTask("1.1.2"), mockTask("1.1.3")),
            List.of(),
            List.of(),
            OrchestrationPlan.ExecutionStrategy.SEQUENTIAL
        );

        assertEquals(3, plan.totalTaskCount());
    }

    @Test
    void testCompletionPercentage() {
        var tasks = List.of(
            mockCompletedTask("1.1.1"),
            mockCompletedTask("1.1.2"),
            mockTask("1.1.3")
        );

        var plan = new OrchestrationPlan(
            "plan-1",
            "Plans.md",
            tasks,
            List.of(),
            List.of(),
            OrchestrationPlan.ExecutionStrategy.SEQUENTIAL
        );

        assertEquals(2.0 / 3.0, plan.completionPercentage(), 0.01);
    }

    @Test
    void testCompletionPercentage_Empty() {
        var plan = new OrchestrationPlan(
            "plan-1",
            "Plans.md",
            List.of(),
            List.of(),
            List.of(),
            OrchestrationPlan.ExecutionStrategy.SEQUENTIAL
        );

        assertEquals(1.0, plan.completionPercentage());
    }

    @Test
    void testIsComplete() {
        var tasks = List.of(
            mockCompletedTask("1.1.1"),
            mockCompletedTask("1.1.2")
        );

        var plan = new OrchestrationPlan(
            "plan-1",
            "Plans.md",
            tasks,
            List.of(),
            List.of(),
            OrchestrationPlan.ExecutionStrategy.SEQUENTIAL
        );

        assertTrue(plan.isComplete());
    }

    @Test
    void testIs_NotComplete() {
        var tasks = List.of(
            mockCompletedTask("1.1.1"),
            mockTask("1.1.2")
        );

        var plan = new OrchestrationPlan(
            "plan-1",
            "Plans.md",
            tasks,
            List.of(),
            List.of(),
            OrchestrationPlan.ExecutionStrategy.SEQUENTIAL
        );

        assertFalse(plan.isComplete());
    }

    private com.chachamaru.harness.workflow.model.Task mockTask(String id) {
        return new com.chachamaru.harness.workflow.model.Task(
            id, "Task " + id, "Description",
            com.chachamaru.harness.workflow.model.Status.CC_TODO,
            "DoD", List.of(), "implementation"
        );
    }

    private com.chachamaru.harness.workflow.model.Task mockCompletedTask(String id) {
        return new com.chachamaru.harness.workflow.model.Task(
            id, "Task " + id, "Description",
            com.chachamaru.harness.workflow.model.Status.CC_DONE,
            "DoD", List.of(), "implementation"
        );
    }
}
