package com.chachamaru.harness.workflow.loader;

import java.util.Map;
import java.util.HashMap;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== Testing VariableResolver ===");
        testVariableResolver();
        
        System.out.println("\n=== Testing ExpressionEvaluator ===");
        testExpressionEvaluator();
        
        System.out.println("\n✅ All tests passed!");
    }
    
    private static void testVariableResolver() {
        String template = "Hello ${name}!";
        Map<String, Object> context = Map.of("name", "World");
        String result = VariableResolver.resolve(template, context);
        if (!result.equals("Hello World!")) {
            throw new RuntimeException("Test failed: expected 'Hello World!', got '" + result + "'");
        }
        System.out.println("✓ Simple variable replacement works");
        
        template = "${greeting} ${name}, count is ${count}";
        context = new HashMap<>();
        context.put("greeting", "Hello");
        context.put("name", "Alice");
        context.put("count", 42);
        result = VariableResolver.resolve(template, context);
        if (!result.equals("Hello Alice, count is 42")) {
            throw new RuntimeException("Test failed: got '" + result + "'");
        }
        System.out.println("✓ Multiple variables work");
    }
    
    private static void testExpressionEvaluator() {
        try {
            Map<String, Object> context;
            
            // Test boolean literals
            boolean result = ExpressionEvaluator.evaluate("true", context);
            if (result != true) throw new RuntimeException("true test failed");
            result = ExpressionEvaluator.evaluate("false", context);
            if (result != false) throw new RuntimeException("false test failed");
            System.out.println("✓ Boolean literals work");
            
            // Test string comparison
            context = Map.of("project_type", "new");
            result = ExpressionEvaluator.evaluate("project_type == 'new'", context);
            if (result != true) throw new RuntimeException("string equality failed");
            System.out.println("✓ String comparison works");
            
            // Test numeric comparison
            context = Map.of("count", 5);
            result = ExpressionEvaluator.evaluate("count > 3", context);
            if (result != true) throw new RuntimeException("numeric comparison failed");
            System.out.println("✓ Numeric comparison works");
            
            // Test logical AND
            context = new HashMap<>();
            context.put("a", true);
            context.put("b", true);
            result = ExpressionEvaluator.evaluate("a && b", context);
            if (result != true) throw new RuntimeException("AND test failed");
            System.out.println("✓ Logical AND works");
            
            // Test logical OR
            context.put("b", false);
            result = ExpressionEvaluator.evaluate("a || b", context);
            if (result != true) throw new RuntimeException("OR test failed");
            System.out.println("✓ Logical OR works");
            
            // Test logical NOT
            context = Map.of("flag", false);
            result = ExpressionEvaluator.evaluate("!flag", context);
            if (result != true) throw new RuntimeException("NOT test failed");
            System.out.println("✓ Logical NOT works");
            
            // Test complex expression
            context = new HashMap<>();
            context.put("project_type", "new");
            context.put("has_dependencies", false);
            result = ExpressionEvaluator.evaluate("project_type == 'new' && !has_dependencies", context);
            if (result != true) throw new RuntimeException("complex expression failed");
            System.out.println("✓ Complex expressions work");
            
            // Test parentheses
            context = new HashMap<>();
            context.put("a", true);
            context.put("b", false);
            context.put("c", true);
            result = ExpressionEvaluator.evaluate("(a || b) && c", context);
            if (result != true) throw new RuntimeException("parentheses test failed");
            System.out.println("✓ Parentheses work");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
