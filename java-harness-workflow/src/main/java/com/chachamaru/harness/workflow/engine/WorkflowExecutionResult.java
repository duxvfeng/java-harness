package com.chachamaru.harness.workflow.engine;

import java.util.Map;

/**
 * 工作流执行结果
 * 包含工作流执行的完整信息和结果
 */
public class WorkflowExecutionResult {
    private String workflowName;
    private String phase;
    private boolean success;
    private String message;
    private long startTime;
    private long endTime;
    private long duration;
    private Map<String, Object> outputVariables;

    public WorkflowExecutionResult() {
        this.success = false;
        this.startTime = System.currentTimeMillis();
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Map<String, Object> getOutputVariables() {
        return outputVariables;
    }

    public void setOutputVariables(Map<String, Object> outputVariables) {
        this.outputVariables = outputVariables;
    }
}
