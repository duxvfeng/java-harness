package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 验证异常
 */
public class AgentValidationException extends AgentExecutionException {
    public AgentValidationException(String message) {
        super(message);
    }

    public AgentValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
