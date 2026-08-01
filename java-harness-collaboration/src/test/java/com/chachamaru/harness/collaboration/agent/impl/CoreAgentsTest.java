package com.chachamaru.harness.collaboration.agent.impl;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import com.chachamaru.harness.foundation.dto.HookInput;
import com.chachamaru.harness.protocol.model.Task;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for core agent implementations.
 */
class CoreAgentsTest {

    @Test
    void testWorkerAgentCreation() {
        WorkerAgent agent = new WorkerAgent();

        assertEquals("worker-default", agent.getId());
        assertEquals("Default Worker Agent", agent.getName());
        assertEquals(Agent.AgentType.WORKER, agent.getType());
        assertEquals(3, agent.getMaxRetries());
        assertEquals(300000, agent.getTimeoutMs());
    }

    @Test
    void testWorkerAgentExecution() throws AgentExecutionException {
        WorkerAgent agent = new WorkerAgent("test-worker", "Test Worker");
        Task task = Task.createTodo("task-1", "Test Task", "Test description");
        AgentContext context = AgentContext.createForTest("test-worker", "Test Worker", AgentContext.AgentType.WORKER);

        // Create context with task
        AgentContext taskContext = new AgentContext(
            "test-worker",
            "Test Worker",
            AgentContext.AgentType.WORKER,
            task,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of(),
            java.time.LocalDateTime.now()
        );

        AgentResult result = agent.execute(taskContext);

        assertTrue(result.isSuccess());
        assertNotNull(result.output());
    }

    @Test
    void testWorkerAgentWithDependencies() throws AgentExecutionException {
        WorkerAgent agent = new WorkerAgent();
        Task task = new Task(
            "task-2",
            "Task with deps",
            "Description",
            com.chachamaru.harness.workflow.model.Status.CC_TODO,
            null,
            java.util.List.of("task-1"),
            "implementation"
        );

        AgentContext context = new AgentContext(
            "worker-default",
            "Default Worker Agent",
            AgentContext.AgentType.WORKER,
            task,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of("task:task-1:completed", true),
            java.time.LocalDateTime.now()
        );

        AgentResult result = agent.execute(context);

        assertTrue(result.isSuccess());
    }

    @Test
    void testWorkerAgentConfiguration() {
        WorkerAgent agent = new WorkerAgent();

        agent.setMaxRetries(5);
        assertEquals(5, agent.getMaxRetries());

        agent.setTimeoutMs(60000);
        assertEquals(60000, agent.getTimeoutMs());
    }

    @Test
    void testReviewerAgentCreation() {
        ReviewerAgent agent = new ReviewerAgent();

        assertEquals("reviewer-default", agent.getId());
        assertEquals("Default Reviewer Agent", agent.getName());
        assertEquals(Agent.AgentType.REVIEWER, agent.getType());
        assertFalse(agent.isCrossModel());
        assertEquals(0.2, agent.getTemperature());
    }

    @Test
    void testReviewerAgentExecution() throws AgentExecutionException {
        ReviewerAgent agent = new ReviewerAgent("test-reviewer", "Test Reviewer");
        Map<String, Object> reviewTarget = Map.of(
            "code", "public class Test { }",
            "changes", java.util.List.of("change1", "change2")
        );

        AgentContext context = new AgentContext(
            "test-reviewer",
            "Test Reviewer",
            AgentContext.AgentType.REVIEWER,
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of("reviewTarget", reviewTarget),
            java.time.LocalDateTime.now()
        );

        AgentResult result = agent.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.output());
    }

    @Test
    void testReviewerAgentConfiguration() {
        ReviewerAgent agent = new ReviewerAgent();

        agent.setCrossModel(true);
        assertTrue(agent.isCrossModel());

        agent.setTemperature(0.7);
        assertEquals(0.7, agent.getTemperature());
    }

    @Test
    void testAdvisorAgentCreation() {
        AdvisorAgent agent = new AdvisorAgent();

        assertEquals("advisor-default", agent.getId());
        assertEquals("Default Advisor Agent", agent.getName());
        assertEquals(Agent.AgentType.ADVISOR, agent.getType());
        assertTrue(agent.isEnabled());
    }

    @Test
    void testAdvisorAgentExecution() throws AgentExecutionException {
        AdvisorAgent agent = new AdvisorAgent("test-advisor", "Test Advisor");
        String question = "How should I implement this feature?";

        AgentContext context = new AgentContext(
            "test-advisor",
            "Test Advisor",
            AgentContext.AgentType.ADVISOR,
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of("question", question),
            java.time.LocalDateTime.now()
        );

        AgentResult result = agent.execute(context);

        assertTrue(result.isSuccess());
        assertNotNull(result.output());
    }

    @Test
    void testAdvisorAgentDisabled() throws AgentExecutionException {
        AdvisorAgent agent = new AdvisorAgent();
        agent.setEnabled(false);

        AgentContext context = new AgentContext(
            "advisor-default",
            "Default Advisor Agent",
            AgentContext.AgentType.ADVISOR,
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of("question", "Test question"),
            java.time.LocalDateTime.now()
        );

        AgentResult result = agent.execute(context);

        assertTrue(result.isSuccess());
        assertEquals("Advisor is disabled", result.message());
    }

    @Test
    void testAgentCoordinatorInitialization() throws AgentExecutionException {
        AgentCoordinator coordinator = new AgentCoordinator();

        assertFalse(coordinator.isInitialized());

        coordinator.initialize();

        assertTrue(coordinator.isInitialized());
        assertEquals(3, coordinator.getRegistry().getAgentCount());
    }

    @Test
    void testAgentCoordinatorSequentialExecution() throws AgentExecutionException {
        AgentCoordinator coordinator = new AgentCoordinator();
        coordinator.initialize();

        Task task = Task.createTodo("task-1", "Test Task", "Test description");
        AgentContext context = new AgentContext(
            "test",
            "Test",
            AgentContext.AgentType.WORKER,
            task,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of(),
            java.time.LocalDateTime.now()
        );

        List<String> agentIds = java.util.List.of("worker-default");

        List<AgentResult> results = coordinator.coordinateSequential(agentIds, context);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).isSuccess());
    }

    @Test
    void testAgentCoordinatorParallelExecution() throws AgentExecutionException {
        AgentCoordinator coordinator = new AgentCoordinator();
        coordinator.initialize();

        AgentContext context = AgentContext.createForTest(
            "test",
            "Test",
            AgentContext.AgentType.WORKER
        );

        // Note: Parallel execution with review target
        AgentContext reviewContext = new AgentContext(
            "reviewer-default",
            "Default Reviewer Agent",
            AgentContext.AgentType.REVIEWER,
            null,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of("reviewTarget", Map.of("code", "test code")),
            java.time.LocalDateTime.now()
        );

        List<String> agentIds = java.util.List.of("reviewer-default");

        List<AgentResult> results = coordinator.coordinateParallel(agentIds, reviewContext);

        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void testAgentCoordinatorShutdown() throws AgentExecutionException {
        AgentCoordinator coordinator = new AgentCoordinator();

        coordinator.initialize();
        assertTrue(coordinator.isInitialized());

        coordinator.shutdown();
        assertFalse(coordinator.isInitialized());
        assertEquals(0, coordinator.getRegistry().getAgentCount());
    }

    @Test
    void testAgentCoordinatorExecuteAgent() throws AgentExecutionException {
        AgentCoordinator coordinator = new AgentCoordinator();
        coordinator.initialize();

        // WorkerAgent needs a task in context
        Task task = Task.createTodo("task-1", "Test Task", "Test description");
        AgentContext context = new AgentContext(
            "worker-default",
            "Default Worker Agent",
            AgentContext.AgentType.WORKER,
            task,
            HookInput.createForTest("test-hook", "test-tool"),
            Map.of(),
            Map.of(),
            java.time.LocalDateTime.now()
        );

        AgentResult result = coordinator.executeAgent("worker-default", context);

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void testAgentLifecycle() throws AgentExecutionException {
        WorkerAgent agent = new WorkerAgent("lifecycle-test", "Lifecycle Test");

        agent.initialize();
        // Should complete without exception

        agent.shutdown();
        // Should complete without exception
    }

    @Test
    void testAllAgentTypes() {
        AgentCoordinator coordinator = new AgentCoordinator();

        assertEquals(0, coordinator.getRegistry().getAgentCount());

        // All three agent types should be registered after initialization
        try {
            coordinator.initialize();

            assertTrue(coordinator.getRegistry().hasAgent("worker-default"));
            assertTrue(coordinator.getRegistry().hasAgent("reviewer-default"));
            assertTrue(coordinator.getRegistry().hasAgent("advisor-default"));

            var workers = coordinator.getRegistry().findByType(Agent.AgentType.WORKER);
            var reviewers = coordinator.getRegistry().findByType(Agent.AgentType.REVIEWER);
            var advisors = coordinator.getRegistry().findByType(Agent.AgentType.ADVISOR);

            assertEquals(1, workers.size());
            assertEquals(1, reviewers.size());
            assertEquals(1, advisors.size());

        } catch (AgentExecutionException e) {
            fail("Initialization failed: " + e.getMessage());
        } finally {
            try {
                coordinator.shutdown();
            } catch (AgentExecutionException e) {
                // Ignore cleanup errors
            }
        }
    }
}
