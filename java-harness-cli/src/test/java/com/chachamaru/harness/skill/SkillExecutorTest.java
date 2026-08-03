package com.chachamaru.harness.skill;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class SkillExecutorTest {
    @Test
    void testMapCommandToSkill() {
        assertEquals("harness-plan", SkillExecutor.mapCommandToSkill("plan"));
        assertEquals("harness-work", SkillExecutor.mapCommandToSkill("work"));
        assertEquals("harness-review", SkillExecutor.mapCommandToSkill("review"));
        assertEquals("harness-release", SkillExecutor.mapCommandToSkill("release"));
        assertEquals("harness-sync", SkillExecutor.mapCommandToSkill("sync"));
    }

    @Test
    void testUnmappedCommandReturnsNull() {
        assertNull(SkillExecutor.mapCommandToSkill("nonexistent"));
        assertNull(SkillExecutor.mapCommandToSkill("init"));
    }

    @Test
    void testShouldRouteToSkill() {
        assertTrue(SkillExecutor.shouldRouteToSkill("plan"));
        assertTrue(SkillExecutor.shouldRouteToSkill("work"));
        assertTrue(SkillExecutor.shouldRouteToSkill("review"));

        assertFalse(SkillExecutor.shouldRouteToSkill("init"));
        assertFalse(SkillExecutor.shouldRouteToSkill("doctor"));
        assertFalse(SkillExecutor.shouldRouteToSkill("help"));
    }

    @Test
    void testExecuteNonexistentSkill() {
        boolean result = SkillExecutor.executeSkill("nonexistent-skill", new String[]{});
        assertFalse(result);
    }

    @Test
    void testGetAvailableSkills() {
        List<String> skills = SkillExecutor.getAvailableSkills();
        assertNotNull(skills);
        // Should have at least some skills
        // Note: This test may fail if skills directory doesn't exist
    }
}
