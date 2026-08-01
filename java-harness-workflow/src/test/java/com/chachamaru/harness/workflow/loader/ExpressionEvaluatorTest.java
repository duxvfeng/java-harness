package com.chachamaru.harness.workflow.loader;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpressionEvaluator 测试
 */
class ExpressionEvaluatorTest {

    @Test
    void testTrueCondition() throws Exception {
        boolean result = ExpressionEvaluator.evaluate("true", new HashMap<>());
        assertTrue(result);
    }

    @Test
    void testFalseCondition() throws Exception {
        boolean result = ExpressionEvaluator.evaluate("false", new HashMap<>());
        assertFalse(result);
    }

    @Test
    void testEmptyCondition() throws Exception {
        boolean result = ExpressionEvaluator.evaluate("", new HashMap<>());
        assertTrue(result); // 空条件默认为 true
    }

    @Test
    void testNullCondition() throws Exception {
        boolean result = ExpressionEvaluator.evaluate((String) null, new HashMap<>());
        assertTrue(result); // null 条件默认为 true
    }

    @Test
    void testStringEquality() throws Exception {
        Map<String, Object> context = Map.of("project_type", "new");
        boolean result = ExpressionEvaluator.evaluate("project_type == 'new'", context);
        assertTrue(result);
    }

    @Test
    void testStringInequality() throws Exception {
        Map<String, Object> context = Map.of("project_type", "existing");
        boolean result = ExpressionEvaluator.evaluate("project_type == 'new'", context);
        assertFalse(result);
    }

    @Test
    void testStringNotEquals() throws Exception {
        Map<String, Object> context = Map.of("status", "done");
        boolean result = ExpressionEvaluator.evaluate("status != 'pending'", context);
        assertTrue(result);
    }

    @Test
    void testNumericComparison() throws Exception {
        Map<String, Object> context = Map.of("count", 5);
        assertTrue(ExpressionEvaluator.evaluate("count > 3", context));
        assertTrue(ExpressionEvaluator.evaluate("count >= 5", context));
        assertFalse(ExpressionEvaluator.evaluate("count < 3", context));
        assertFalse(ExpressionEvaluator.evaluate("count <= 4", context));
    }

    @Test
    void testLogicalAnd() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", true);
        assertTrue(ExpressionEvaluator.evaluate("a && b", context));

        context.put("b", false);
        assertFalse(ExpressionEvaluator.evaluate("a && b", context));
    }

    @Test
    void testLogicalOr() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        assertTrue(ExpressionEvaluator.evaluate("a || b", context));

        context.put("a", false);
        assertFalse(ExpressionEvaluator.evaluate("a || b", context));
    }

    @Test
    void testLogicalNot() throws Exception {
        Map<String, Object> context = Map.of("flag", false);
        assertTrue(ExpressionEvaluator.evaluate("!flag", context));
    }

    @Test
    void testComplexExpression() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("project_type", "new");
        context.put("has_dependencies", false);

        // project_type == 'new' && !has_dependencies
        boolean result = ExpressionEvaluator.evaluate(
            "project_type == 'new' && !has_dependencies",
            context
        );
        assertTrue(result);
    }

    @Test
    void testParentheses() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        context.put("c", true);

        // (a || b) && c
        boolean result = ExpressionEvaluator.evaluate("(a || b) && c", context);
        assertTrue(result);
    }

    @Test
    void testStringWithSpecialCharacters() throws Exception {
        Map<String, Object> context = Map.of("path", "C:\\Users\\Test");
        boolean result = ExpressionEvaluator.evaluate("path == 'C:\\\\Users\\\\Test'", context);
        assertTrue(result);
    }

    @Test
    void testDoubleQuotedStrings() throws Exception {
        Map<String, Object> context = Map.of("name", "Alice");
        boolean result = ExpressionEvaluator.evaluate("name == \"Alice\"", context);
        assertTrue(result);
    }

    @Test
    void testNumericVariableComparison() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("task_count", 10);
        context.put("min_tasks", 5);

        assertTrue(ExpressionEvaluator.evaluate("task_count >= min_tasks", context));
        assertFalse(ExpressionEvaluator.evaluate("task_count < min_tasks", context));
    }

    @Test
    void testUndefinedVariable() throws Exception {
        Map<String, Object> context = Map.of("defined_var", 42);
        // 未定义的变量在比较中应该被处理
        boolean result = ExpressionEvaluator.evaluate("undefined_var == null", context);
        assertTrue(result);
    }

    @Test
    void testChainedComparisons() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("project_type", "existing");
        context.put("has_tests", true);
        context.put("test_coverage", 80);

        // project_type == 'existing' && has_tests && test_coverage >= 70
        boolean result = ExpressionEvaluator.evaluate(
            "project_type == 'existing' && has_tests && test_coverage >= 70",
            context
        );
        assertTrue(result);
    }

    @Test
    void testComplexBooleanLogic() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        context.put("c", true);

        // a || (b && c)  = true
        assertTrue(ExpressionEvaluator.evaluate("a || (b && c)", context));

        // (a || b) && c = true
        assertTrue(ExpressionEvaluator.evaluate("(a || b) && c", context));

        // !(a && b) && c = true
        assertTrue(ExpressionEvaluator.evaluate("!(a && b) && c", context));
    }

    @Test
    void testWhitespaceTolerance() throws Exception {
        Map<String, Object> context = Map.of("x", 5);

        // 各种空白情况
        assertTrue(ExpressionEvaluator.evaluate("x==5", context));
        assertTrue(ExpressionEvaluator.evaluate("x == 5", context));
        assertTrue(ExpressionEvaluator.evaluate("x  ==  5", context));
        assertTrue(ExpressionEvaluator.evaluate("( x == 5 )", context));
    }

    @Test
    void testInvalidSyntax() {
        Map<String, Object> context = Map.of("x", 5);

        assertThrows(Exception.class, () -> {
            ExpressionEvaluator.evaluate("x == ", context);
        });

        assertThrows(Exception.class, () -> {
            ExpressionEvaluator.evaluate("== 5", context);
        });

        assertThrows(Exception.class, () -> {
            ExpressionEvaluator.evaluate("((x == 5)", context);
        });
    }

    @Test
    void testLessThanOrEquals() throws Exception {
        Map<String, Object> context = Map.of("count", 5);

        assertTrue(ExpressionEvaluator.evaluate("count <= 5", context));
        assertTrue(ExpressionEvaluator.evaluate("count <= 10", context));
        assertFalse(ExpressionEvaluator.evaluate("count <= 3", context));
    }

    @Test
    void testGreaterThanOrEquals() throws Exception {
        Map<String, Object> context = Map.of("count", 5);

        assertTrue(ExpressionEvaluator.evaluate("count >= 5", context));
        assertTrue(ExpressionEvaluator.evaluate("count >= 1", context));
        assertFalse(ExpressionEvaluator.evaluate("count >= 10", context));
    }

    @Test
    void testFloatingPointNumbers() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("pi", 3.14159);
        context.put("threshold", 3.0);

        assertTrue(ExpressionEvaluator.evaluate("pi > threshold", context));
        assertTrue(ExpressionEvaluator.evaluate("pi >= 3.14159", context));
    }

    @Test
    void testRealWorldWorkflowCondition() throws Exception {
        // 模拟 init.yaml 中的真实条件
        Map<String, Object> context = new HashMap<>();
        context.put("project_type", "ambiguous");
        context.put("ambiguity_reason", "template_only");

        // project_type == 'ambiguous'
        boolean result = ExpressionEvaluator.evaluate(
            "project_type == 'ambiguous'",
            context
        );
        assertTrue(result);

        // project_type == 'new' || project_type == 'existing'
        result = ExpressionEvaluator.evaluate(
            "project_type == 'new' || project_type == 'existing'",
            context
        );
        assertFalse(result);
    }
}
