package com.chachamaru.harness.workflow.loader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VariableResolver 测试
 */
class VariableResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void testSimpleVariableReplacement() {
        String template = "Hello ${name}!";
        Map<String, Object> context = Map.of("name", "World");

        String result = VariableResolver.resolve(template, context);
        assertEquals("Hello World!", result);
    }

    @Test
    void testMultipleVariables() {
        String template = "${greeting} ${name}, today is ${day}";
        Map<String, Object> context = new HashMap<>();
        context.put("greeting", "Hello");
        context.put("name", "Alice");
        context.put("day", "Monday");

        String result = VariableResolver.resolve(template, context);
        assertEquals("Hello Alice, today is Monday", result);
    }

    @Test
    void testMissingVariable() {
        String template = "Hello ${name}!";
        Map<String, Object> context = Map.of(); // 空上下文

        String result = VariableResolver.resolve(template, context);
        // 未定义的变量应保持原样
        assertEquals("Hello ${name}!", result);
    }

    @Test
    void testNullTemplate() {
        String result = VariableResolver.resolve(null, Map.of("name", "World"));
        assertNull(result);
    }

    @Test
    void testEmptyTemplate() {
        String result = VariableResolver.resolve("", Map.of("name", "World"));
        assertEquals("", result);
    }

    @Test
    void testVariableWithNumber() {
        String template = "Count: ${count}";
        Map<String, Object> context = Map.of("count", 42);

        String result = VariableResolver.resolve(template, context);
        assertEquals("Count: 42", result);
    }

    @Test
    void testComplexTemplate() throws IOException {
        Path yamlFile = tempDir.resolve("workflow.yaml");
        String yamlContent = """
            phase: ${phase_name}
            description: "Workflow for ${project_name}"
            steps:
              - id: step-${step_number}
                skill: ${skill_name}
                input:
                  variables:
                    - ${var1}
                    - ${var2}
            """;

        Map<String, Object> context = new HashMap<>();
        context.put("phase_name", "test");
        context.put("project_name", "my-project");
        context.put("step_number", 1);
        context.put("skill_name", "test-skill");
        context.put("var1", "value1");
        context.put("var2", "value2");

        String result = VariableResolver.resolve(yamlContent, context);
        assertTrue(result.contains("phase: test"));
        assertTrue(result.contains("Workflow for my-project"));
        assertTrue(result.contains("step-1"));
        assertTrue(result.contains("test-skill"));
        assertTrue(result.contains("value1"));
        assertTrue(result.contains("value2"));
    }

    @Test
    void testSpecialCharactersInVariableValue() {
        String template = "Path: ${path}";
        Map<String, Object> context = Map.of("path", "C:\\Users\\Test\\file.txt");

        String result = VariableResolver.resolve(template, context);
        assertEquals("Path: C:\\Users\\Test\\file.txt", result);
    }

    @Test
    void testRepeatedVariable() {
        String template = "${name} loves ${name}";
        Map<String, Object> context = Map.of("name", "Bob");

        String result = VariableResolver.resolve(template, context);
        assertEquals("Bob loves Bob", result);
    }

    @Test
    void testNestedBraces() {
        String template = "${{outer}}";
        Map<String, Object> context = Map.of("outer", "value");

        String result = VariableResolver.resolve(template, context);
        // 不支持嵌套，应保持原样
        assertEquals("${{outer}}", result);
    }
}
