package com.chachamaru.harness.workflow.agent.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.Optional;

/**
 * Agent 框架核心
 */
public class AgentFramework implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(AgentFramework.class);

    private final AgentRegistry registry;
    private final AgentExecutor executor;

    public AgentFramework() {
        this.registry = new AgentRegistry();
        this.executor = new AgentExecutor();
        initializeCoreAgents();
    }

    private void initializeCoreAgents() {
        // 阶段1：暂时使用占位符，后续会替换为真实的 Agent 实现
        logger.info("Initializing core agents");

        // 核心Agent将在后续任务中实现：
        // - WorkerAgent
        // - ReviewerAgent
        // - AdvisorAgent

        logger.info("AgentFramework initialized (ready for agent registration)");
    }

    public void registerAgent(Agent agent) {
        registry.register(agent);
        logger.info("Registered agent: {}", agent.getAgentId());
    }

    public AgentResult executeAgent(String agentId, AgentContext context) throws AgentExecutionException {
        Agent agent = registry.getAgent(agentId);
        if (agent == null) {
            throw new AgentNotFoundException(agentId);
        }

        return executor.execute(agent, context);
    }

    public Optional<Agent> findAgent(String agentId) {
        Agent agent = registry.getAgent(agentId);
        return Optional.ofNullable(agent);
    }

    public Map<String, AgentRegistry.AgentMetadata> getRegisteredAgents() {
        return registry.getAllAgents();
    }

    public int getAgentCount() {
        return registry.getAgentCount();
    }

    @Override
    public void close() {
        logger.info("Shutting down AgentFramework");
        registry.clear();
    }
}
