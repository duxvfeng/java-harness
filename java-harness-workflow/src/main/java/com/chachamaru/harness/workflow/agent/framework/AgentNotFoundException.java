package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 未找到异常
 */
public class AgentNotFoundException extends AgentExecutionException {
    private final String agentId;

    public AgentNotFoundException(String agentId) {
        super("Agent not found: " + agentId);
        this.agentId = agentId;
    }

    public String getAgentId() {
        return agentId;
    }
}
