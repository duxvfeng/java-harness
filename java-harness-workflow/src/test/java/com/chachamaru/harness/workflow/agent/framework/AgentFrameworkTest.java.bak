package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AgentFramework 测试")
public class AgentFrameworkTest {

    @Test
    @DisplayName("应该初始化框架")
    public void testInitialize() {
        AgentFramework framework = new AgentFramework();

        assertEquals(0, framework.getRegisteredAgents().size());
    }

    @Test
    @DisplayName("应该注册自定义 Agent")
    public void testRegisterAgent() {
        AgentFramework framework = new AgentFramework();
        Agent customAgent = mock(Agent.class);
        when(customAgent.getAgentId()).thenReturn("custom");

        framework.registerAgent(customAgent);

        assertTrue(framework.findAgent("custom").isPresent());
        assertEquals(4, framework.getRegisteredAgents().size());
    }

    @Test
    @DisplayName("应该执行 Agent")
    public void testExecuteAgent() throws AgentExecutionException {
        AgentFramework framework = new AgentFramework();
        AgentContext mockContext = mock(AgentContext.class);
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");
        when(mockAgent.validatePreconditions(mockContext)).thenReturn(true);
        when(mockAgent.execute(mockContext)).thenReturn(AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .build());

        framework.registerAgent(mockAgent);

        AgentResult result = framework.executeAgent("worker", mockContext);

        assertNotNull(result);
        assertEquals("worker", result.getAgentId());
    }

    @Test
    @DisplayName("应该在 Agent 不存在时抛出异常")
    public void testExecuteNonExistentAgent() {
        AgentFramework framework = new AgentFramework();
        AgentContext mockContext = mock(AgentContext.class);

        assertThrows(AgentNotFoundException.class, () -> {
            framework.executeAgent("nonexistent", mockContext);
        });
    }

    @Test
    @DisplayName("应该查找 Agent")
    public void testFindAgent() {
        AgentFramework framework = new AgentFramework();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");
        framework.registerAgent(mockAgent);

        assertTrue(framework.findAgent("worker").isPresent());
        assertFalse(framework.findAgent("nonexistent").isPresent());
    }

    @Test
    @DisplayName("应该关闭框架")
    public void testClose() throws Exception {
        AgentFramework framework = new AgentFramework();
        framework.registerAgent(mock(Agent.class));

        framework.close();

        assertEquals(0, framework.getRegisteredAgents().size());
    }
}
