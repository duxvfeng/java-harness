package com.chachamaru.harness.workflow.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExpressionEvaluator 完整测试套件
 * 验证所有Go版本支持的条件表达式语法
 */
@DisplayName("条件表达式引擎完整测试")
class ExpressionEvaluatorCompleteTest {

    @Test
    @DisplayName("布尔字面值")
    void testBooleanLiterals() throws Exception {
        assertTrue(ExpressionEvaluator.evaluate("true", new HashMap<>()));
        assertFalse(ExpressionEvaluator.evaluate("false", new HashMap<>()));
    }

    @Test
    @DisplayName("字符串相等比较")
    void testStringEquality() throws Exception {
        Map<String, Object> context = Map.of("status", "done");
        assertTrue(ExpressionEvaluator.evaluate("status == 'done'", context));
        assertFalse(ExpressionEvaluator.evaluate("status == 'pending'", context));
    }

    @Test
    @DisplayName("字符串不等比较")
    void testStringInequality() throws Exception {
        Map<String, Object> context = Map.of("status", "done");
        assertTrue(ExpressionEvaluator.evaluate("status != 'pending'", context));
        assertFalse(ExpressionEvaluator.evaluate("status != 'done'", context));
    }

    @Test
    @DisplayName("数字比较 - 大于")
    void testNumericGreaterThan() throws Exception {
        Map<String, Object> context = Map.of("count", 5);
        assertTrue(ExpressionEvaluator.evaluate("count > 3", context));
        assertFalse(ExpressionEvaluator.evaluate("count > 5", context));
        assertFalse(ExpressionEvaluator.evaluate("count > 10", context));
    }

    @Test
    @DisplayName("数字比较 - 小于")
    void testNumericLessThan() throws Exception {
        Map<String, Object> context = Map.of("count", 5);
        assertTrue(ExpressionEvaluator.evaluate("count < 10", context));
        assertFalse(ExpressionEvaluator.evaluate("count < 5", context));
        assertFalse(ExpressionEvaluator.evaluate("count < 3", context));
    }

    @Test
    @DisplayName("数字比较 - 大于等于")
    void testNumericGreaterOrEqual() throws Exception {
        Map<String, Object> context = Map.of("count", 5);
        assertTrue(ExpressionEvaluator.evaluate("count >= 5", context));
        assertTrue(ExpressionEvaluator.evaluate("count >= 3", context));
        assertFalse(ExpressionEvaluator.evaluate("count >= 10", context));
    }

    @Test
    @DisplayName("数字比较 - 小于等于")
    void testNumericLessOrEqual() throws Exception {
        Map<String, Object> context = Map.of("count", 5);
        assertTrue(ExpressionEvaluator.evaluate("count <= 5", context));
        assertTrue(ExpressionEvaluator.evaluate("count <= 10", context));
        assertFalse(ExpressionEvaluator.evaluate("count <= 3", context));
    }

    @Test
    @DisplayName("逻辑与操作")
    void testLogicalAnd() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", true);
        assertTrue(ExpressionEvaluator.evaluate("a && b", context));

        context.put("b", false);
        assertFalse(ExpressionEvaluator.evaluate("a && b", context));

        context.put("a", false);
        assertFalse(ExpressionEvaluator.evaluate("a && b", context));
    }

    @Test
    @DisplayName("逻辑或操作")
    void testLogicalOr() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        assertTrue(ExpressionEvaluator.evaluate("a || b", context));

        context.put("a", false);
        assertFalse(ExpressionEvaluator.evaluate("a || b", context));

        context.put("b", true);
        assertTrue(ExpressionEvaluator.evaluate("a || b", context));
    }

    @Test
    @DisplayName("逻辑非操作")
    void testLogicalNot() throws Exception {
        Map<String, Object> context = Map.of("flag", true);
        assertFalse(ExpressionEvaluator.evaluate("!flag", context));

        context.put("flag", false);
        assertTrue(ExpressionEvaluator.evaluate("!flag", context));
    }

    @Test
    @DisplayName("括号分组")
    void testParenthesesGrouping() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        context.put("c", true);

        // (a || b) && c
        assertTrue(ExpressionEvaluator.evaluate("(a || b) && c", context));

        // a || (b && c)
        assertTrue(ExpressionEvaluator.evaluate("a || (b && c)", context));

        // !(a && b)
        assertTrue(ExpressionEvaluator.evaluate("!(a && b)", context));
    }

    @Test
    @DisplayName("复杂布尔表达式")
    void testComplexBooleanExpression() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        context.put("c", true);
        context.put("d", false);

        // a && b || c && d
        assertTrue(ExpressionEvaluator.evaluate("a && b || c && d", context));

        // (a || b) && (c || d)
        assertTrue(ExpressionEvaluator.evaluate("(a || b) && (c || d)", context));

        // !a && !b || c
        assertTrue(ExpressionEvaluator.evaluate("!a && !b || c", context));
    }

    @Test
    @DisplayName("变量与字面值混合比较")
    void testMixedVariableLiteralComparison() throws Exception {
        Map<String, Object> context = Map.of("count", 42);

        assertTrue(ExpressionEvaluator.evaluate("count == 42", context));
        assertTrue(ExpressionEvaluator.evaluate("count > 40", context));
        assertTrue(ExpressionEvaluator.evaluate("count <= 100", context));
    }

    @Test
    @DisplayName("浮点数比较")
    void testFloatingPointComparison() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("pi", 3.14159);
        context.put("threshold", 3.0);

        assertTrue(ExpressionEvaluator.evaluate("pi > threshold", context));
        assertTrue(ExpressionEvaluator.evaluate("pi >= 3.14159", context));
        assertFalse(ExpressionEvaluator.evaluate("pi < 3.0", context));
    }

    @Test
    @DisplayName("字符串转数字比较")
    void testStringToNumericComparison() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("str_count", "5");
        context.put("num_count", 10);

        // 数字字符串与数字比较
        assertTrue(ExpressionEvaluator.evaluate("num_count > str_count", context));
    }

    @Test
    @DisplayName("多重嵌套括号")
    void testNestedParentheses() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        context.put("c", true);
        context.put("d", true);
        context.put("e", false);

        // (((a || b) && c) && (d || e))
        assertTrue(ExpressionEvaluator.evaluate("(((a || b) && c) && (d || e))", context));

        // !((a && b) || (c && d))
        assertFalse(ExpressionEvaluator.evaluate("!((a && b) || (c && d))", context));
    }

    @Test
    @DisplayName("真实工作流条件表达式 - init.yaml")
    void testRealWorldInitConditions() throws Exception {
        // 模拟 init.yaml 中的条件
        Map<String, Object> context = new HashMap<>();
        context.put("project_type", "new");
        context.put("ambiguity_reason", "template_only");

        // project_type == 'ambiguous'
        context.put("project_type", "ambiguous");
        assertTrue(ExpressionEvaluator.evaluate("project_type == 'ambiguous'", context));

        // project_type == 'new' || project_type == 'existing'
        assertFalse(ExpressionEvaluator.evaluate("project_type == 'new' || project_type == 'existing'", context));

        context.put("project_type", "new");
        assertTrue(ExpressionEvaluator.evaluate("project_type == 'new' || project_type == 'existing'", context));
    }

    @Test
    @DisplayName("真实工作流条件表达式 - plan.yaml")
    void testRealWorldPlanConditions() throws Exception {
        // 模拟 plan.yaml 中的条件
        Map<String, Object> context = new HashMap<>();
        context.put("user_prompt", null);

        // !user_prompt（检查用户输入是否存在）
        assertTrue(ExpressionEvaluator.evaluate("!user_prompt", context));

        context.put("user_prompt", "create feature");
        assertFalse(ExpressionEvaluator.evaluate("!user_prompt", context));
    }

    @Test
    @DisplayName("连续比较操作")
    void testChainedComparisons() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("value", 5);

        // 3 < value && value < 10
        assertTrue(ExpressionEvaluator.evaluate("3 < value && value < 10", context));

        // value >= 1 && value <= 10
        assertTrue(ExpressionEvaluator.evaluate("value >= 1 && value <= 10", context));
    }

    @Test
    @DisplayName("空条件和null条件")
    void testEmptyAndNullConditions() throws Exception {
        Map<String, Object> context = new HashMap<>();

        // 空条件默认为 true
        assertTrue(ExpressionEvaluator.evaluate("", context));
        assertTrue(ExpressionEvaluator.evaluate((String) null, context));

        // 只有空格的条件
        assertTrue(ExpressionEvaluator.evaluate("   ", context));
    }

    @Test
    @DisplayName("操作符优先级")
    void testOperatorPrecedence() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("a", true);
        context.put("b", false);
        context.put("c", true);
        context.put("d", 5);

        // && 优先于 ||
        assertTrue(ExpressionEvaluator.evaluate("a || b && c", context));

        // ! 优先于 &&
        assertTrue(ExpressionEvaluator.evaluate("!a || c", context));

        // 比较操作符优先于布尔操作符
        assertTrue(ExpressionEvaluator.evaluate("d > 3 && a", context));
    }

    @Test
    @DisplayName("前后空格容忍")
    void testWhitespaceTolerance() throws Exception {
        Map<String, Object> context = Map.of("x", 5);

        // 各种空格情况都应该正常工作
        assertTrue(ExpressionEvaluator.evaluate("x==5", context));
        assertTrue(ExpressionEvaluator.evaluate("x == 5", context));
        assertTrue(ExpressionEvaluator.evaluate("x  ==  5", context));
        assertTrue(ExpressionEvaluator.evaluate("( x == 5 )", context));
        assertTrue(ExpressionEvaluator.evaluate("  x  >  3  ", context));
    }

    @Test
    @DisplayName("错误语法检测")
    void testInvalidSyntax() {
        Map<String, Object> context = Map.of("x", 5);

        // 不完整的表达式
        assertThrows(Exception.class, () -> ExpressionEvaluator.evaluate("x == ", context));
        assertThrows(Exception.class, () -> ExpressionEvaluator.evaluate("== 5", context));

        // 不匹配的括号
        assertThrows(Exception.class, () -> ExpressionEvaluator.evaluate("(x == 5", context));
        assertThrows(Exception.class, () -> ExpressionEvaluator.evaluate("x == 5)", context));

        // 无效的操作符
        assertThrows(Exception.class, () -> ExpressionEvaluator.evaluate("x === 5", context));
    }

    @Test
    @DisplayName("双重否定")
    void testDoubleNegation() throws Exception {
        Map<String, Object> context = Map.of("flag", false);

        // !!flag = flag
        assertTrue(ExpressionEvaluator.evaluate("!!flag", context));

        context.put("flag", true);
        assertTrue(ExpressionEvaluator.evaluate("!!flag", context));
    }

    @Test
    @DisplayName("混合类型比较")
    void testMixedTypeComparison() throws Exception {
        Map<String, Object> context = new HashMap<>();
        context.put("str_val", "42");
        context.put("num_val", 42);

        // 字符串数字与数字比较
        assertTrue(ExpressionEvaluator.evaluate("str_val == num_val", context));
    }

    @Test
    @DisplayName("长变量名")
    void testLongVariableNames() throws Exception {
        Map<String, Object> context = Map.of("very_long_variable_name", "test_value");

        assertTrue(ExpressionEvaluator.evaluate("very_long_variable_name == 'test_value'", context));
    }

    @Test
    @DisplayName("特殊字符在字符串中")
    void testSpecialCharactersInStrings() throws Exception {
        Map<String, Object> context = Map.of("path", "C:\\Users\\Test\\file.txt");

        assertTrue(ExpressionEvaluator.evaluate("path == 'C:\\\\Users\\\\Test\\\\file.txt'", context));
    }

    @Test
    @DisplayName("复杂真实场景")
    void testComplexRealWorldScenario() throws Exception {
        // 模拟复杂的真实工作流决策
        Map<String, Object> context = new HashMap<>();
        context.put("project_type", "existing");
        context.put("has_tests", true);
        context.put("test_coverage", 85);
        context.put("has_ci", true);
        context.put("team_size", 5);

        // 复杂条件：(project_type == 'existing' && has_tests && test_coverage >= 80) || (has_ci && team_size > 3)
        String complexCondition = "(project_type == 'existing' && has_tests && test_coverage >= 80) || (has_ci && team_size > 3)";
        assertTrue(ExpressionEvaluator.evaluate(complexCondition, context), "Complex scenario should pass");
    }
}
