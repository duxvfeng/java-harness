package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("AgentRegistry 测试")
public class AgentRegistryTest {

    @Test
    @DisplayName("应该注册 Agent")
    public void testRegisterAgent() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");

        registry.register(mockAgent);

        assertTrue(registry.isRegistered("worker"));
        assertEquals(1, registry.getAgentCount());
    }

    @Test
    @DisplayName("应该获取 Agent 元数据")
    public void testGetAgentMetadata() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");
        when(mockAgent.getAgentName()).thenReturn("Worker Agent");
        when(mockAgent.getVersion()).thenReturn("1.0.0");
        when(mockAgent.getDescription()).thenReturn("执行工作");
        when(mockAgent.getAgentType()).thenReturn(AgentType.WORKER);

        registry.register(mockAgent);

        AgentRegistry.AgentMetadata metadata = registry.getMetadata("worker");
        assertNotNull(metadata);
        assertEquals("worker", metadata.getAgentId());
        assertEquals("Worker Agent", metadata.getAgentName());
    }

    @Test
    @DisplayName("应该获取所有 Agent 元数据")
    public void testGetAllAgents() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent1 = mock(Agent.class);
        Agent mockAgent2 = mock(Agent.class);
        when(mockAgent1.getAgentId()).thenReturn("worker");
        when(mockAgent2.getAgentId()).thenReturn("reviewer");

        registry.register(mockAgent1);
        registry.register(mockAgent2);

        assertEquals(2, registry.getAllAgents().size());
        assertTrue(registry.getAllAgents().containsKey("worker"));
        assertTrue(registry.getAllAgents().containsKey("reviewer"));
    }

    @Test
    @DisplayName("应该注销 Agent")
    public void testUnregisterAgent() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");

        registry.register(mockAgent);
        assertTrue(registry.isRegistered("worker"));

        registry.unregister("worker");
        assertFalse(registry.isRegistered("worker"));
    }

    @Test
    @DisplayName("应该清空所有 Agent")
    public void testClear() {
        AgentRegistry registry = new AgentRegistry();
        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getAgentId()).thenReturn("worker");

        registry.register(mockAgent);
        registry.clear();

        assertEquals(0, registry.getAgentCount());
    }
}
