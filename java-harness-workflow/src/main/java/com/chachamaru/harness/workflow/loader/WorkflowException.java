package com.chachamaru.harness.workflow.loader;

/**
 * 工作流异常
 */
public class WorkflowException extends Exception {
    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }
}
