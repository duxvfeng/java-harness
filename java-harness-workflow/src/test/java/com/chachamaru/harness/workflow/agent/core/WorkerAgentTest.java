package com.chachamaru.harness.workflow.agent.core;

import com.chachamaru.harness.workflow.agent.framework.*;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WorkerAgent 测试")
public class WorkerAgentTest {

    private WorkerAgent agent;
    private SkillFramework mockSkillFramework;
    private AgentContext context;

    @BeforeEach
    public void setUp() {
        agent = new WorkerAgent();
        mockSkillFramework = mock(SkillFramework.class);
        context = AgentContext.builder()
                .taskId("task-001")
                .skillFramework(mockSkillFramework)
                .skillContext(SkillContext.builder()
                        .userIntent("实现用户认证功能")
                        .projectRoot(Paths.get("/project"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("应该返回正确的 Agent 基本信息")
    public void testAgentInfo() {
        assertEquals("worker", agent.getAgentId());
        assertEquals("Worker Agent", agent.getAgentName());
        assertEquals("1.0.0-java", agent.getVersion());
        assertEquals(AgentType.WORKER, agent.getAgentType());
    }

    @Test
    @DisplayName("应该执行工作策略")
    public void testExecuteWorkStrategy() throws AgentExecutionException {
        when(mockSkillFramework.executeSkill(eq("work"), any()))
                .thenReturn(SkillResult.builder()
                        .skillId("work")
                        .status(SkillResult.SkillStatus.SUCCESS)
                        .output("工作完成")
                        .build());

        AgentResult result = agent.execute(context);

        assertEquals("worker", result.getAgentId());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("应该在 Skill 执行失败时返回失败结果")
    public void testSkillExecutionFailure() throws AgentExecutionException {
        when(mockSkillFramework.executeSkill(eq("work"), any()))
                .thenThrow(new com.chachamaru.harness.workflow.skill.framework.SkillExecutionException("执行失败"));

        AgentResult result = agent.execute(context);

        assertEquals(AgentStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("应该初始化 Agent")
    public void testInitialize() {
        assertDoesNotThrow(() -> agent.initialize());
    }
}
