package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AgentExecutor 测试")
public class AgentExecutorTest {

    @Test
    @DisplayName("应该执行 Agent")
    public void testExecuteAgent() throws AgentExecutionException {
        AgentExecutor executor = new AgentExecutor();
        Agent mockAgent = mock(Agent.class);
        AgentContext mockContext = mock(AgentContext.class);
        AgentResult mockResult = AgentResult.builder()
                .agentId("worker")
                .status(AgentStatus.SUCCESS)
                .output("任务完成")
                .build();

        when(mockAgent.execute(mockContext)).thenReturn(mockResult);
        when(mockAgent.validatePreconditions(mockContext)).thenReturn(true);

        AgentResult result = executor.execute(mockAgent, mockContext);

        assertNotNull(result);
        assertEquals("worker", result.getAgentId());
        assertEquals(AgentStatus.SUCCESS, result.getStatus());
        verify(mockAgent).execute(mockContext);
    }

    @Test
    @DisplayName("应该在前置条件失败时返回失败结果")
    public void testPreconditionFailure() throws AgentExecutionException {
        AgentExecutor executor = new AgentExecutor();
        Agent mockAgent = mock(Agent.class);
        AgentContext mockContext = mock(AgentContext.class);

        when(mockAgent.validatePreconditions(mockContext)).thenReturn(false);

        AgentResult result = executor.execute(mockAgent, mockContext);

        assertEquals(AgentStatus.FAILED, result.getStatus());
        assertNotNull(result.getErrorMessage());
        verify(mockAgent, never()).execute(mockContext);
    }

    @Test
    @DisplayName("应该在执行异常时返回失败结果")
    public void testExecutionException() throws AgentExecutionException {
        AgentExecutor executor = new AgentExecutor();
        Agent mockAgent = mock(Agent.class);
        AgentContext mockContext = mock(AgentContext.class);

        when(mockAgent.validatePreconditions(mockContext)).thenReturn(true);
        when(mockAgent.execute(mockContext)).thenThrow(new AgentExecutionException("执行失败"));

        AgentResult result = executor.execute(mockAgent, mockContext);

        assertEquals(AgentStatus.FAILED, result.getStatus());
        assertEquals("执行失败", result.getErrorMessage());
    }

    @Test
    @DisplayName("应该获取活跃执行计数")
    public void testGetActiveExecutionCount() {
        AgentExecutor executor = new AgentExecutor();
        assertEquals(0, executor.getActiveExecutionCount());
    }
}
