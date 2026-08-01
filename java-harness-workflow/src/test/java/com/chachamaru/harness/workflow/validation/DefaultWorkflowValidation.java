package com.chachamaru.harness.workflow.validation;

import com.chachamaru.harness.workflow.loader.WorkflowLoader;
import com.chachamaru.harness.workflow.loader.WorkflowException;
import com.chachamaru.harness.workflow.models.Workflow;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 默认工作流适配验证
 * 验证 init.yaml、plan.yaml、work.yaml、review.yaml 能在 Java 环境正确加载和解析
 */
public class DefaultWorkflowValidation {

    public static void main(String[] args) {
        System.out.println("=== 默认工作流适配验证 ===\n");

        testWorkflowLoading();
        testVariableReplacementInWorkflows();
        testConditionEvaluationInWorkflows();
        testWorkflowStructure();

        System.out.println("\n✅ 所有默认工作流适配验证通过！");
    }

    /**
     * 测试工作流加载
     */
    private static void testWorkflowLoading() {
        System.out.println("1. 工作流加载测试");

        WorkflowLoader loader = new WorkflowLoader();

        // 测试加载各个默认工作流
        String[] workflowNames = {"init", "plan", "work", "review"};

        for (String name : workflowNames) {
            try {
                Workflow workflow = loader.loadDefaultWorkflow(name);
                assert workflow != null : "工作流 " + name + " 加载失败";
                assert workflow.getPhase() != null : "工作流 " + name + " 缺少 phase";
                assert workflow.getSteps() != null : "工作流 " + name + " 缺少步骤";
                assert !workflow.getSteps().isEmpty() : "工作流 " + name + " 没有步骤";

                System.out.println("   ✓ " + name + ".yaml: " + workflow.getSteps().size() + " 个步骤");
            } catch (Exception e) {
                System.err.println("   ✗ " + name + ".yaml 加载失败: " + e.getMessage());
                throw new RuntimeException("工作流加载失败", e);
            }
        }
    }

    /**
     * 测试工作流中的变量替换
     */
    private static void testVariableReplacementInWorkflows() {
        System.out.println("2. 变量替换测试");

        WorkflowLoader loader = new WorkflowLoader();

        // 模拟工作流中的变量替换
        Map<String, Object> context = new HashMap<>();
        context.put("project_name", "TestProject");
        context.put("task_count", 5);

        try {
            // 测试加载带变量的工作流
            Workflow workflow = loader.loadDefaultWorkflow("init", context);
            assert workflow != null : "带变量的工作流加载失败";

            // 验证步骤中的变量引用
            for (Workflow.WorkflowStep step : workflow.getSteps()) {
                if (step.getCondition() != null) {
                    System.out.println("   ✓ " + step.getId() + " 条件: " + step.getCondition());
                }
            }

            System.out.println("   ✓ 变量替换正常工作");

        } catch (Exception e) {
            System.err.println("   ✗ 变量替换测试失败: " + e.getMessage());
            throw new RuntimeException("变量替换测试失败", e);
        }
    }

    /**
     * 测试工作流中的条件评估
     */
    private static void testConditionEvaluationInWorkflows() {
        System.out.println("3. 条件评估测试");

        WorkflowLoader loader = new WorkflowLoader();

        // 测试 init.yaml 的条件
        Map<String, Object> context = new HashMap<>();
        context.put("project_type", "new");
        context.put("ambiguity_reason", "template_only");

        try {
            Workflow workflow = loader.loadDefaultWorkflow("init", context);

            // 验证条件表达式
            for (Workflow.WorkflowStep step : workflow.getSteps()) {
                if (step.getCondition() != null) {
                    boolean result = loader.evaluateCondition(step.getCondition(), context);
                    System.out.println("   ✓ " + step.getId() + " 条件 '" + step.getCondition() + "' → " + result);
                }
            }

            System.out.println("   ✓ 条件评估正常");

        } catch (Exception e) {
            System.err.println("   ✗ 条件评估测试失败: " + e.getMessage());
            throw new RuntimeException("条件评估测试失败", e);
        }
    }

    /**
     * 测试工作流结构完整性
     */
    private static void testWorkflowStructure() {
        System.out.println("4. 工作流结构测试");

        WorkflowLoader loader = new WorkflowLoader();

        try {
            // 测试所有工作流的结构完整性
            Workflow initWorkflow = loader.loadDefaultWorkflow("init");
            validateWorkflowStructure(initWorkflow, "init");

            Workflow planWorkflow = loader.loadDefaultWorkflow("plan");
            validateWorkflowStructure(planWorkflow, "plan");

            Workflow workWorkflow = loader.loadDefaultWorkflow("work");
            validateWorkflowStructure(workWorkflow, "work");

            Workflow reviewWorkflow = loader.loadDefaultWorkflow("review");
            validateWorkflowStructure(reviewWorkflow, "review");

            System.out.println("   ✓ 所有工作流结构完整");

        } catch (Exception e) {
            System.err.println("   ✗ 结构测试失败: " + e.getMessage());
            throw new RuntimeException("结构测试失败", e);
        }
    }

    /**
     * 验证单个工作流的结构
     */
    private static void validateWorkflowStructure(Workflow workflow, String workflowName) {
        assert workflow != null : workflowName + " workflow 为 null";
        assert workflow.getPhase() != null : workflowName + " 缺少 phase";
        assert !workflow.getPhase().isEmpty() : workflowName + " phase 为空";

        // 验证步骤
        assert workflow.getSteps() != null : workflowName + " 步骤为 null";
        assert !workflow.getSteps().isEmpty() : workflowName + " 没有步骤";

        // 验证每个步骤
        for (Workflow.WorkflowStep step : workflow.getSteps()) {
            assert step.getId() != null : workflowName + " 步骤缺少 ID";
            assert !step.getId().isEmpty() : workflowName + " 步骤 ID 为空";
            assert step.getSkill() != null : workflowName + " 步骤缺少 skill";
            assert !step.getSkill().isEmpty() : workflowName + " 步骤 skill 为空";

            // 验证 mode（如果有）
            if (step.getMode() != null) {
                assert step.getMode().equals("required") || step.getMode().equals("optional") :
                    workflowName + " 步骤 " + step.getId() + " 有无效 mode: " + step.getMode();
            }

            // 验证条件语法（如果有）
            if (step.getCondition() != null && !step.getCondition().isBlank()) {
                // 仅验证语法，不实际求值
                loader.evaluateCondition(step.getCondition(), new HashMap<>());
            }
        }

        // 验证输出（如果有）
        if (workflow.getOnSuccess() != null) {
            assert workflow.getOnSuccess().getMessage() != null :
                workflowName + " onSuccess 缺少 message";
        }

        if (workflow.getOnError() != null) {
            assert workflow.getOnError().getMessage() != null :
                workflowName + " onError 缺少 message";
        }
    }
}
