package com.chachamaru.harness.collaboration.agent;

import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import com.chachamaru.harness.foundation.dto.HookInput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for agent framework.
 */
class AgentTest {

    @Test
    void testAgentContextCreation() {
        AgentContext context = new AgentContext(
            "test-agent",
            "Test Agent",
            AgentContext.AgentType.WORKER,
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of("key", "value"),
            Map.of("session", "data"),
            java.time.LocalDateTime.now()
        );

        assertEquals("test-agent", context.agentId());
        assertEquals("Test Agent", context.agentName());
        assertEquals(AgentContext.AgentType.WORKER, context.agentType());
        assertEquals("value", context.getConfiguration("key", String.class));
        assertEquals("data", context.getSessionState("session", String.class));
    }

    @Test
    void testAgentContextValidation() {
        assertThrows(IllegalArgumentException.class, () -> new AgentContext(
            "", "Test", AgentContext.AgentType.WORKER, null,
            HookInput.createForTest("test", "test"), Map.of(), Map.of(), java.time.LocalDateTime.now()
        ));

        assertThrows(IllegalArgumentException.class, () -> new AgentContext(
            "test", null, AgentContext.AgentType.WORKER, null,
            HookInput.createForTest("test", "test"), Map.of(), Map.of(), java.time.LocalDateTime.now()
        ));

        assertThrows(IllegalArgumentException.class, () -> new AgentContext(
            "test", "Test", null, null,
            HookInput.createForTest("test", "test"), Map.of(), Map.of(), java.time.LocalDateTime.now()
        ));
    }

    @Test
    void testAgentContextCreateForTest() {
        AgentContext context = AgentContext.createForTest("agent-1", "Agent One", AgentContext.AgentType.REVIEWER);

        assertEquals("agent-1", context.agentId());
        assertEquals("Agent One", context.agentName());
        assertEquals(AgentContext.AgentType.REVIEWER, context.agentType());
        assertNotNull(context.hookInput());
        assertTrue(context.configuration().isEmpty());
        assertTrue(context.sessionState().isEmpty());
        assertNotNull(context.executionStartTime());
    }

    @Test
    void testAgentResultSuccess() {
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
        AgentResult result = AgentResult.success("agent-1", "output", "Success message", startTime);

        assertEquals("agent-1", result.agentId());
        assertEquals(AgentResult.AgentStatus.SUCCESS, result.status());
        assertEquals("output", result.output());
        assertEquals("Success message", result.message());
        assertTrue(result.isSuccess());
        assertFalse(result.isFailed());
        assertFalse(result.isWaiting());
    }

    @Test
    void testAgentResultFailure() {
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
        AgentResult result = AgentResult.failure("agent-1", "Error message", startTime);

        assertEquals("agent-1", result.agentId());
        assertEquals(AgentResult.AgentStatus.FAILED, result.status());
        assertNull(result.output());
        assertEquals("Error message", result.message());
        assertFalse(result.isSuccess());
        assertTrue(result.isFailed());
        assertFalse(result.isWaiting());
    }

    @Test
    void testAgentResultWaiting() {
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
        AgentResult result = AgentResult.waiting("agent-1", "Waiting for input", startTime);

        assertEquals("agent-1", result.agentId());
        assertEquals(AgentResult.AgentStatus.WAITING, result.status());
        assertEquals("Waiting for input", result.message());
        assertFalse(result.isSuccess());
        assertFalse(result.isFailed());
        assertTrue(result.isWaiting());
    }

    @Test
    void testAgentResultValidation() {
        assertThrows(IllegalArgumentException.class, () -> new AgentResult(
            "", AgentResult.AgentStatus.SUCCESS, null, "msg", Map.of(), null, null, 0
        ));
    }

    @Test
    void testSimpleAgentExecution() throws AgentExecutionException {
        TestAgent agent = new TestAgent("test-agent", "Test Agent", Agent.AgentType.WORKER);
        AgentContext context = AgentContext.createForTest("test-agent", "Test Agent", AgentContext.AgentType.WORKER);

        AgentResult result = agent.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("agent-output", result.output());
        assertEquals("Agent executed successfully", result.message());
    }

    @Test
    void testAgentInterfaceDefaultMethods() {
        TestAgent agent = new TestAgent("agent-1", "Agent One", Agent.AgentType.WORKER);

        assertEquals("agent-1", agent.getId());
        assertEquals("Agent One", agent.getName());
        assertEquals("Test agent description", agent.getDescription());
        assertEquals(Agent.AgentType.WORKER, agent.getType());
        assertEquals("1.0.0", agent.getVersion());
        assertEquals(0, agent.getPriority());
        assertTrue(agent.canExecute(null));
    }

    @Test
    void testAgentRegistry() {
        AgentRegistry registry = new AgentRegistry();

        TestAgent agent1 = new TestAgent("agent-1", "Agent One", Agent.AgentType.WORKER);
        TestAgent agent2 = new TestAgent("agent-2", "Agent Two", Agent.AgentType.REVIEWER);
        TestAgent agent3 = new TestAgent("agent-3", "Agent Three", Agent.AgentType.WORKER);

        // Test registration
        registry.register(agent1);
        registry.register(agent2);
        registry.register(agent3);

        assertEquals(3, registry.getAgentCount());
        assertTrue(registry.hasAgent("agent-1"));
        assertTrue(registry.hasAgent("agent-2"));
        assertTrue(registry.hasAgent("agent-3"));

        // Test retrieval
        assertEquals(agent1, registry.getAgent("agent-1"));
        assertEquals(agent2, registry.getAgent("agent-2"));
        assertEquals(agent3, registry.getAgent("agent-3"));

        // Test getAllAgents
        var allAgents = registry.getAllAgents();
        assertEquals(3, allAgents.size());

        // Test findByType
        var workers = registry.findByType(Agent.AgentType.WORKER);
        assertEquals(2, workers.size());

        var reviewers = registry.findByType(Agent.AgentType.REVIEWER);
        assertEquals(1, reviewers.size());

        // Test unregister
        assertTrue(registry.unregister("agent-1"));
        assertFalse(registry.hasAgent("agent-1"));
        assertEquals(2, registry.getAgentCount());

        // Test duplicate registration
        assertThrows(IllegalArgumentException.class, () -> registry.register(new TestAgent("agent-2", "Duplicate", Agent.AgentType.WORKER)));
    }

    @Test
    void testAgentRegistryClear() {
        AgentRegistry registry = new AgentRegistry();

        registry.register(new TestAgent("agent-1", "Agent One", Agent.AgentType.WORKER));
        registry.register(new TestAgent("agent-2", "Agent Two", Agent.AgentType.REVIEWER));

        assertEquals(2, registry.getAgentCount());

        registry.clear();

        assertEquals(0, registry.getAgentCount());
        assertTrue(registry.getAllAgents().isEmpty());
    }

    @Test
    void testAgentExecutionException() {
        AgentExecutionException exception = new AgentExecutionException("test-agent", "Test failure");
        assertEquals("test-agent", exception.getAgentId());
        assertEquals("Test failure", exception.getMessage());
    }

    @Test
    void testAgentExecutionExceptionWithCause() {
        Throwable cause = new RuntimeException("Root cause");
        AgentExecutionException exception = new AgentExecutionException("test-agent", "Test failure", cause);

        assertEquals("test-agent", exception.getAgentId());
        assertEquals("Test failure", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }

    /**
     * Test agent implementation.
     */
    static class TestAgent implements Agent {
        private final String id;
        private final String name;
        private final Agent.AgentType type;

        TestAgent(String id, String name, Agent.AgentType type) {
            this.id = id;
            this.name = name;
            this.type = type;
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
        public String getDescription() {
            return "Test agent description";
        }

        @Override
        public Agent.AgentType getType() {
            return type;
        }

        @Override
        public AgentResult execute(AgentContext context) throws AgentExecutionException {
            return AgentResult.success(id, "agent-output", "Agent executed successfully", java.time.LocalDateTime.now());
        }
    }
}
