package com.chachamaru.harness.workflow.skill.framework;

import java.nio.file.Paths;

/**
 * 测试用的简单技能实现
 */
public class TestSkill implements Skill {

    @Override
    public String getSkillId() {
        return "test-skill";
    }

    @Override
    public String getSkillName() {
        return "Test Skill";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "A simple skill for testing purposes";
    }

    @Override
    public Object execute(SkillContext context) throws SkillExecutionException {
        // 简单的测试实现
        return "Test executed successfully";
    }

    @Override
    public boolean validatePreconditions(SkillContext context) {
        // 验证有用户意图
        return context.getUserIntent() != null && !context.getUserIntent().isEmpty();
    }
}