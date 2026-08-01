package com.chachamaru.harness.workflow.engine;

import com.chachamaru.harness.workflow.model.Workflow;

import java.util.Map;
import java.util.HashMap;

/**
 * 工作流步骤执行结果
 */
public class WorkflowStepExecution {
    private Workflow.WorkflowStep step;
    private boolean success;
    private String errorMessage;
    private Map<String, Object> output;
    private long startTime;
    private long endTime;
    private long duration;

    public WorkflowStepExecution() {
        this.success = false;
        this.output = new HashMap<>();
    }

    public Workflow.WorkflowStep getStep() {
        return step;
    }

    public void setStep(Workflow.WorkflowStep step) {
        this.step = step;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output != null ? output : new HashMap<>();
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
}
