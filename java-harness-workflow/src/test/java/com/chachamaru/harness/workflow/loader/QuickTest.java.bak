package com.chachamaru.harness.workflow.loader;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单的独立测试，验证 VariableResolver 和 ExpressionEvaluator 功能
 */
public class QuickTest {

    public static void main(String[] args) {
        System.out.println("=== VariableResolver 测试 ===");
        testVariableResolver();

        System.out.println("\n=== ExpressionEvaluator 测试 ===");
        testExpressionEvaluator();

        System.out.println("\n✅ 所有测试通过！");
    }

    private static void testVariableResolver() {
        // 测试1: 简单变量替换
        String template = "Hello ${name}!";
        Map<String, Object> context = Map.of("name", "World");
        String result = VariableResolver.resolve(template, context);
        assert result.equals("Hello World!") : "Expected 'Hello World!', got: " + result;
        System.out.println("✓ 简单变量替换: " + result);

        // 测试2: 多个变量
        template = "${greeting} ${name}, count is ${count}";
        context = new HashMap<>();
        context.put("greeting", "Hello");
        context.put("name", "Alice");
        context.put("count", 42);
        result = VariableResolver.resolve(template, context);
        assert result.equals("Hello Alice, count is 42") : "Expected 'Hello Alice, count is 42', got: " + result;
        System.out.println("✓ 多个变量: " + result);

        // 测试3: 未定义变量
        template = "Hello ${missing}!";
        context = Map.of();
        result = VariableResolver.resolve(template, context);
        assert result.equals("Hello ${missing}!") : "Expected 'Hello ${missing}!', got: " + result;
        System.out.println("✓ 未定义变量保持原样: " + result);
    }

    private static void testExpressionEvaluator() {
        Map<String, Object> context;

        try {
            // 测试1: 布尔字面值
            assert ExpressionEvaluator.evaluate("true", context) == true;
            assert ExpressionEvaluator.evaluate("false", context) == false;
            System.out.println("✓ 布尔字面值");

            // 测试2: 字符串相等
            context = Map.of("project_type", "new");
            boolean result = ExpressionEvaluator.evaluate("project_type == 'new'", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 字符串相等: project_type == 'new'");

            // 测试3: 数值比较
            context = Map.of("count", 5);
            result = ExpressionEvaluator.evaluate("count > 3", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 数值比较: count > 3");

            // 测试4: 逻辑与
            context = new HashMap<>();
            context.put("a", true);
            context.put("b", true);
            result = ExpressionEvaluator.evaluate("a && b", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 逻辑与: a && b");

            // 测试5: 逻辑或
            context.put("b", false);
            result = ExpressionEvaluator.evaluate("a || b", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 逻辑或: a || b");

            // 测试6: 逻辑非
            context = Map.of("flag", false);
            result = ExpressionEvaluator.evaluate("!flag", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 逻辑非: !flag");

            // 测试7: 复杂表达式
            context = new HashMap<>();
            context.put("project_type", "new");
            context.put("has_dependencies", false);
            result = ExpressionEvaluator.evaluate("project_type == 'new' && !has_dependencies", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 复杂表达式: project_type == 'new' && !has_dependencies");

            // 测试8: 括号
            context = new HashMap<>();
            context.put("a", true);
            context.put("b", false);
            context.put("c", true);
            result = ExpressionEvaluator.evaluate("(a || b) && c", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 括号分组: (a || b) && c");

            // 测试9: 数值大小等于
            context = Map.of("count", 5);
            assert ExpressionEvaluator.evaluate("count >= 5", context) == true;
            assert ExpressionEvaluator.evaluate("count <= 5", context) == true;
            assert ExpressionEvaluator.evaluate("count < 10", context) == true;
            assert ExpressionEvaluator.evaluate("count > 1", context) == true;
            System.out.println("✓ 数值大小等于比较");

            // 测试10: 真实工作流条件
            context = Map.of("project_type", "ambiguous");
            result = ExpressionEvaluator.evaluate("project_type == 'ambiguous'", context);
            assert result == true : "Expected true, got: " + result;
            System.out.println("✓ 真实工作流条件: project_type == 'ambiguous'");

        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
