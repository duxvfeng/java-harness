package com.chachamaru.harness.workflow.loader;

import com.chachamaru.harness.workflow.model.Workflow;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流加载器
 * 负责从YAML文件加载工作流定义
 * 支持变量替换和条件表达式求值
 *
 * 注意：采用宽松Map解析而非严格JavaBean映射，以兼容Go版本YAML中的
 * 下划线命名、额外字段、字符串/列表混用等写法。
 */
public class WorkflowLoader {
    private final Yaml yaml;
    private final Map<String, Workflow> workflowCache;

    public WorkflowLoader() {
        this.yaml = new Yaml();
        this.workflowCache = new HashMap<>();
    }

    /**
     * 从文件加载工作流
     */
    public Workflow loadWorkflow(File workflowFile) throws WorkflowException {
        return loadWorkflow(workflowFile, new HashMap<>());
    }

    /**
     * 从文件加载工作流（带变量上下文）
     */
    public Workflow loadWorkflow(File workflowFile, Map<String, Object> context) throws WorkflowException {
        try {
            String content = Files.readString(workflowFile.toPath());

            // 应用变量替换
            String processedContent = VariableResolver.resolve(content, context);

            // 先解析为宽松Map，再手动转换为Workflow（兼容Go风格YAML）
            Map<String, Object> data = yaml.load(processedContent);
            Workflow workflow = convertToWorkflow(data);

            // 验证工作流定义
            validateWorkflow(workflow);

            return workflow;
        } catch (Exception e) {
            throw new WorkflowException("Failed to load workflow: " + workflowFile, e);
        }
    }

    /**
     * 从目录加载所有工作流
     */
    public Map<String, Workflow> loadWorkflowsFromDirectory(File workflowDir) throws WorkflowException {
        return loadWorkflowsFromDirectory(workflowDir, new HashMap<>());
    }

    /**
     * 从目录加载所有工作流（带变量上下文）
     */
    public Map<String, Workflow> loadWorkflowsFromDirectory(File workflowDir, Map<String, Object> context) throws WorkflowException {
        Map<String, Workflow> workflows = new HashMap<>();

        if (!workflowDir.exists() || !workflowDir.isDirectory()) {
            return workflows;
        }

        File[] workflowFiles = workflowDir.listFiles((dir, name) ->
            name.endsWith(".yaml") || name.endsWith(".yml"));

        if (workflowFiles != null) {
            for (File file : workflowFiles) {
                try {
                    Workflow workflow = loadWorkflow(file, context);
                    String workflowName = file.getName().replace(".yaml", "").replace(".yml", "");
                    workflows.put(workflowName, workflow);
                } catch (Exception e) {
                    // 记录错误但继续加载其他工作流
                    System.err.println("Failed to load workflow " + file + ": " + e.getMessage());
                }
            }
        }

        return workflows;
    }

    /**
     * 加载默认工作流
     */
    public Workflow loadDefaultWorkflow(String workflowName) throws WorkflowException {
        return loadDefaultWorkflow(workflowName, new HashMap<>());
    }

    /**
     * 加载默认工作流（带变量上下文）
     */
    public Workflow loadDefaultWorkflow(String workflowName, Map<String, Object> context) throws WorkflowException {
        String cacheKey = "default:" + workflowName;

        if (workflowCache.containsKey(cacheKey)) {
            return workflowCache.get(cacheKey);
        }

        File defaultWorkflowFile = new File("workflows/default", workflowName + ".yaml");
        if (!defaultWorkflowFile.exists()) {
            throw new WorkflowException("Default workflow not found: " + workflowName);
        }

        Workflow workflow = loadWorkflow(defaultWorkflowFile, context);
        workflowCache.put(cacheKey, workflow);
        return workflow;
    }

    /**
     * 获取所有可用的默认工作流
     */
    public Map<String, Workflow> loadAllDefaultWorkflows() throws WorkflowException {
        return loadAllDefaultWorkflows(new HashMap<>());
    }

    /**
     * 获取所有可用的默认工作流（带变量上下文）
     */
    public Map<String, Workflow> loadAllDefaultWorkflows(Map<String, Object> context) throws WorkflowException {
        Map<String, Workflow> workflows = new HashMap<>();
        File defaultDir = new File("workflows/default");

        if (!defaultDir.exists()) {
            return workflows;
        }

        return loadWorkflowsFromDirectory(defaultDir, context);
    }

    /**
     * 评估步骤的条件表达式
     */
    public boolean evaluateCondition(String condition, Map<String, Object> context) throws WorkflowException {
        if (condition == null || condition.isBlank()) {
            return true; // 无条件时默认执行
        }

        try {
            return ExpressionEvaluator.evaluate(condition, context);
        } catch (ExpressionEvaluator.ExpressionException e) {
            throw new WorkflowException("Failed to evaluate condition: " + condition, e);
        }
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        workflowCache.clear();
    }

    // -------------------------------------------------------------------------
    // 宽松 YAML -> Workflow 转换（兼容Go版本的下划线命名和额外字段）
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Workflow convertToWorkflow(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        Workflow workflow = new Workflow();
        workflow.setPhase(getString(data, "phase"));
        workflow.setDescription(getString(data, "description"));
        workflow.setSteps(convertSteps(getList(data, "steps")));
        workflow.setOnSuccess(convertWorkflowOutput(getMap(data, "on_success", "onSuccess")));
        workflow.setOnError(convertWorkflowOutput(getMap(data, "on_error", "onError")));
        return workflow;
    }

    @SuppressWarnings("unchecked")
    private List<Workflow.WorkflowStep> convertSteps(List<Object> stepsData) {
        List<Workflow.WorkflowStep> steps = new ArrayList<>();
        if (stepsData == null) {
            return steps;
        }

        for (Object stepObj : stepsData) {
            if (stepObj instanceof Map) {
                steps.add(convertStep((Map<String, Object>) stepObj));
            }
        }
        return steps;
    }

    @SuppressWarnings("unchecked")
    private Workflow.WorkflowStep convertStep(Map<String, Object> data) {
        Workflow.WorkflowStep step = new Workflow.WorkflowStep();
        step.setId(getString(data, "id"));
        step.setSkill(getString(data, "skill"));
        step.setCondition(getString(data, "condition"));
        step.setInput(convertStepInput(getMap(data, "input")));
        step.setOutput(convertStepOutput(getMap(data, "output")));
        step.setMode(getString(data, "mode"));
        step.setParallel(getBoolean(data, "parallel"));
        step.setLoop(getString(data, "loop"));
        return step;
    }

    @SuppressWarnings("unchecked")
    private Workflow.StepInput convertStepInput(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        Workflow.StepInput input = new Workflow.StepInput();
        input.setFiles(getObject(data, "files"));
        input.setVariables(getStringList(data, "variables"));
        input.setContextFrom(getStringList(data, "context_from", "contextFrom"));
        input.setTemplates(getStringList(data, "templates"));
        input.setAction(getString(data, "action"));
        input.setFrom(getString(data, "from"));
        input.setTo(getString(data, "to"));
        input.setTarget(getString(data, "target"));
        return input;
    }

    @SuppressWarnings("unchecked")
    private Workflow.StepOutput convertStepOutput(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        Workflow.StepOutput output = new Workflow.StepOutput();
        output.setVariables(getStringList(data, "variables"));
        output.setUpdateFiles(getStringList(data, "update_files", "updateFiles"));
        output.setCreateFiles(getStringList(data, "create_files", "createFiles"));
        output.setCreatedFiles(getBoolean(data, "created_files", "createdFiles"));
        output.setUserMessage(getBoolean(data, "user_message", "userMessage"));
        return output;
    }

    @SuppressWarnings("unchecked")
    private Workflow.WorkflowOutput convertWorkflowOutput(Map<String, Object> data) {
        if (data == null) {
            return null;
        }

        Workflow.WorkflowOutput output = new Workflow.WorkflowOutput();
        output.setMessage(getString(data, "message"));
        output.setVariables(getMap(data, "variables"));
        return output;
    }

    // -------------------------------------------------------------------------
    // 辅助方法：安全读取宽松Map（同时支持下划线与驼峰key）
    // -------------------------------------------------------------------------

    private String getString(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof String) {
                return (String) value;
            }
        }
        return null;
    }

    private boolean getBoolean(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
        }
        return false;
    }

    private Object getObject(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key)) {
                return data.get(key);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof Map) {
                return (Map<String, Object>) value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Object> getList(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof List) {
                return (List<Object>) value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> data, String... keys) {
        List<Object> list = getList(data, keys);
        if (list == null) {
            return null;
        }

        List<String> result = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                result.add(Objects.toString(item));
            }
        }
        return result;
    }

    /**
     * 验证工作流定义
     */
    private void validateWorkflow(Workflow workflow) throws WorkflowException {
        if (workflow == null) {
            throw new WorkflowException("Workflow cannot be null");
        }

        if (workflow.getPhase() == null || workflow.getPhase().isEmpty()) {
            throw new WorkflowException("Workflow phase is required");
        }

        if (workflow.getSteps() == null || workflow.getSteps().isEmpty()) {
            throw new WorkflowException("Workflow must have at least one step");
        }

        // 验证每个步骤
        for (Workflow.WorkflowStep step : workflow.getSteps()) {
            if (step.getId() == null || step.getId().isEmpty()) {
                throw new WorkflowException("Step ID is required");
            }

            if (step.getSkill() == null || step.getSkill().isEmpty()) {
                throw new WorkflowException("Step skill is required for: " + step.getId());
            }

            // 验证mode
            if (step.getMode() != null &&
                !step.getMode().equals("required") &&
                !step.getMode().equals("optional")) {
                throw new WorkflowException("Invalid mode for step " + step.getId() +
                    ": must be 'required' or 'optional'");
            }

            // 验证条件表达式的语法（如果有）
            if (step.getCondition() != null && !step.getCondition().isBlank()) {
                try {
                    // 仅验证语法，不实际求值
                    ExpressionEvaluator.evaluate(step.getCondition(), new HashMap<>());
                } catch (ExpressionEvaluator.ExpressionException e) {
                    throw new WorkflowException("Invalid condition expression for step " + step.getId() +
                        ": " + step.getCondition(), e);
                }
            }
        }
    }
}
