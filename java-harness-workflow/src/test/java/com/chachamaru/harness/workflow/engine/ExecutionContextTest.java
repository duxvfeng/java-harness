package com.chachamaru.harness.workflow.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionContext 测试
 */
class ExecutionContextTest {

    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        context = new ExecutionContext();
    }

    @Test
    void testVariableManagement() {
        // 设置变量
        context.setVariable("name", "Alice");
        context.setVariable("count", 42);

        // 获取变量
        assertEquals("Alice", context.getVariable("name"));
        assertEquals(42, context.getVariable("count"));

        // 类型安全获取
        String name = context.getVariable("name", String.class);
        assertEquals("Alice", name);

        Integer count = context.getVariable("count", Integer.class);
        assertEquals(42, count);

        // 不存在的变量
        assertNull(context.getVariable("nonexistent"));
        assertNull(context.getVariable("name", Integer.class)); // 类型不匹配
    }

    @Test
    void testBatchVariableOperations() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("project_type", "new");
        vars.put("has_dependencies", false);
        vars.put("task_count", 5);

        context.setVariables(vars);

        assertEquals("new", context.getVariable("project_type"));
        assertEquals(false, context.getVariable("has_dependencies"));
        assertEquals(5, context.getVariable("task_count"));

        // getAllVariables 返回副本
        Map<String, Object> allVars = context.getAllVariables();
        assertEquals(3, allVars.size());

        // 修改副本不影响原上下文
        allVars.put("new_key", "new_value");
        assertNull(context.getVariable("new_key"));
    }

    @Test
    void testFileContextManagement() {
        context.setFileContext("Plans.md", "task content");
        context.setFileContext("spec.md", Map.of("status", "approved"));

        assertEquals("task content", context.getFileContext("Plans.md"));
        assertNotNull(context.getFileContext("spec.md"));
    }

    @Test
    void testSessionStateManagement() {
        context.setSessionState("user_id", "user-123");
        context.setSessionState("current_phase", "7.1.2");

        assertEquals("user-123", context.getSessionState("user_id"));
        assertEquals("7.1.2", context.getSessionState("current_phase"));
    }

    @Test
    void testExecutionStack() {
        // 测试嵌套执行
        context.pushExecution("step1");
        assertEquals("step1", context.getCurrentExecution());

        context.pushExecution("step2");
        assertEquals("step2", context.getCurrentExecution());

        // 弹出
        String popped = context.popExecution();
        assertEquals("step2", popped);
        assertEquals("step1", context.getCurrentExecution());

        // 清空栈
        context.popExecution();
        assertNull(context.getCurrentExecution());
        assertNull(context.popExecution()); // 空栈返回 null
    }

    @Test
    void testMetadataManagement() {
        context.setMetadata("workflow_name", "test-workflow");
        context.setMetadata("start_time", System.currentTimeMillis());

        assertEquals("test-workflow", context.getMetadata("workflow_name"));
        assertNotNull(context.getMetadata("start_time"));
    }

    @Test
    void testClear() {
        context.setVariable("key", "value");
        context.setFileContext("file", "context");
        context.setSessionState("session", "state");
        context.pushExecution("step");

        assertEquals(1, context.getAllVariables().size());

        context.clear();

        assertEquals(0, context.getAllVariables().size());
        assertNull(context.getVariable("key"));
        assertNull(context.getCurrentExecution());
    }

    @Test
    void testChildContextCreation() {
        // 父上下文设置变量
        context.setVariable("parent_var", "parent_value");
        context.setSessionState("session_key", "session_value");
        context.pushExecution("parent_step");

        // 创建子上下文
        ExecutionContext child = context.createChildContext();

        // 子上下文应该继承父上下文的变量和会话状态
        assertEquals("parent_value", child.getVariable("parent_var"));
        assertEquals("session_value", child.getSessionState("session_key"));

        // 但不继承执行栈
        assertNull(child.getCurrentExecution());

        // 子上下文修改不影响父上下文
        child.setVariable("child_var", "child_value");
        assertNull(context.getVariable("child_var"));
    }

    @Test
    void testChildContextMerge() {
        context.setVariable("parent_var", "parent_value");

        ExecutionContext child = context.createChildContext();
        child.setVariable("child_var", "child_value");
        child.setVariable("parent_var", "updated_value"); // 覆盖父变量

        // 合并子上下文
        context.mergeChildContext(child);

        // 父上下文应该包含子上下文的所有变量
        assertEquals("updated_value", context.getVariable("parent_var")); // 被覆盖
        assertEquals("child_value", context.getVariable("child_var"));
    }

    @Test
    void testTemplateRendering() {
        context.setVariable("name", "Alice");
        context.setVariable("count", 42);

        String template = "Hello ${name}, you have ${count} tasks";
        String rendered = context.renderTemplate(template);

        assertEquals("Hello Alice, you have 42 tasks", rendered);
    }

    @Test
    void testTemplateRenderingWithNullValues() {
        context.setVariable("name", null);
        context.setVariable("count", 42);

        String template = "Hello ${name}, count is ${count}";
        String rendered = context.renderTemplate(template);

        assertEquals("Hello ${name}, count is 42", rendered); // null 变量保持原样（VariableResolver行为）
    }

    @Test
    void testTemplateRenderingWithMissingVariables() {
        String template = "Hello ${missing}";
        String rendered = context.renderTemplate(template);

        assertEquals("Hello ${missing}", rendered); // 缺失变量保持原样
    }

    @Test
    void testHasVariable() {
        assertFalse(context.hasVariable("key"));

        context.setVariable("key", "value");
        assertTrue(context.hasVariable("key"));

        context.setVariable("key", null);
        assertTrue(context.hasVariable("key")); // null 值也算存在
    }

    @Test
    void testGetSummary() {
        context.setVariable("var1", "value1");
        context.setFileContext("file1", "context1");
        context.setSessionState("session1", "state1");
        context.pushExecution("step1");
        context.setMetadata("meta1", "value1");

        String summary = context.getSummary();

        assertTrue(summary.contains("Variables: 1"));
        assertTrue(summary.contains("Files: 1"));
        assertTrue(summary.contains("Session State: 1"));
        assertTrue(summary.contains("Execution Depth: 1"));
        assertTrue(summary.contains("Metadata: 1"));
    }

    @Test
    void testComplexObjectVariables() {
        // 测试复杂对象作为变量
        Map<String, Object> complexObject = new HashMap<>();
        complexObject.put("nested_key", "nested_value");

        context.setVariable("complex", complexObject);

        @SuppressWarnings("unchecked")
        Map<String, Object> retrieved = context.getVariable("complex", Map.class);
        assertNotNull(retrieved);
        assertEquals("nested_value", retrieved.get("nested_key"));
    }

    @Test
    void testVariableOverwrite() {
        context.setVariable("key", "value1");
        assertEquals("value1", context.getVariable("key"));

        context.setVariable("key", "value2");
        assertEquals("value2", context.getVariable("key"));
    }

    @Test
    void testNullHandling() {
        // 测试 null 值处理
        context.setVariable("null_key", null);
        assertTrue(context.hasVariable("null_key"));
        assertNull(context.getVariable("null_key"));

        // 获取不存在的变量
        assertNull(context.getVariable("nonexistent"));
        assertNull(context.getFileContext("nonexistent"));
        assertNull(context.getSessionState("nonexistent"));
        assertNull(context.getMetadata("nonexistent"));
    }

    @Test
    void testMultipleChildContexts() {
        context.setVariable("base", "base_value");

        // 创建第一个子上下文
        ExecutionContext child1 = context.createChildContext();
        child1.setVariable("var1", "value1");

        // 创建第二个子上下文
        ExecutionContext child2 = context.createChildContext();
        child2.setVariable("var2", "value2");

        // 子上下文之间独立
        assertNull(child1.getVariable("var2"));
        assertNull(child2.getVariable("var1"));

        // 但都继承父上下文
        assertEquals("base_value", child1.getVariable("base"));
        assertEquals("base_value", child2.getVariable("base"));
    }

    @Test
    void testExecutionContextIsolation() {
        // 测试不同上下文之间的隔离
        ExecutionContext context1 = new ExecutionContext();
        ExecutionContext context2 = new ExecutionContext();

        context1.setVariable("key", "value1");
        context2.setVariable("key", "value2");

        assertEquals("value1", context1.getVariable("key"));
        assertEquals("value2", context2.getVariable("key"));

        // 互不影响
        assertNotEquals(context1.getVariable("key"), context2.getVariable("key"));
    }
}
