package com.chachamaru.harness.workflow.agent.core;

import com.chachamaru.harness.workflow.agent.framework.*;
import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ReviewerAgent 测试")
public class ReviewerAgentTest {

    private ReviewerAgent agent;
    private SkillFramework mockSkillFramework;
    private AgentContext context;

    @BeforeEach
    public void setUp() {
        agent = new ReviewerAgent();
        mockSkillFramework = mock(SkillFramework.class);
        context = AgentContext.builder()
                .taskId("task-001")
                .skillFramework(mockSkillFramework)
                .skillContext(SkillContext.builder()
                        .userIntent("审查代码")
                        .projectRoot(Paths.get("/project"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("应该返回正确的 Agent 基本信息")
    public void testAgentInfo() {
        assertEquals("reviewer", agent.getAgentId());
        assertEquals("Reviewer Agent", agent.getAgentName());
        assertEquals(AgentType.REVIEWER, agent.getAgentType());
    }

    @Test
    @DisplayName("应该执行审查")
    public void testExecuteReview() throws AgentExecutionException {
        Map<String, Object> sharedState = new HashMap<>();
        sharedState.put("workResult", "工作成果");

        context = AgentContext.builder()
                .taskId("task-001")
                .skillFramework(mockSkillFramework)
                .skillContext(SkillContext.builder()
                        .userIntent("审查代码")
                        .projectRoot(Paths.get("/project"))
                        .build())
                .sharedState(sharedState)
                .build();

        AgentResult result = agent.execute(context);

        assertEquals("reviewer", result.getAgentId());
        assertTrue(result.isSuccess() || result.isPartialSuccess());
    }

    @Test
    @DisplayName("应该初始化 Agent")
    public void testInitialize() {
        assertDoesNotThrow(() -> agent.initialize());
    }
}
