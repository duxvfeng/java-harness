package com.chachamaru.harness.workflow.integration;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentRegistry;
import com.chachamaru.harness.collaboration.agent.impl.AgentCoordinator;
import com.chachamaru.harness.collaboration.agent.impl.AdvisorAgent;
import com.chachamaru.harness.collaboration.agent.impl.ReviewerAgent;
import com.chachamaru.harness.collaboration.agent.impl.WorkerAgent;
import com.chachamaru.harness.collaboration.skill.Skill;
import com.chachamaru.harness.collaboration.skill.SkillRegistry;
import com.chachamaru.harness.collaboration.skill.impl.PlanSkill;
import com.chachamaru.harness.collaboration.skill.impl.ReviewSkill;
import com.chachamaru.harness.collaboration.skill.impl.WorkSkill;
import com.chachamaru.harness.collaboration.skill.loader.MarkdownSkillLoader;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 代理协调集成测试
 * 验证代理注册和基本协调功能
 */
class AgentCoordinationTest {

    private AgentRegistry agentRegistry;
    private SkillRegistry skillRegistry;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        agentRegistry = new AgentRegistry();
        skillRegistry = new SkillRegistry();
    }

    @Test
    void testAgentRegistry() {
        // 创建代理
        WorkerAgent worker = new WorkerAgent();
        ReviewerAgent reviewer = new ReviewerAgent();
        AdvisorAgent advisor = new AdvisorAgent();

        // 注册代理
        agentRegistry.register(worker);
        agentRegistry.register(reviewer);
        agentRegistry.register(advisor);

        // 验证注册
        assertTrue(agentRegistry.hasAgent("worker"));
        assertTrue(agentRegistry.hasAgent("reviewer"));
        assertTrue(agentRegistry.hasAgent("advisor"));

        // 获取代理
        assertEquals("worker", agentRegistry.getAgent("worker").getId());
        assertEquals("reviewer", agentRegistry.getAgent("reviewer").getId());
        assertEquals("advisor", agentRegistry.getAgent("advisor").getId());
    }

    @Test
    void testSkillRegistry() {
        // 创建技能
        PlanSkill planSkill = new PlanSkill();
        WorkSkill workSkill = new WorkSkill();
        ReviewSkill reviewSkill = new ReviewSkill();

        // 注册技能
        skillRegistry.register(planSkill);
        skillRegistry.register(workSkill);
        skillRegistry.register(reviewSkill);

        // 验证注册
        assertTrue(skillRegistry.hasSkill("plan"));
        assertTrue(skillRegistry.hasSkill("work"));
        assertTrue(skillRegistry.hasSkill("review"));

        // 获取技能
        assertEquals("plan", skillRegistry.getSkill("plan").getName());
        assertEquals("work", skillRegistry.getSkill("work").getName());
        assertEquals("review", skillRegistry.getSkill("review").getName());
    }

    @Test
    void testSkillExecution() throws Exception {
        // 创建技能
        PlanSkill planSkill = new PlanSkill();
        skillRegistry.register(planSkill);

        // 创建技能上下文
        SkillContext context = new SkillContext(
            "test-session",
            tempDir.toString(),
            List.of("worker")
        );

        // 执行技能
        CompletableFuture<SkillResult> future = planSkill.executeAsync(context);
        SkillResult result = future.get();

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("plan", result.getSkillName());
    }

    @Test
    void testMarkdownSkillLoading() throws Exception {
        // 创建测试技能文件
        Path skillFile = tempDir.resolve(".SKILL.md");
        String skillContent = """
            ---
            name: test-skill
            description: Test skill for integration testing
            version: 1.0.0
            ---

            # Test Skill

            This is a test skill for integration testing purposes.

            ## Usage

            Use this skill to test the skill loading mechanism.
            """;

        java.nio.file.Files.writeString(skillFile, skillContent);

        // 加载Markdown技能
        MarkdownSkillLoader loader = new MarkdownSkillLoader();
        Skill skill = loader.load(skillFile.toString());

        assertNotNull(skill);
        assertEquals("test-skill", skill.getName());
        assertEquals("Test skill for integration testing", skill.getDescription());
    }

    @Test
    void testAgentContext() {
        // 创建代理上下文
        SkillContext skillContext = new SkillContext(
            "agent-test-session",
            tempDir.toString(),
            List.of("worker", "reviewer")
        );

        AgentContext agentContext = new AgentContext(
            "worker",
            skillContext,
            "Implement task 6.1.1: integration tests"
        );

        assertNotNull(agentContext);
        assertEquals("worker", agentContext.getAgentId());
        assertEquals("Implement task 6.1.1: integration tests", agentContext.getTask());
        assertEquals("agent-test-session", agentContext.getSkillContext().getSessionId());
    }

    @Test
    void testAgentResult() {
        // 创建成功结果
        AgentResult successResult = new AgentResult(
            "worker",
            true,
            "Task completed successfully",
            "Integration tests created",
            null
        );

        assertNotNull(successResult);
        assertEquals("worker", successResult.getAgentId());
        assertTrue(successResult.isSuccess());
        assertEquals("Integration tests created", successResult.getOutput());
        assertNull(successResult.getErrorMessage());

        // 创建失败结果
        AgentResult failureResult = new AgentResult(
            "worker",
            false,
            "Task failed",
            null,
            "Compilation error: missing dependency"
        );

        assertFalse(failureResult.isSuccess());
        assertNotNull(failureResult.getErrorMessage());
        assertEquals("Compilation error: missing dependency", failureResult.getErrorMessage());
    }

    @Test
    void testSkillResult() {
        // 创建技能结果
        SkillResult skillResult = new SkillResult(
            "plan",
            true,
            "Plan created successfully",
            List.of("Task 1", "Task 2", "Task 3"),
            null
        );

        assertNotNull(skillResult);
        assertEquals("plan", skillResult.getSkillName());
        assertTrue(skillResult.isSuccess());
        assertEquals(3, skillResult.getTasks().size());
        assertNull(skillResult.getError());
    }

    @Test
    void testAgentCoordinatorBasicFunctionality() {
        // 创建代理协调器
        AgentCoordinator coordinator = new AgentCoordinator(agentRegistry, skillRegistry);

        // 注册基本代理
        WorkerAgent worker = new WorkerAgent();
        ReviewerAgent reviewer = new ReviewerAgent();
        AdvisorAgent advisor = new AdvisorAgent();

        agentRegistry.register(worker);
        agentRegistry.register(reviewer);
        agentRegistry.register(advisor);

        // 验证协调器
        assertNotNull(coordinator);
        assertEquals(agentRegistry, coordinator.getAgentRegistry());
        assertEquals(skillRegistry, coordinator.getSkillRegistry());
    }

    @Test
    void testMultipleAgentTypes() {
        // 测试多种代理类型
        AgentRegistry registry = new AgentRegistry();

        // 添加不同类型的代理
        Agent worker = new WorkerAgent();
        Agent reviewer = new ReviewerAgent();
        Agent advisor = new AdvisorAgent();

        registry.register(worker);
        registry.register(reviewer);
        registry.register(advisor);

        // 验证所有代理都已注册
        assertEquals(3, registry.getAllAgents().size());
        assertTrue(registry.hasAgent("worker"));
        assertTrue(registry.hasAgent("reviewer"));
        assertTrue(registry.hasAgent("advisor"));
    }

    @Test
    void testSkillContextWithDifferentAgents() {
        // 测试不同代理的技能上下文
        SkillContext workerContext = new SkillContext(
            "worker-session",
            tempDir.toString(),
            List.of("worker")
        );

        SkillContext reviewerContext = new SkillContext(
            "reviewer-session",
            tempDir.toString(),
            List.of("reviewer", "worker")
        );

        assertEquals(List.of("worker"), workerContext.getAvailableAgents());
        assertEquals(List.of("reviewer", "worker"), reviewerContext.getAvailableAgents());
    }
}