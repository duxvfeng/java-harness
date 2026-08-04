package com.chachamaru.harness.workflow.agent.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 注册表
 */
public class AgentRegistry {
    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();
    private final Map<String, AgentMetadata> metadata = new ConcurrentHashMap<>();

    public void register(Agent agent) {
        agents.put(agent.getAgentId(), agent);
        metadata.put(agent.getAgentId(), new AgentMetadata(agent));
        logger.debug("Registered agent: {}", agent.getAgentId());
    }

    public Agent getAgent(String agentId) {
        return agents.get(agentId);
    }

    public AgentMetadata getMetadata(String agentId) {
        return metadata.get(agentId);
    }

    public Map<String, AgentMetadata> getAllAgents() {
        return Collections.unmodifiableMap(metadata);
    }

    public boolean isRegistered(String agentId) {
        return agents.containsKey(agentId);
    }

    public int getAgentCount() {
        return agents.size();
    }

    public void unregister(String agentId) {
        agents.remove(agentId);
        metadata.remove(agentId);
        logger.debug("Unregistered agent: {}", agentId);
    }

    public void clear() {
        agents.clear();
        metadata.clear();
    }

    /**
     * Agent 元数据
     */
    public static class AgentMetadata {
        private final String agentId;
        private final String agentName;
        private final AgentType type;
        private final String version;
        private final String description;
        private final java.util.List<String> requiredSkills;

        public AgentMetadata(Agent agent) {
            this.agentId = agent.getAgentId();
            this.agentName = agent.getAgentName();
            this.type = agent.getAgentType();
            this.version = agent.getVersion();
            this.description = agent.getDescription();
            this.requiredSkills = agent.getRequiredSkills();
        }

        public String getAgentId() { return agentId; }
        public String getAgentName() { return agentName; }
        public AgentType getType() { return type; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public java.util.List<String> getRequiredSkills() { return requiredSkills; }
    }
}
