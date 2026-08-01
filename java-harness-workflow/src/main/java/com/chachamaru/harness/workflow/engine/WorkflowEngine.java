package com.chachamaru.harness.workflow.engine;

import com.chachamaru.harness.collaboration.skills.Skill;
import com.chachamaru.harness.collaboration.skills.SkillContext;
import com.chachamaru.harness.collaboration.skills.SkillRegistry;
import com.chachamaru.harness.workflow.loader.WorkflowException;
import com.chachamaru.harness.workflow.loader.WorkflowLoader;
import com.chachamaru.harness.workflow.models.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 工作流执行引擎
 * 负责执行工作流的各个步骤，处理条件、并行执行等
 */
public class WorkflowEngine {
    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowLoader workflowLoader;
    private final SkillRegistry skillRegistry;
    private final ExecutionContext executionContext;
    private final ConditionExpressionEvaluator conditionEvaluator;
    private final ExecutorService executorService;

    public WorkflowEngine(WorkflowLoader workflowLoader, SkillRegistry skillRegistry) {
        this.workflowLoader = workflowLoader;
        this.skillRegistry = skillRegistry;
        this.executionContext = new ExecutionContext();
        this.conditionEvaluator = new ConditionExpressionEvaluator();
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * 执行工作流
     */
    public WorkflowExecutionResult executeWorkflow(String workflowName, Map<String, Object> input) {
        WorkflowExecutionResult result = new WorkflowExecutionResult();
        result.setWorkflowName(workflowName);
        result.setStartTime(System.currentTimeMillis());

        try {
            // 加载工作流定义
            Workflow workflow = workflowLoader.loadDefaultWorkflow(workflowName);
            result.setPhase(workflow.getPhase());

            log.info("Executing workflow: {} ({})", workflowName, workflow.getDescription());

            // 初始化执行上下文
            executionContext.clear();
            if (input != null) {
                executionContext.putAll(input);
            }

            // 执行工作流步骤
            List<WorkflowStepExecution> stepResults = executeWorkflowSteps(workflow);
            result.setStepExecutions(stepResults);

            // 检查是否所有必需步骤都成功
            boolean allRequiredSuccessful = stepResults.stream()
                .filter(step -> step.getStep().getMode().equals("required"))
                .allMatch(WorkflowStepExecution::isSuccess);

            if (allRequiredSuccessful) {
                result.setSuccess(true);
                result.setMessage(renderTemplate(workflow.getOnSuccess().getMessage(), executionContext));
                log.info("Workflow {} completed successfully", workflowName);
            } else {
                result.setSuccess(false);
                result.setMessage(renderTemplate(workflow.getOnError().getMessage(), executionContext));
                log.warn("Workflow {} completed with errors", workflowName);
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Workflow execution failed: " + e.getMessage());
            log.error("Failed to execute workflow: " + workflowName, e);
        } finally {
            result.setEndTime(System.currentTimeMillis());
            result.setDuration(result.getEndTime() - result.getStartTime());
        }

        return result;
    }

    /**
     * 执行工作流的所有步骤
     */
    private List<WorkflowStepExecution> executeWorkflowSteps(Workflow workflow) {
        List<WorkflowStepExecution> results = new ArrayList<>();

        // 分离并行和串行步骤
        List<Workflow.WorkflowStep> parallelSteps = workflow.getSteps().stream()
            .filter(step -> step.isParallel())
            .collect(Collectors.toList());

        List<Workflow.WorkflowStep> sequentialSteps = workflow.getSteps().stream()
            .filter(step -> !step.isParallel())
            .collect(Collectors.toList());

        // 先执行串行步骤
        for (Workflow.WorkflowStep step : sequentialSteps) {
            if (shouldExecuteStep(step)) {
                WorkflowStepExecution stepResult = executeStep(step);
                results.add(stepResult);

                // 更新执行上下文
                updateExecutionContext(stepResult);

                // 如果必需步骤失败，停止执行
                if (!stepResult.isSuccess() && step.getMode().equals("required")) {
                    log.error("Required step {} failed, stopping workflow", step.getId());
                    break;
                }
            }
        }

        // 执行并行步骤
        if (!parallelSteps.isEmpty()) {
            List<WorkflowStepExecution> parallelResults = executeParallelSteps(parallelSteps);
            results.addAll(parallelResults);

            // 更新执行上下文
            for (WorkflowStepExecution stepResult : parallelResults) {
                updateExecutionContext(stepResult);
            }
        }

        return results;
    }

    /**
     * 并行执行多个步骤
     */
    private List<WorkflowStepExecution> executeParallelSteps(List<Workflow.WorkflowStep> steps) {
        List<WorkflowStepExecution> results = new ArrayList<>();

        try {
            List<CompletableFuture<WorkflowStepExecution>> futures = new ArrayList<>();

            for (Workflow.WorkflowStep step : steps) {
                if (shouldExecuteStep(step)) {
                    CompletableFuture<WorkflowStepExecution> future =
                        CompletableFuture.supplyAsync(() -> executeStep(step), executorService);
                    futures.add(future);
                }
            }

            // 等待所有并行步骤完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 收集结果
            for (CompletableFuture<WorkflowStepExecution> future : futures) {
                results.add(future.get());
            }

        } catch (Exception e) {
            log.error("Error executing parallel steps", e);
        }

        return results;
    }

    /**
     * 执行单个步骤
     */
    private WorkflowStepExecution executeStep(Workflow.WorkflowStep step) {
        WorkflowStepExecution execution = new WorkflowStepExecution();
        execution.setStep(step);
        execution.setStartTime(System.currentTimeMillis());

        try {
            log.info("Executing step: {} using skill: {}", step.getId(), step.getSkill());

            // 获取对应的技能
            Skill skill = skillRegistry.getSkill(step.getSkill());
            if (skill == null) {
                throw new WorkflowException("Skill not found: " + step.getSkill());
            }

            // 准备技能上下文
            SkillContext context = prepareSkillContext(step);

            // 执行技能
            com.chachamaru.harness.collaboration.skills.SkillResult skillResult = skill.execute(context);

            if (skillResult.isSuccess() || skillResult.isApplicable()) {
                execution.setSuccess(true);
                execution.setOutput(extractSkillOutput(skillResult));
                log.info("Step {} completed successfully", step.getId());
            } else {
                execution.setSuccess(false);
                execution.setErrorMessage("Skill execution failed: " + skillResult.getMessage());
                log.warn("Step {} failed: {}", step.getId(), skillResult.getMessage());
            }

        } catch (Exception e) {
            execution.setSuccess(false);
            execution.setErrorMessage("Step execution error: " + e.getMessage());
            log.error("Error executing step: " + step.getId(), e);
        } finally {
            execution.setEndTime(System.currentTimeMillis());
            execution.setDuration(execution.getEndTime() - execution.getStartTime());
        }

        return execution;
    }

    /**
     * 判断是否应该执行步骤
     */
    private boolean shouldExecuteStep(Workflow.WorkflowStep step) {
        // 检查条件
        if (step.getCondition() != null && !step.getCondition().isEmpty()) {
            return evaluateCondition(step.getCondition());
        }

        // 检查模式
        if (step.getMode().equals("optional")) {
            return true; // 可选步骤总是尝试执行
        }

        return true;
    }

    /**
     * 评估条件表达式
     */
    private boolean evaluateCondition(String condition) {
        // 简单的条件评估实现
        // 支持变量检查、布尔表达式等

        try {
            // 替换变量
            String evaluated = condition;
            for (Map.Entry<String, Object> entry : executionContext.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                if (evaluated.contains(placeholder)) {
                    evaluated = evaluated.replace(placeholder, String.valueOf(entry.getValue()));
                }
            }

            // 简单布尔表达式评估
            if (evaluated.equals("true")) return true;
            if (evaluated.equals("false")) return false;

            // 包含检查
            if (evaluated.contains("==")) {
                String[] parts = evaluated.split("==");
                return parts[0].trim().equals(parts[1].trim());
            }

            if (evaluated.contains("!=")) {
                String[] parts = evaluated.split("!=");
                return !parts[0].trim().equals(parts[1].trim());
            }

            // 默认返回true
            return true;

        } catch (Exception e) {
            log.warn("Failed to evaluate condition: {}", condition, e);
            return false;
        }
    }

    /**
     * 准备技能执行上下文
     */
    private SkillContext prepareSkillContext(Workflow.WorkflowStep step) {
        SkillContext context = new SkillContext();

        // 设置基本参数
        Map<String, Object> parameters = new HashMap<>();
        if (step.getInput().getVariables() != null) {
            parameters.putAll(step.getInput().getVariables());
        }

        // 添加执行上下文变量
        for (String contextVar : step.getInput().getContextFrom()) {
            if (executionContext.containsKey(contextVar)) {
                parameters.put(contextVar, executionContext.get(contextVar));
            }
        }

        context.setParameters(parameters);
        context.setSessionId(generateSessionId());

        // 设置文件输入
        if (step.getInput().getFiles() != null) {
            // 这里可以转换为文件对象或其他适当的形式
            context.setInputArtifact(step.getInput().getFiles());
        }

        return context;
    }

    /**
     * 提取技能输出
     */
    private Map<String, Object> extractSkillOutput(com.chachamaru.harness.collaboration.skills.SkillResult skillResult) {
        Map<String, Object> output = new HashMap<>();

        if (skillResult.getArtifact() != null) {
            output.put("artifact", skillResult.getArtifact());
        }

        output.put("success", skillResult.isSuccess());
        output.put("message", skillResult.getMessage());

        return output;
    }

    /**
     * 更新执行上下文
     */
    private void updateExecutionContext(WorkflowStepExecution stepExecution) {
        if (stepExecution.getOutput() != null) {
            executionContext.putAll(stepExecution.getOutput());
        }
    }

    /**
     * 渲染模板
     */
    private String renderTemplate(String template, Map<String, Object> context) {
        if (template == null) return "";

        String result = template;
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * 生成会话ID
     */
    private String generateSessionId() {
        return "workflow-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 获取执行上下文
     */
    public Map<String, Object> getExecutionContext() {
        return new HashMap<>(executionContext);
    }

    /**
     * 关闭引擎
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
