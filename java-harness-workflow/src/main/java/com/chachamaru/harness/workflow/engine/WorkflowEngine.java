package com.chachamaru.harness.workflow.engine;

// import com.chachamaru.harness.collaboration.skill.Skill;
// import com.chachamaru.harness.collaboration.skill.SkillRegistry;
// import com.chachamaru.harness.collaboration.skill.model.SkillContext;
// import com.chachamaru.harness.collaboration.skill.model.SkillResult;
import com.chachamaru.harness.workflow.loader.WorkflowLoader;
import com.chachamaru.harness.workflow.loader.WorkflowException;
import com.chachamaru.harness.workflow.model.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 工作流执行引擎
 * 负责执行工作流的各个步骤，处理条件、并行执行等
 *
 * 注意：此版本暂时移除了对 Skill 系统的依赖以解决循环依赖问题。
 * 未来版本将重新设计架构以支持 Skill 集成。
 */
public class WorkflowEngine {
    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final WorkflowLoader workflowLoader;
    // private final SkillRegistry skillRegistry;
    private final ExecutionContext executionContext;
    private final ExecutorService executorService;

    public WorkflowEngine(WorkflowLoader workflowLoader) { // , SkillRegistry skillRegistry) {
        this.workflowLoader = workflowLoader;
        // this.skillRegistry = skillRegistry;
        this.executionContext = new ExecutionContext();
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
            Workflow workflow = workflowLoader.loadDefaultWorkflow(workflowName, input);
            result.setPhase(workflow.getPhase());

            log.info("Executing workflow: {} ({})", workflowName, workflow.getDescription());

            // 初始化执行上下文
            executionContext.clear();
            if (input != null) {
                executionContext.setVariables(input);
            }

            // 执行工作流步骤
            List<WorkflowStepExecution> stepResults = executeWorkflowSteps(workflow);

            // 检查是否所有必需步骤都成功
            boolean allRequiredSuccessful = stepResults.stream()
                .filter(step -> step.getStep().getMode().equals("required"))
                .allMatch(WorkflowStepExecution::isSuccess);

            if (allRequiredSuccessful) {
                result.setSuccess(true);
                result.setMessage(renderWorkflowOutput(workflow.getOnSuccess()));
                log.info("Workflow {} completed successfully", workflowName);
            } else {
                result.setSuccess(false);
                result.setMessage(renderWorkflowOutput(workflow.getOnError()));
                log.warn("Workflow {} completed with errors", workflowName);
            }

            // 收集输出变量
            result.setOutputVariables(executionContext.getAllVariables());

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
     * 注意：此版本简化实现，移除了Skill集成以解决循环依赖问题
     */
    private WorkflowStepExecution executeStep(Workflow.WorkflowStep step) {
        WorkflowStepExecution execution = new WorkflowStepExecution();
        execution.setStep(step);
        execution.setStartTime(System.currentTimeMillis());

        executionContext.pushExecution(step.getId());

        try {
            log.info("Executing step: {}", step.getId());

            // 简化实现：直接标记为成功，实际Skill执行将在future版本实现
            execution.setSuccess(true);

            // 创建基础输出
            Map<String, Object> output = new HashMap<>();
            output.put("step_id", step.getId());
            output.put("success", true);
            output.put("message", "Step executed (Skill integration pending)");
            execution.setOutput(output);

            log.info("Step {} completed (simplified execution)", step.getId());

        } catch (Exception e) {
            execution.setSuccess(false);
            execution.setErrorMessage("Step execution error: " + e.getMessage());
            log.error("Error executing step: " + step.getId(), e);
        } finally {
            execution.setEndTime(System.currentTimeMillis());
            execution.setDuration(execution.getEndTime() - execution.getStartTime());
            executionContext.popExecution();
        }

        return execution;
    }

    /**
     * 判断是否应该执行步骤
     */
    private boolean shouldExecuteStep(Workflow.WorkflowStep step) {
        // 检查条件
        if (step.getCondition() != null && !step.getCondition().isEmpty()) {
            try {
                return workflowLoader.evaluateCondition(step.getCondition(), executionContext.getAllVariables());
            } catch (Exception e) {
                log.warn("Failed to evaluate condition for step {}: {}", step.getId(), e.getMessage());
                return false;
            }
        }

        return true;
    }

    /**
     * 准备步骤执行配置（简化版本）
     */
    private Map<String, Object> prepareStepConfiguration(Workflow.WorkflowStep step) {
        Map<String, Object> configuration = new HashMap<>();

        // 添加步骤输入变量
        if (step.getInput() != null && step.getInput().getVariables() != null) {
            // 渲染变量值（支持变量替换）
            Map<String, Object> inputVars = step.getInput().getVariables();
            for (Map.Entry<String, Object> entry : inputVars.entrySet()) {
                String varName = entry.getKey();
                if (executionContext.hasVariable(varName)) {
                    configuration.put(varName, executionContext.getVariable(varName));
                }
            }
        }

        return configuration;
    }

    /**
     * 提取步骤输出（简化版本）
     */
    private Map<String, Object> extractStepOutput(WorkflowStepExecution stepExecution) {
        return new HashMap<>(stepExecution.getOutput());
    }

    /**
     * 更新执行上下文
     */
    private void updateExecutionContext(WorkflowStepExecution stepExecution) {
        if (stepExecution.getOutput() != null) {
            // 提取步骤输出中的变量
            if (stepExecution.getStep().getOutput() != null &&
                stepExecution.getStep().getOutput().getVariables() != null) {

                Map<String, Object> outputVars = stepExecution.getStep().getOutput().getVariables();
                for (String varName : outputVars.keySet()) {
                    if (stepExecution.getOutput().containsKey(varName)) {
                        Object value = stepExecution.getOutput().get(varName);
                        executionContext.setVariable(varName, value);
                    }
                }
            }

            // 添加所有输出变量到执行上下文
            Map<String, Object> outputCopy = new HashMap<>(stepExecution.getOutput());
            executionContext.setVariables(outputCopy);
        }
    }

    /**
     * 渲染工作流输出消息
     */
    private String renderWorkflowOutput(Workflow.WorkflowOutput workflowOutput) {
        if (workflowOutput == null || workflowOutput.getMessage() == null) {
            return "";
        }

        return executionContext.renderTemplate(workflowOutput.getMessage());
    }

    /**
     * 获取执行上下文
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    /**
     * 关闭引擎
     */
    public void shutdown() {
        log.info("Shutting down workflow engine");
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
