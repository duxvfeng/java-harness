package com.chachamaru.harness.workflow.agent.core;

import com.chachamaru.harness.workflow.agent.framework.*;
import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import java.nio.file.Paths;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AdvisorAgent 测试")
public class AdvisorAgentTest {

    private AdvisorAgent agent;
    private SkillFramework mockSkillFramework;
    private AgentContext context;

    @BeforeEach
    public void setUp() {
        agent = new AdvisorAgent();
        mockSkillFramework = mock(SkillFramework.class);
        context = AgentContext.builder()
                .taskId("task-001")
                .skillFramework(mockSkillFramework)
                .skillContext(SkillContext.builder()
                        .userIntent("如何优化代码？")
                        .projectRoot(Paths.get("/project"))
                        .build())
                .build();
    }

    @Test
    @DisplayName("应该返回正确的 Agent 基本信息")
    public void testAgentInfo() {
        assertEquals("advisor", agent.getAgentId());
        assertEquals("Advisor Agent", agent.getAgentName());
        assertEquals(AgentType.ADVISOR, agent.getAgentType());
    }

    @Test
    @DisplayName("应该提供建议")
    public void testExecuteAdvise() throws AgentExecutionException {
        AgentResult result = agent.execute(context);

        assertEquals("advisor", result.getAgentId());
        assertNotNull(result.getOutput());
    }

    @Test
    @DisplayName("应该初始化 Agent")
    public void testInitialize() {
        assertDoesNotThrow(() -> agent.initialize());
    }
}
