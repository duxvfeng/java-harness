package com.chachamaru.harness.collaboration.skill.impl;

import com.chachamaru.harness.collaboration.skill.SkillExecutionException;
import com.chachamaru.harness.collaboration.skill.SkillRegistry;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.protocol.model.Task;
import com.chachamaru.harness.protocol.model.Status;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for core skills implementation.
 */
class CoreSkillsTest {

    @Test
    void testPlanSkillCreation() {
        PlanSkill skill = new PlanSkill();

        assertEquals("plan", skill.getId());
        assertEquals("Plan Skill", skill.getName());
        assertEquals("Skill for planning and creating project plans", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion());
    }

    @Test
    void testWorkSkillCreation() {
        WorkSkill skill = new WorkSkill();

        assertEquals("work", skill.getId());
        assertEquals("Work Skill", skill.getName());
        assertEquals("Skill for executing work tasks", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion());
    }

    @Test
    void testReviewSkillCreation() {
        ReviewSkill skill = new ReviewSkill();

        assertEquals("review", skill.getId());
        assertEquals("Review Skill", skill.getName());
        assertEquals("Skill for reviewing code and implementation", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion());
    }

    @Test
    void testWorkSkillExecution() throws SkillExecutionException {
        WorkSkill skill = new WorkSkill();
        Task task = Task.createTodo("task-1", "Test Task", "Test description");
        SkillContext context = new SkillContext(
            "work",
            "Work Skill",
            task,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of()
        );

        SkillResult result = skill.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.output());
    }

    @Test
    void testWorkSkillWithDependencies() throws SkillExecutionException {
        WorkSkill skill = new WorkSkill();
        Task task = new Task(
            "task-2",
            "Task with deps",
            "Description",
            Status.CC_TODO,
            null,
            java.util.List.of("task-1"),
            "implementation"
        );

        SkillContext context = new SkillContext(
            "work",
            "Work Skill",
            task,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of("task:task-1:completed", true)
        );

        SkillResult result = skill.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    void testReviewSkillExecution() throws SkillExecutionException {
        ReviewSkill skill = new ReviewSkill();
        Map<String, Object> reviewTarget = Map.of(
            "taskId", "task-1",
            "status", "completed"
        );

        SkillContext context = new SkillContext(
            "review",
            "Review Skill",
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of("reviewTarget", reviewTarget)
        );

        SkillResult result = skill.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.output());
    }

    @Test
    void testReviewSkillWithoutTarget() {
        ReviewSkill skill = new ReviewSkill();
        SkillContext context = SkillContext.createForTest("review", "Review Skill");

        assertThrows(SkillExecutionException.class, () -> skill.execute(context));
    }

    @Test
    void testSkillRegistry() {
        SkillRegistry registry = new SkillRegistry();

        PlanSkill planSkill = new PlanSkill();
        WorkSkill workSkill = new WorkSkill();
        ReviewSkill reviewSkill = new ReviewSkill();

        // Test registration
        registry.register(planSkill);
        registry.register(workSkill);
        registry.register(reviewSkill);

        assertEquals(3, registry.getSkillCount());
        assertTrue(registry.hasSkill("plan"));
        assertTrue(registry.hasSkill("work"));
        assertTrue(registry.hasSkill("review"));

        // Test retrieval
        assertEquals(planSkill, registry.getSkill("plan"));
        assertEquals(workSkill, registry.getSkill("work"));
        assertEquals(reviewSkill, registry.getSkill("review"));

        // Test getAllSkills
        var allSkills = registry.getAllSkills();
        assertEquals(3, allSkills.size());

        // Test unregister
        assertTrue(registry.unregister("plan"));
        assertFalse(registry.hasSkill("plan"));
        assertEquals(2, registry.getSkillCount());

        // Test duplicate registration (should throw since we still have work and review)
        assertThrows(IllegalArgumentException.class, () -> registry.register(new WorkSkill()));
    }

    @Test
    void testWorkSkillWithoutTask() {
        WorkSkill skill = new WorkSkill();
        SkillContext context = SkillContext.createForTest("work", "Work Skill");

        assertThrows(SkillExecutionException.class, () -> skill.execute(context));
    }

    @Test
    void testReviewResultRecords() {
        ReviewFinding finding = new ReviewFinding(
            ReviewSeverity.INFO,
            "Test finding",
            "Test description"
        );

        assertEquals(ReviewSeverity.INFO, finding.severity());
        assertEquals("Test finding", finding.title());
        assertEquals("Test description", finding.description());

        ReviewResult result = new ReviewResult(
            ReviewStatus.APPROVED,
            java.util.List.of(finding),
            "Test summary"
        );

        assertEquals(ReviewStatus.APPROVED, result.status());
        assertEquals(1, result.findings().size());
        assertEquals("Test summary", result.summary());
        assertTrue(result.isApproved());
    }
}
