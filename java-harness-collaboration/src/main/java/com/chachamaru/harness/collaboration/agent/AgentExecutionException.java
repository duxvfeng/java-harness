package com.chachamaru.harness.collaboration.agent;

/**
 * Exception thrown when an agent fails to execute.
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class AgentExecutionException extends Exception {

    private final String agentId;

    /**
     * Creates a new agent execution exception.
     *
     * @param agentId the agent ID that failed
     * @param message the error message
     */
    public AgentExecutionException(String agentId, String message) {
        super(message);
        this.agentId = agentId;
    }

    /**
     * Creates a new agent execution exception with a cause.
     *
     * @param agentId the agent ID that failed
     * @param message the error message
     * @param cause the underlying cause
     */
    public AgentExecutionException(String agentId, String message, Throwable cause) {
        super(message, cause);
        this.agentId = agentId;
    }

    /**
     * Returns the agent ID that failed.
     *
     * @return the agent ID
     */
    public String getAgentId() {
        return agentId;
    }
}
