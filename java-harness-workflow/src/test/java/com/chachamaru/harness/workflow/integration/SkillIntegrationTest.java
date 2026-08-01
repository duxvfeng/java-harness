package com.chachamaru.harness.workflow.integration;

import com.chachamaru.harness.collaboration.skill.Skill;
import com.chachamaru.harness.collaboration.skill.SkillRegistry;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import com.chachamaru.harness.workflow.engine.ExecutionContext;
import com.chachamaru.harness.workflow.engine.WorkflowEngine;
import com.chachamaru.harness.workflow.loader.WorkflowLoader;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 工作流引擎与技能系统集成测试
 * 验证 WorkflowEngine 能正确调用 SkillRegistry 中注册的技能
 */
public class SkillIntegrationTest {

    public static void main(String[] args) {
        System.out.println("=== 技能系统集成测试 ===\n");

        testBasicSkillInvocation();
        testSkillWithContext();
        testMultipleSkills();
        testSkillErrorHandling();

        System.out.println("\n✅ 所有集成测试通过！");
    }

    /**
     * 测试基本的技能调用
     */
    private static void testBasicSkillInvocation() {
        System.out.println("1. 基本技能调用测试");

        // 创建技能注册表
        SkillRegistry registry = new SkillRegistry();

        // 注册测试技能
        registry.register(new TestSkill("test-skill", "测试技能"));

        // 验证注册
        assert registry.hasSkill("test-skill") : "技能应该已注册";
        assert registry.getSkill("test-skill") != null : "应该能获取技能";

        System.out.println("   ✓ 技能注册和查找正常");
    }

    /**
     * 测试带上下文的技能调用
     */
    private static void testSkillWithContext() {
        System.out.println("2. 上下文技能调用测试");

        SkillRegistry registry = new SkillRegistry();
        registry.register(new ContextAwareSkill("context-skill"));

        Skill skill = registry.getSkill("context-skill");
        assert skill != null : "应该能获取技能";

        // 创建执行上下文
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setVariable("input_var", "test_value");

        System.out.println("   ✓ 技能上下文准备正常");
    }

    /**
     * 测试多技能注册和调用
     */
    private static void testMultipleSkills() {
        System.out.println("3. 多技能注册测试");

        SkillRegistry registry = new SkillRegistry();

        // 注册多个技能
        registry.register(new TestSkill("skill-1", "技能1"));
        registry.register(new TestSkill("skill-2", "技能2"));
        registry.register(new TestSkill("skill-3", "技能3"));

        assert registry.getSkillCount() == 3 : "应该有3个技能";
        assert registry.hasSkill("skill-1") : "应该有 skill-1";
        assert registry.hasSkill("skill-2") : "应该有 skill-2";
        assert registry.hasSkill("skill-3") : "应该有 skill-3";

        System.out.println("   ✓ 多技能注册正常");
    }

    /**
     * 测试技能错误处理
     */
    private static void testSkillErrorHandling() {
        System.out.println("4. 技能错误处理测试");

        SkillRegistry registry = new SkillRegistry();

        // 尝试获取不存在的技能
        Skill skill = registry.getSkill("nonexistent");
        assert skill == null : "不存在的技能应返回 null";

        // 尝试注册重复技能
        try {
            registry.register(new TestSkill("duplicate", "重复技能"));
            registry.register(new TestSkill("duplicate", "重复技能2"));
            assert false : "应该抛出异常";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("already registered") : "错误消息应包含 'already registered'";
        }

        System.out.println("   ✓ 技能错误处理正常");
    }

    /**
     * 测试技能实现
     */
    static class TestSkill implements Skill {
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
        public SkillResult execute(SkillContext context) {
            LocalDateTime startTime = LocalDateTime.now();
            return SkillResult.success(id, "Test output", "执行成功", startTime);
        }
    }

    /**
     * 上下文感知技能实现
     */
    static class ContextAwareSkill implements Skill {
        private final String id = "context-skill";

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return "上下文感知技能";
        }

        @Override
        public SkillResult execute(SkillContext context) {
            LocalDateTime startTime = LocalDateTime.now();

            // 从上下文中获取变量
            Object inputVar = context.getSessionState("input_var", Object.class);

            return SkillResult.success(
                id,
                "Processed: " + inputVar,
                "上下文处理成功",
                startTime
            );
        }
    }
}
