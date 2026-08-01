package com.chachamaru.harness.collaboration.agent;

import com.chachamaru.harness.collaboration.agent.model.AgentContext;
import com.chachamaru.harness.collaboration.agent.model.AgentResult;

/**
 * Interface for agents in the collaboration layer.
 *
 * <p>Agents are autonomous entities that can perform tasks, make decisions,
 * and interact with other agents within the harness workflow.</p>
 *
 * <p>Unlike skills which are executable units, agents have:
 * <ul>
 *   <li>Persistent state and identity</li>
 *   <li>Decision-making capabilities</li>
 *   <li>Inter-agent communication</li>
 *   <li>Lifecycle management</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public interface Agent {

    /**
     * Executes the agent with the given context.
     *
     * <p>This method is called when the agent needs to perform its task.
     * Agents should maintain their state and make autonomous decisions.</p>
     *
     * @param context the agent execution context
     * @return the result of agent execution
     * @throws AgentExecutionException if the agent fails to execute
     */
    AgentResult execute(AgentContext context) throws AgentExecutionException;

    /**
     * Returns the unique identifier for this agent.
     *
     * @return the agent identifier
     */
    String getId();

    /**
     * Returns the display name for this agent.
     *
     * @return the agent name
     */
    String getName();

    /**
     * Returns the description of what this agent does.
     *
     * @return the agent description
     */
    String getDescription();

    /**
     * Returns the type of this agent.
     *
     * @return the agent type
     */
    AgentType getType();

    /**
     * Returns the version of this agent.
     *
     * @return the agent version
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Checks if this agent can handle the given context.
     *
     * @param context the agent context to check
     * @return true if this agent can handle the context, false otherwise
     */
    default boolean canExecute(AgentContext context) {
        return true;
    }

    /**
     * Returns the priority of this agent.
     *
     * <p>Higher priority agents are selected first. Default priority is 0.</p>
     *
     * @return the priority value (higher values = higher priority)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Initializes the agent.
     *
     * <p>This method is called once before the agent starts processing.
     * Subclasses can override to perform initialization logic.</p>
     *
     * @throws AgentExecutionException if initialization fails
     */
    default void initialize() throws AgentExecutionException {
        // Default: no-op
    }

    /**
     * Shuts down the agent.
     *
     * <p>This method is called when the agent is no longer needed.
     * Subclasses can override to perform cleanup.</p>
     *
     * @throws AgentExecutionException if shutdown fails
     */
    default void shutdown() throws AgentExecutionException {
        // Default: no-op
    }

    /**
     * Agent type enumeration.
     */
    enum AgentType {
        /** Worker agent - executes tasks */
        WORKER,
        /** Reviewer agent - reviews code and changes */
        REVIEWER,
        /** Advisor agent - provides guidance */
        ADVISOR,
        /** Coordinator agent - coordinates other agents */
        COORDINATOR
    }
}
