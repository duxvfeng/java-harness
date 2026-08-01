package com.chachamaru.harness.collaboration.skill;

import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.workflow.model.Task;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Skill framework.
 */
class SkillTest {

    @Test
    void testSkillContextCreation() {
        SkillContext context = new SkillContext(
            "test-skill",
            "Test Skill",
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of("key", "value"),
            Map.of("session", "data")
        );

        assertEquals("test-skill", context.skillId());
        assertEquals("Test Skill", context.skillName());
        assertEquals("value", context.getConfiguration("key", String.class));
        assertEquals("data", context.getSessionState("session", String.class));
    }

    @Test
    void testSkillContextValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SkillContext(
            "", "Test", null, HookInput.createForTest("test", "test"), Map.of(), Map.of()
        ));

        assertThrows(IllegalArgumentException.class, () -> new SkillContext(
            "test", null, null, HookInput.createForTest("test", "test"), Map.of(), Map.of()
        ));
    }

    @Test
    void testSkillContextCreateForTest() {
        SkillContext context = SkillContext.createForTest("skill-1", "Skill One");

        assertEquals("skill-1", context.skillId());
        assertEquals("Skill One", context.skillName());
        assertNotNull(context.hookInput());
        assertTrue(context.configuration().isEmpty());
        assertTrue(context.sessionState().isEmpty());
    }

    @Test
    void testSkillResultSuccess() {
        SkillResult result = SkillResult.success("skill-1", "output", "Success message", java.time.LocalDateTime.now());

        assertEquals("skill-1", result.skillId());
        assertEquals(SkillResult.SkillStatus.SUCCESS, result.status());
        assertEquals("output", result.output());
        assertEquals("Success message", result.message());
        assertTrue(result.isSuccess());
        assertFalse(result.isFailed());
    }

    @Test
    void testSkillResultFailure() {
        SkillResult result = SkillResult.failure("skill-1", "Error message", java.time.LocalDateTime.now());

        assertEquals("skill-1", result.skillId());
        assertEquals(SkillResult.SkillStatus.FAILED, result.status());
        assertNull(result.output());
        assertEquals("Error message", result.message());
        assertFalse(result.isSuccess());
        assertTrue(result.isFailed());
    }

    @Test
    void testSkillResultValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SkillResult(
            "", SkillResult.SkillStatus.SUCCESS, null, "msg", Map.of(), null, null, 0
        ));
    }

    @Test
    void testSimpleSkillExecution() throws SkillExecutionException {
        TestSkill skill = new TestSkill("test-skill", "Test Skill");
        SkillContext context = SkillContext.createForTest("test-skill", "Test Skill");

        SkillResult result = skill.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("test-output", result.output());
        assertEquals("Skill executed successfully", result.message());
    }

    @Test
    void testSkillInterfaceDefaultMethods() {
        Skill skill = new TestSkill("skill-1", "Skill One");

        assertEquals("skill-1", skill.getId());
        assertEquals("Skill One", skill.getName());
        assertEquals("Skill: Skill One", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion());
        assertEquals(0, skill.getPriority());
        assertTrue(skill.getTags().isEmpty());
        assertTrue(skill.canExecute(null));
    }

    @Test
    void testCoreSkillValidation() {
        TestSkill skill = new TestSkill("skill-1", "Skill One");
        SkillContext wrongContext = SkillContext.createForTest("wrong-skill", "Wrong Skill");

        assertThrows(SkillExecutionException.class, () -> skill.execute(wrongContext));
    }

    static class TestSkill extends CoreSkill {
        private final String id;
        private final String name;

        TestSkill(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        protected Object doExecute(SkillContext context) throws SkillExecutionException {
            return "test-output";
        }
    }
}
