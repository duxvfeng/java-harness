package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 生命周期异常
 */
public class AgentLifecycleException extends AgentExecutionException {
    public AgentLifecycleException(String message) {
        super(message);
    }

    public AgentLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
