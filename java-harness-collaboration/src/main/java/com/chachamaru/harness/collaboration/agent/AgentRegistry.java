package com.chachamaru.harness.collaboration.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing agents in the collaboration layer.
 *
 * <p>The AgentRegistry provides:
 * <ul>
 *   <li>Agent registration and unregistration</li>
 *   <li>Agent lookup by ID</li>
 *   <li>Agent discovery by type</li>
 *   <li>Thread-safe agent management</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class AgentRegistry {

    private static final Logger logger = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, Agent> agentsById;
    private final Map<Agent.AgentType, Set<Agent>> agentsByType;

    /**
     * Creates a new agent registry.
     */
    public AgentRegistry() {
        this.agentsById = new ConcurrentHashMap<>();
        this.agentsByType = new ConcurrentHashMap<>();
    }

    /**
     * Registers an agent.
     *
     * @param agent the agent to register
     * @throws IllegalArgumentException if agent ID is already registered
     */
    public void register(Agent agent) {
        Objects.requireNonNull(agent, "agent cannot be null");

        String id = agent.getId();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Agent ID cannot be null or blank");
        }

        if (agentsById.containsKey(id)) {
            throw new IllegalArgumentException("Agent already registered: " + id);
        }

        logger.info("Registering agent: {} ({})", agent.getName(), id);
        agentsById.put(id, agent);

        // Index by type
        Agent.AgentType type = agent.getType();
        if (type != null) {
            agentsByType.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(agent);
        }
    }

    /**
     * Unregisters an agent.
     *
     * @param agentId the ID of the agent to unregister
     * @return true if the agent was registered and removed, false otherwise
     */
    public boolean unregister(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            return false;
        }

        Agent agent = agentsById.remove(agentId);
        if (agent == null) {
            logger.warn("Attempted to unregister non-existent agent: {}", agentId);
            return false;
        }

        logger.info("Unregistering agent: {}", agentId);

        // Remove from type index
        Agent.AgentType type = agent.getType();
        if (type != null) {
            Set<Agent> typedAgents = agentsByType.get(type);
            if (typedAgents != null) {
                typedAgents.remove(agent);
                if (typedAgents.isEmpty()) {
                    agentsByType.remove(type);
                }
            }
        }

        return true;
    }

    /**
     * Gets an agent by ID.
     *
     * @param agentId the agent ID
     * @return the agent, or null if not found
     */
    public Agent getAgent(String agentId) {
        return agentsById.get(agentId);
    }

    /**
     * Checks if an agent is registered.
     *
     * @param agentId the agent ID
     * @return true if the agent is registered, false otherwise
     */
    public boolean hasAgent(String agentId) {
        return agentsById.containsKey(agentId);
    }

    /**
     * Gets all registered agents.
     *
     * @return unmodifiable collection of all agents
     */
    public Collection<Agent> getAllAgents() {
        return Collections.unmodifiableCollection(agentsById.values());
    }

    /**
     * Finds agents by type.
     *
     * @param type the agent type
     * @return collection of agents of the given type
     */
    public Collection<Agent> findByType(Agent.AgentType type) {
        if (type == null) {
            return Collections.emptyList();
        }

        Set<Agent> agents = agentsByType.get(type);
        return agents != null ? Collections.unmodifiableSet(agents) : Collections.emptyList();
    }

    /**
     * Finds agents by priority (highest first).
     *
     * @return list of agents sorted by priority (descending)
     */
    public List<Agent> findByPriority() {
        List<Agent> agents = new ArrayList<>(agentsById.values());
        agents.sort((a1, a2) -> Integer.compare(a2.getPriority(), a1.getPriority()));
        return Collections.unmodifiableList(agents);
    }

    /**
     * Gets the count of registered agents.
     *
     * @return the number of registered agents
     */
    public int getAgentCount() {
        return agentsById.size();
    }

    /**
     * Clears all registered agents.
     */
    public void clear() {
        logger.info("Clearing all agents from registry");
        agentsById.clear();
        agentsByType.clear();
    }

    /**
     * Gets all agent types in the registry.
     *
     * @return unmodifiable set of all agent types
     */
    public Set<Agent.AgentType> getAllTypes() {
        return Collections.unmodifiableSet(agentsByType.keySet());
    }
}
