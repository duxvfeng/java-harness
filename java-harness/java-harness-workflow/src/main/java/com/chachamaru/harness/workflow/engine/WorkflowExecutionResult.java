package com.chachamaru.harness.workflow.engine;

import com.chachamaru.harness.workflow.models.Workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工作流执行结果
 */
public class WorkflowExecutionResult {
    private String workflowName;
    private String phase;
    private boolean success;
    private String message;
    private long startTime;
    private long endTime;
    private long duration;
    private List<WorkflowStepExecution> stepExecutions;

    public WorkflowExecutionResult() {
        this.stepExecutions = new ArrayList<>();
        this.success = false;
    }

    // Getters and Setters
    public String getWorkflowName() { return workflowName; }
    public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public List<WorkflowStepExecution> getStepExecutions() { return stepExecutions; }
    public void setStepExecutions(List<WorkflowStepExecution> stepExecutions) { this.stepExecutions = stepExecutions; }

    /**
     * 获取成功的步骤数量
     */
    public int getSuccessfulStepCount() {
        return (int) stepExecutions.stream().filter(WorkflowStepExecution::isSuccess).count();
    }

    /**
     * 获取失败的步骤数量
     */
    public int getFailedStepCount() {
        return (int) stepExecutions.stream().filter(step -> !step.isSuccess()).count();
    }

    /**
     * 获取工作流摘要
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Workflow: ").append(workflowName).append("\n");
        summary.append("Phase: ").append(phase).append("\n");
        summary.append("Status: ").append(success ? "SUCCESS" : "FAILED").append("\n");
        summary.append("Duration: ").append(duration).append("ms\n");
        summary.append("Steps: ").append(getSuccessfulStepCount()).append("/").append(stepExecutions.size()).append(" successful\n");

        if (!success && !stepExecutions.isEmpty()) {
            summary.append("\nFailed steps:\n");
            stepExecutions.stream()
                .filter(step -> !step.isSuccess())
                .forEach(step -> summary.append("  - ")
                    .append(step.getStep().getId())
                    .append(": ")
                    .append(step.getErrorMessage())
                    .append("\n"));
        }

        return summary.toString();
    }
}

/**
 * 工作流步骤执行结果
 */
class WorkflowStepExecution {
    private Workflow.WorkflowStep step;
    private boolean success;
    private String errorMessage;
    private Map<String, Object> output;
    private long startTime;
    private long endTime;
    private long duration;

    public WorkflowStepExecution() {
        this.success = false;
    }

    // Getters and Setters
    public Workflow.WorkflowStep getStep() { return step; }
    public void setStep(Workflow.WorkflowStep step) { this.step = step; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
}
