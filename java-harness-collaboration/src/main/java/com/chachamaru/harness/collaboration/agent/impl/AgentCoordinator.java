package com.chachamaru.harness.collaboration.agent.impl;

import com.chachamaru.harness.collaboration.agent.Agent;
import com.chachamaru.harness.collaboration.agent.AgentRegistry;
import com.chachamaru.harness.collaboration.agent.AgentExecutionException;
import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinator for managing multi-agent workflows.
 *
 * <p>The AgentCoordinator is responsible for:
 * <ul>
 *   <li>Coordinating multiple agents</li>
 *   <li>Managing agent lifecycles</li>
 *   <li>Handling inter-agent communication</li>
 *   <li>Orchestrating complex workflows</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class AgentCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(AgentCoordinator.class);

    private final AgentRegistry registry;
    private final ExecutorService executorService;
    private final Map<String, AgentExecutionContext> activeExecutions;
    private boolean initialized = false;

    /**
     * Creates an AgentCoordinator.
     */
    public AgentCoordinator() {
        this.registry = new AgentRegistry();
        this.executorService = Executors.newCachedThreadPool();
        this.activeExecutions = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the coordinator.
     *
     * @throws AgentExecutionException if initialization fails
     */
    public void initialize() throws AgentExecutionException {
        if (initialized) {
            return;
        }

        logger.info("Initializing AgentCoordinator");

        // Register default agents
        registry.register(new WorkerAgent());
        registry.register(new ReviewerAgent());
        registry.register(new AdvisorAgent());

        // Initialize all agents
        for (Agent agent : registry.getAllAgents()) {
            try {
                agent.initialize();
            } catch (AgentExecutionException e) {
                logger.warn("Failed to initialize agent {}: {}", agent.getId(), e.getMessage());
            }
        }

        initialized = true;
        logger.info("AgentCoordinator initialized with {} agents", registry.getAgentCount());
    }

    /**
     * Shuts down the coordinator.
     *
     * @throws AgentExecutionException if shutdown fails
     */
    public void shutdown() throws AgentExecutionException {
        if (!initialized) {
            return;
        }

        logger.info("Shutting down AgentCoordinator");

        // Shutdown all agents
        for (Agent agent : registry.getAllAgents()) {
            try {
                agent.shutdown();
            } catch (AgentExecutionException e) {
                logger.warn("Failed to shutdown agent {}: {}", agent.getId(), e.getMessage());
            }
        }

        // Clear registry
        registry.clear();

        // Shutdown executor
        executorService.shutdown();

        initialized = false;
        logger.info("AgentCoordinator shut down");
    }

    /**
     * Executes an agent synchronously.
     *
     * @param agentId the agent ID
     * @param context the execution context
     * @return the execution result
     * @throws AgentExecutionException if execution fails
     */
    public AgentResult executeAgent(String agentId, AgentContext context) throws AgentExecutionException {
        Agent agent = registry.getAgent(agentId);
        if (agent == null) {
            throw new AgentExecutionException(agentId, "Agent not found: " + agentId);
        }

        return executeAgent(agent, context);
    }

    /**
     * Executes an agent synchronously.
     *
     * @param agent the agent to execute
     * @param context the execution context
     * @return the execution result
     * @throws AgentExecutionException if execution fails
     */
    public AgentResult executeAgent(Agent agent, AgentContext context) throws AgentExecutionException {
        if (!initialized) {
            throw new AgentExecutionException("coordinator", "Coordinator not initialized");
        }

        String executionId = UUID.randomUUID().toString();
        AgentExecutionContext execContext = new AgentExecutionContext(executionId, agent, context);

        activeExecutions.put(executionId, execContext);

        try {
            logger.info("Executing agent: {} ({})", agent.getName(), agent.getId());

            if (!agent.canExecute(context)) {
                throw new AgentExecutionException(agent.getId(), "Agent cannot execute in current context");
            }

            AgentResult result = agent.execute(context);

            logger.info("Agent {} completed with status: {}", agent.getId(), result.status());
            return result;

        } finally {
            activeExecutions.remove(executionId);
        }
    }

    /**
     * Executes an agent asynchronously.
     *
     * @param agentId the agent ID
     * @param context the execution context
     * @return future containing the execution result
     */
    public CompletableFuture<AgentResult> executeAgentAsync(String agentId, AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeAgent(agentId, context);
            } catch (AgentExecutionException e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    /**
     * Coordinates multiple agents in sequence.
     *
     * @param agentIds list of agent IDs to execute
     * @param context the execution context
     * @return list of results in execution order
     * @throws AgentExecutionException if coordination fails
     */
    public List<AgentResult> coordinateSequential(List<String> agentIds, AgentContext context)
            throws AgentExecutionException {
        if (!initialized) {
            throw new AgentExecutionException("coordinator", "Coordinator not initialized");
        }

        logger.info("Coordinating {} agents sequentially", agentIds.size());
        List<AgentResult> results = new ArrayList<>();

        for (String agentId : agentIds) {
            AgentResult result = executeAgent(agentId, context);
            results.add(result);

            // Stop if execution failed
            if (result.isFailed()) {
                logger.warn("Agent {} failed, stopping sequential execution", agentId);
                break;
            }
        }

        return results;
    }

    /**
     * Coordinates multiple agents in parallel.
     *
     * @param agentIds list of agent IDs to execute
     * @param context the execution context
     * @return list of results (order not guaranteed)
     * @throws AgentExecutionException if coordination fails
     */
    public List<AgentResult> coordinateParallel(List<String> agentIds, AgentContext context)
            throws AgentExecutionException {
        if (!initialized) {
            throw new AgentExecutionException("coordinator", "Coordinator not initialized");
        }

        logger.info("Coordinating {} agents in parallel", agentIds.size());

        List<CompletableFuture<AgentResult>> futures = new ArrayList<>();

        for (String agentId : agentIds) {
            futures.add(executeAgentAsync(agentId, context));
        }

        // Wait for all to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.join(); // Wait for completion

            // Collect results
            List<AgentResult> results = new ArrayList<>();
            for (CompletableFuture<AgentResult> future : futures) {
                results.add(future.get());
            }

            return results;

        } catch (Exception e) {
            throw new AgentExecutionException("coordinator", "Parallel coordination failed", e);
        }
    }

    /**
     * Gets the agent registry.
     *
     * @return the registry
     */
    public AgentRegistry getRegistry() {
        return registry;
    }

    /**
     * Checks if coordinator is initialized.
     *
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets active execution count.
     *
     * @return number of active executions
     */
    public int getActiveExecutionCount() {
        return activeExecutions.size();
    }

    /**
     * Agent execution context for tracking.
     */
    private record AgentExecutionContext(
        String executionId,
        Agent agent,
        AgentContext context
    ) {
    }
}
