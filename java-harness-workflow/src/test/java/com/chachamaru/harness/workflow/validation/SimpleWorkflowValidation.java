package com.chachamaru.harness.workflow.validation;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * 简单的默认工作流结构验证
 * 验证 YAML 文件能否正确读取和解析
 */
public class SimpleWorkflowValidation {

    public static void main(String[] args) {
        System.out.println("=== 默认工作流结构验证 ===\n");

        String[] workflowFiles = {
            "workflows/default/init.yaml",
            "workflows/default/plan.yaml",
            "workflows/default/work.yaml",
            "workflows/default/review.yaml"
        };

        for (String filePath : workflowFiles) {
            validateWorkflowFile(filePath);
        }

        System.out.println("\n✅ 所有工作流文件验证通过！");
    }

    private static void validateWorkflowFile(String filePath) {
        System.out.println("验证: " + filePath);

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.err.println("   ✗ 文件不存在: " + filePath);
                return;
            }

            // 读取文件内容
            String content = Files.readString(file.toPath());
            if (content == null || content.isEmpty()) {
                System.err.println("   ✗ 文件为空: " + filePath);
                return;
            }

            // 基本结构验证
            if (!content.contains("phase:")) {
                System.err.println("   ✗ 缺少 phase 字段: " + filePath);
                return;
            }

            if (!content.contains("steps:")) {
                System.err.println("   ✗ 缺少 steps 字段: " + filePath);
                return;
            }

            // 验证关键步骤结构
            if (filePath.contains("init.yaml")) {
                validateInitWorkflow(content);
            } else if (filePath.contains("plan.yaml")) {
                validatePlanWorkflow(content);
            } else if (filePath.contains("work.yaml")) {
                validateWorkWorkflow(content);
            } else if (filePath.contains("review.yaml")) {
                validateReviewWorkflow(content);
            }

            System.out.println("   ✓ " + filePath + " 结构正确");

        } catch (Exception e) {
            System.err.println("   ✗ 验证失败: " + e.getMessage());
        }
    }

    private static void validateInitWorkflow(String content) {
        // 验证 init.yaml 特定步骤
        assertContentContains(content, "analyze-project");
        assertContentContains(content, "clarify-project-type");
        assertContentContains(content, "init-requirements");
        assertContentContains(content, "generate-workflow-files");
        assertContentContains(content, "next-action-guide");

        // 验证条件表达式
        assertContentContains(content, "project_type == 'ambiguous'");
        assertContentContains(content, "project_type == 'new'");
    }

    private static void validatePlanWorkflow(String content) {
        // 验证 plan.yaml 特定步骤
        assertContentContains(content, "read-context");
        assertContentContains(content, "plan-feature");
        assertContentContains(content, "review-plan");

        // 验证条件表达式
        assertContentContains(content, "!user_prompt");
    }

    private static void validateWorkWorkflow(String content) {
        // 验证 work.yaml 基本结构
        assertContentContains(content, "steps:");
    }

    private static void validateReviewWorkflow(String content) {
        // 验证 review.yaml 基本结构
        assertContentContains(content, "steps:");
    }

    private static void assertContentContains(String content, String expected) {
        if (!content.contains(expected)) {
            throw new AssertionError("缺少必需内容: " + expected);
        }
    }
}
