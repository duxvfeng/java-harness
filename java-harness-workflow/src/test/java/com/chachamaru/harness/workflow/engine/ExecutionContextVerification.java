package com.chachamaru.harness.workflow.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * ExecutionContext 功能验证
 */
public class ExecutionContextVerification {

    public static void main(String[] args) {
        System.out.println("=== ExecutionContext 功能验证 ===\n");

        testBasicVariableManagement();
        testFileContextManagement();
        testSessionStateManagement();
        testExecutionStack();
        testChildContext();
        testTemplateRendering();

        System.out.println("\n✅ 所有验证通过！");
    }

    private static void testBasicVariableManagement() {
        System.out.println("1. 基本变量管理");
        ExecutionContext context = new ExecutionContext();

        context.setVariable("name", "Alice");
        context.setVariable("count", 42);
        context.setVariable("flag", true);

        assert "Alice".equals(context.getVariable("name")) : "String variable failed";
        assert Integer.valueOf(42).equals(context.getVariable("count", Integer.class)) : "Integer variable failed";
        assert Boolean.TRUE.equals(context.getVariable("flag", Boolean.class)) : "Boolean variable failed";

        assert context.hasVariable("name") : "hasVariable check failed";
        assert !context.hasVariable("nonexistent") : "hasVariable for non-existent failed";

        System.out.println("   ✓ 变量设置、获取、检查正常");
    }

    private static void testFileContextManagement() {
        System.out.println("2. 文件上下文管理");
        ExecutionContext context = new ExecutionContext();

        context.setFileContext("Plans.md", "task content");
        context.setFileContext("spec.md", Map.of("status", "approved"));

        assert "task content".equals(context.getFileContext("Plans.md")) : "File context failed";
        assert context.getFileContext("spec.md") != null : "Complex file context failed";

        System.out.println("   ✓ 文件上下文管理正常");
    }

    private static void testSessionStateManagement() {
        System.out.println("3. 会话状态管理");
        ExecutionContext context = new ExecutionContext();

        context.setSessionState("user_id", "user-123");
        context.setSessionState("current_phase", "7.1.2");

        assert "user-123".equals(context.getSessionState("user_id")) : "Session state failed";
        assert "7.1.2".equals(context.getSessionState("current_phase")) : "Current phase failed";

        System.out.println("   ✓ 会话状态管理正常");
    }

    private static void testExecutionStack() {
        System.out.println("4. 执行栈管理");
        ExecutionContext context = new ExecutionContext();

        context.pushExecution("step1");
        assert "step1".equals(context.getCurrentExecution()) : "Push step1 failed";

        context.pushExecution("step2");
        assert "step2".equals(context.getCurrentExecution()) : "Push step2 failed";

        String popped = context.popExecution();
        assert "step2".equals(popped) : "Pop step2 failed";
        assert "step1".equals(context.getCurrentExecution()) : "After pop, step1 should be current";

        context.popExecution();
        assert context.getCurrentExecution() == null : "Stack should be empty";

        System.out.println("   ✓ 执行栈管理正常");
    }

    private static void testChildContext() {
        System.out.println("5. 子上下文管理");
        ExecutionContext parent = new ExecutionContext();
        parent.setVariable("parent_var", "parent_value");
        parent.setSessionState("session_key", "session_value");

        // 创建子上下文
        ExecutionContext child = parent.createChildContext();

        // 子上下文应该继承父上下文的变量和会话状态
        assert "parent_value".equals(child.getVariable("parent_var")) : "Child should inherit parent variables";
        assert "session_value".equals(child.getSessionState("session_key")) : "Child should inherit session state";

        // 子上下文修改不影响父上下文
        child.setVariable("child_var", "child_value");
        assert parent.getVariable("child_var") == null : "Parent should not see child variables";

        // 合并子上下文
        child.setVariable("parent_var", "updated_value");
        parent.mergeChildContext(child);
        assert "updated_value".equals(parent.getVariable("parent_var")) : "Merge should overwrite parent variable";
        assert "child_value".equals(parent.getVariable("child_var")) : "Merge should add child variable";

        System.out.println("   ✓ 子上下文管理正常");
    }

    private static void testTemplateRendering() {
        System.out.println("6. 模板渲染（${variable} 语法）");
        ExecutionContext context = new ExecutionContext();

        context.setVariable("name", "Alice");
        context.setVariable("count", 42);

        String template = "Hello ${name}, you have ${count} tasks";
        String rendered = context.renderTemplate(template);

        assert "Hello Alice, you have 42 tasks".equals(rendered) :
            "Template rendering failed: got '" + rendered + "'";

        // 测试缺失变量
        String missingTemplate = "Hello ${missing}";
        String missingRendered = context.renderTemplate(missingTemplate);
        assert "Hello ${missing}".equals(missingRendered) : "Missing variable should stay as-is";

        System.out.println("   ✓ 模板渲染正常");
    }
}
