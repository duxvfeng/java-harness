package com.chachamaru.harness.workflow.model;

import java.util.List;
import java.util.Map;

/**
 * 工作流定义模型
 * 对应Go项目中的workflow YAML文件结构
 */
public class Workflow {
    private String phase;
    private String description;
    private List<WorkflowStep> steps;
    private WorkflowOutput onSuccess;
    private WorkflowOutput onError;

    public Workflow() {
        this.steps = List.of();
    }

    // Getters and Setters
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<WorkflowStep> getSteps() { return steps; }
    public void setSteps(List<WorkflowStep> steps) { this.steps = steps; }

    public WorkflowOutput getOnSuccess() { return onSuccess; }
    public void setOnSuccess(WorkflowOutput onSuccess) { this.onSuccess = onSuccess; }

    public WorkflowOutput getOnError() { return onError; }
    public void setOnError(WorkflowOutput onError) { this.onError = onError; }

    /**
     * 工作流步骤模型
     */
    public static class WorkflowStep {
        private String id;
        private String skill;
        private String condition;
        private StepInput input;
        private StepOutput output;
        private String mode; // required | optional
        private boolean parallel; // 是否并行执行

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getSkill() { return skill; }
        public void setSkill(String skill) { this.skill = skill; }

        public String getCondition() { return condition; }
        public void setCondition(String condition) { this.condition = condition; }

        public StepInput getInput() { return input; }
        public void setInput(StepInput input) { this.input = input; }

        public StepOutput getOutput() { return output; }
        public void setOutput(StepOutput output) { this.output = output; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
    }

    /**
     * 步骤输入模型
     */
    public static class StepInput {
        private List<String> files;
        private Map<String, Object> variables;
        private List<String> contextFrom;
        private List<String> templates;
        private String action;
        private String from;
        private String to;
        private String target;

        // Getters and Setters
        public List<String> getFiles() { return files; }
        public void setFiles(List<String> files) { this.files = files; }

        public Map<String, Object> getVariables() { return variables; }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }

        public List<String> getContextFrom() { return contextFrom; }
        public void setContextFrom(List<String> contextFrom) { this.contextFrom = contextFrom; }

        public List<String> getTemplates() { return templates; }
        public void setTemplates(List<String> templates) { this.templates = templates; }

        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }

        public String getFrom() { return from; }
        public void setFrom(String from) { this.from = from; }

        public String getTo() { return to; }
        public void setTo(String to) { this.to = to; }

        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
    }

    /**
     * 步骤输出模型
     */
    public static class StepOutput {
        private Map<String, Object> variables;
        private List<String> updateFiles;
        private List<String> createFiles;
        private boolean userMessage;

        // Getters and Setters
        public Map<String, Object> getVariables() { return variables; }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }

        public List<String> getUpdateFiles() { return updateFiles; }
        public void setUpdateFiles(List<String> updateFiles) { this.updateFiles = updateFiles; }

        public List<String> getCreateFiles() { return createFiles; }
        public void setCreateFiles(List<String> createFiles) { this.createFiles = createFiles; }

        public boolean isUserMessage() { return userMessage; }
        public void setUserMessage(boolean userMessage) { this.userMessage = userMessage; }
    }

    /**
     * 工作流输出模型
     */
    public static class WorkflowOutput {
        private String message;
        private Map<String, Object> variables;

        // Getters and Setters
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public Map<String, Object> getVariables() { return variables; }
        public void setVariables(Map<String, Object> variables) { this.variables = variables; }
    }
}
