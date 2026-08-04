package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 执行异常基类
 */
public class AgentExecutionException extends Exception {
    public AgentExecutionException(String message) {
        super(message);
    }

    public AgentExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
