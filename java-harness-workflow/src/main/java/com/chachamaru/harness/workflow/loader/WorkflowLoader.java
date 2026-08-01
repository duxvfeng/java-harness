package com.chachamaru.harness.workflow.loader;

import com.chachamaru.harness.workflow.model.Workflow;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流加载器
 * 负责从YAML文件加载工作流定义
 * 支持变量替换和条件表达式求值
 *
 * 安全说明：使用 Constructor 只能实例化 Workflow 类，避免任意代码执行
 */
public class WorkflowLoader {
    private final Yaml yaml;
    private final Map<String, Workflow> workflowCache;

    public WorkflowLoader() {
        // SnakeYAML 2.x 需要 LoaderOptions
        LoaderOptions options = new LoaderOptions();
        this.yaml = new Yaml(new Constructor(Workflow.class, options));
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

            Workflow workflow = yaml.load(processedContent);

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

    /**
     * 清除缓存
     */
    public void clearCache() {
        workflowCache.clear();
    }
}
