package com.chachamaru.harness.workflow.integration;

import com.chachamaru.harness.workflow.models.Workflow;
import com.chachamaru.harness.workflow.loader.WorkflowLoader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流集成示例
 * 展示如何使用工作流编排系统
 */
public class WorkflowIntegrationExample {

    /**
     * 基础工作流执行示例
     */
    public static void basicWorkflowExample() {
        try {
            // 1. 创建工作流加载器
            WorkflowLoader loader = new WorkflowLoader();

            // 2. 加载默认工作流
            Map<String, Workflow> workflows = loader.loadAllDefaultWorkflows();

            System.out.println("=== 可用工作流列表 ===");
            workflows.forEach((name, workflow) -> {
                System.out.println("- " + name + ": " + workflow.getDescription());
            });

            // 3. 执行计划工作流
            if (workflows.containsKey("plan")) {
                Workflow planWorkflow = workflows.get("plan");
                System.out.println("\n=== 执行计划工作流 ===");
                System.out.println("阶段: " + planWorkflow.getPhase());
                System.out.println("步骤数量: " + planWorkflow.getSteps().size());

                // 展示工作流步骤
                for (Workflow.WorkflowStep step : planWorkflow.getSteps()) {
                    System.out.println("- " + step.getId() + ": 使用技能 " + step.getSkill());
                    if (step.getCondition() != null) {
                        System.out.println("  条件: " + step.getCondition());
                    }
                    if (step.isParallel()) {
                        System.out.println("  并行执行: 是");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("工作流执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 条件表达式示例
     */
    public static void conditionExpressionExample() {
        com.chachamaru.harness.workflow.engine.ConditionExpressionEvaluator evaluator =
            new com.chachamaru.harness.workflow.engine.ConditionExpressionEvaluator();
        com.chachamaru.harness.workflow.engine.ExecutionContext context =
            new com.chachamaru.harness.workflow.engine.ExecutionContext();

        // 设置测试变量
        context.setVariable("project_type", "new");
        context.setVariable("user_approved", true);
        context.setVariable("task_count", 5);

        System.out.println("=== 条件表达式测试 ===");

        // 测试各种条件
        testCondition(evaluator, context, "project_type == 'new'", "项目类型检查");
        testCondition(evaluator, context, "user_approved == true", "用户批准检查");
        testCondition(evaluator, context, "task_count > 3", "任务数量检查");
        testCondition(evaluator, context, "project_type == 'new' && user_approved == true", "组合条件检查");
        testCondition(evaluator, context, "task_count > 3 || task_count < 2", "OR条件检查");
    }

    private static void testCondition(com.chachamaru.harness.workflow.engine.ConditionExpressionEvaluator evaluator,
                                   com.chachamaru.harness.workflow.engine.ExecutionContext context,
                                   String condition, String description) {
        try {
            boolean result = evaluator.evaluate(condition, context);
            System.out.println(description + ": " + condition + " -> " + result);
        } catch (Exception e) {
            System.err.println(description + ": ERROR - " + e.getMessage());
        }
    }

    /**
     * 执行上下文示例
     */
    public static void executionContextExample() {
        com.chachamaru.harness.workflow.engine.ExecutionContext context =
            new com.chachamaru.harness.workflow.engine.ExecutionContext();

        System.out.println("=== 执行上下文管理示例 ===");

        // 设置变量
        context.setVariable("project_name", "java-harness");
        context.setVariable("version", "4.1.0");
        context.setVariable("feature_ready", true);

        // 设置文件上下文
        context.setFileContext("Plans.md", "计划文件");
        context.setFileContext("README.md", "项目说明");

        // 设置会话状态
        context.setSessionState("current_user", "developer");
        context.setSessionState("session_id", "sess-12345");

        // 渲染模板
        String template = "项目 {{project_name}} 版本 {{version}} 准备就绪: {{feature_ready}}";
        String rendered = context.renderTemplate(template);

        System.out.println("模板渲染结果:");
        System.out.println("原始模板: " + template);
        System.out.println("渲染结果: " + rendered);
        System.out.println("上下文摘要:");
        System.out.println(context.getSummary());
    }

    /**
     * 工作流文件解析示例
     */
    public static void workflowFileParsingExample() {
        try {
            WorkflowLoader loader = new WorkflowLoader();
            File defaultWorkflowDir = new File("workflows/default");

            System.out.println("=== 工作流文件解析示例 ===");

            if (defaultWorkflowDir.exists()) {
                Map<String, Workflow> workflows = loader.loadWorkflowsFromDirectory(defaultWorkflowDir);

                System.out.println("发现 " + workflows.size() + " 个工作流文件:");

                for (Map.Entry<String, Workflow> entry : workflows.entrySet()) {
                    Workflow workflow = entry.getValue();
                    System.out.println("\n工作流: " + entry.getKey());
                    System.out.println("阶段: " + workflow.getPhase());
                    System.out.println("描述: " + workflow.getDescription());
                    System.out.println("步骤数: " + workflow.getSteps().size());

                    // 展示步骤详情
                    for (Workflow.WorkflowStep step : workflow.getSteps()) {
                        System.out.println("  - " + step.getId() + " (技能: " + step.getSkill() + ")");
                        if (step.getCondition() != null) {
                            System.out.println("    条件: " + step.getCondition());
                        }
                        if (step.isParallel()) {
                            System.out.println("    并行: 是");
                        }
                        if (step.getMode() != null) {
                            System.out.println("    模式: " + step.getMode());
                        }
                    }
                }
            } else {
                System.out.println("默认工作流目录不存在: " + defaultWorkflowDir.getAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("工作流解析失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 主方法 - 运行所有示例
     */
    public static void main(String[] args) {
        System.out.println("Java Harness 工作流编排系统 - 集成示例\n");

        // 1. 基础工作流执行示例
        basicWorkflowExample();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 2. 条件表达式示例
        conditionExpressionExample();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 3. 执行上下文示例
        executionContextExample();

        System.out.println("\n" + "=".repeat(50) + "\n");

        // 4. 工作流文件解析示例
        workflowFileParsingExample();

        System.out.println("\n" + "=".repeat(50) + "\n");
        System.out.println("所有示例执行完成！");
        System.out.println("\n工作流编排系统已准备就绪，可以按照Go项目的方式进行流程编排。");
    }
}
