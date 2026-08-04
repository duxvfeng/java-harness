package com.chachamaru.harness.workflow.skill.framework;

/**
 * 技能未找到异常
 */
public class SkillNotFoundException extends SkillExecutionException {

    public SkillNotFoundException(String skillId) {
        super(skillId, null, "Skill not found: " + skillId);
    }

    public SkillNotFoundException(String skillId, Throwable cause) {
        super(skillId, null, "Skill not found: " + skillId, cause);
    }
}