package com.chachamaru.harness.workflow.integration;

import com.chachamaru.harness.workflow.agent.core.WorkerAgent;
import com.chachamaru.harness.workflow.agent.framework.AgentContext;
import com.chachamaru.harness.workflow.agent.framework.AgentFramework;
import com.chachamaru.harness.workflow.agent.framework.AgentResult;
import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 系统 + Skill 系统集成测试
 * 验证 Agent 能够正确调用 Skill
 */
@DisplayName("Agent + Skill 集成测试")
public class AgentSkillIntegrationTest {

    @Test
    @DisplayName("端到端测试：WorkerAgent 调用 WorkSkill")
    public void testWorkerAgentCallsWorkSkill() {
        // 1. 初始化 AgentFramework
        AgentFramework agentFramework = new AgentFramework();

        // 2. 创建 AgentContext
        SkillContext skillContext = SkillContext.builder()
                .userIntent("实现用户认证功能")
                .projectRoot(Paths.get("/project"))
                .build();

        AgentContext agentContext = AgentContext.builder()
                .taskId("task-001")
                .skillFramework(agentFramework.getSkillFramework())
                .skillContext(skillContext)
                .build();

        // 3. 执行 WorkerAgent
        try {
            AgentResult result = agentFramework.executeAgent("worker", agentContext);

            // 4. 验证结果
            assertNotNull(result, "AgentResult should not be null");
            assertEquals("worker", result.getAgentId());
            assertTrue(result.isSuccess() || result.isPartialSuccess(),
                    "WorkerAgent should succeed or partially succeed");

            // 5. 验证 Skill 调用追踪
            assertNotNull(result.getSkillCalls(), "Should have skill call traces");
            assertTrue(result.getSkillCalls().size() >= 2,
                    "Should have at least 2 skill calls (plan + work)");

            System.out.println("✅ Integration test passed!");
            System.out.println("   Agent: " + result.getAgentId());
            System.out.println("   Status: " + result.getStatus());
            System.out.println("   Skill calls: " + result.getSkillCalls().size());

        } catch (Exception e) {
            fail("Integration test failed: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("端到端测试：完整工作流")
    public void testCompleteWorkflow() {
        AgentFramework agentFramework = new AgentFramework();

        SkillContext skillContext = SkillContext.builder()
                .userIntent("开发用户登录功能")
                .projectRoot(Paths.get("/project"))
                .build();

        AgentContext agentContext = AgentContext.builder()
                .taskId("task-002")
                .skillFramework(agentFramework.getSkillFramework())
                .skillContext(skillContext)
                .build();

        try {
            // 执行 WorkerAgent（会调用 PlanSkill 和 WorkSkill）
            AgentResult workerResult = agentFramework.executeAgent("worker", agentContext);

            assertTrue(workerResult.isSuccess(), "WorkerAgent should succeed");
            assertTrue(workerResult.getSkillCalls().size() >= 2,
                    "Should call plan and work skills");

            // 验证 Skill 调用顺序和状态
            workerResult.getSkillCalls().forEach(trace -> {
                System.out.println("   Skill call: " + trace.getSkillId() +
                        " | Success: " + trace.isSuccessful() +
                        " | Decision: " + trace.getCallerDecision());
                assertTrue(trace.isSuccessful(), "Each skill call should succeed");
            });

            System.out.println("✅ Complete workflow test passed!");

        } catch (Exception e) {
            fail("Workflow test failed: " + e.getMessage());
        }
    }
}
