package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;

/**
 * ModelReferenceResolver 解析器的单元测试
 * 测试环境变量引用解析、直接模型名称处理、缺失和空环境变量处理等功能
 */
class ModelReferenceResolverTest {

    private ModelReferenceResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ModelReferenceResolver();
    }

    @AfterEach
    void tearDown() {
        // 环境变量在测试中无法真正修改，这里只是占位符
    }

    @Test
    void testResolveDirectModelName() {
        // 直接模型名称应该原样返回
        String result = resolver.resolve("glm-4.7");
        assertEquals("glm-4.7", result);
    }

    @Test
    void testResolveClaudeModelName() {
        // Claude 模型名称应该原样返回
        String result = resolver.resolve("claude-sonnet-4-20250514");
        assertEquals("claude-sonnet-4-20250514", result);
    }

    @Test
    void testResolveExistingEnvReference() {
        // 测试实际存在的环境变量
        // 这个测试依赖于环境中确实存在 PATH 或其他常见环境变量
        String result = resolver.resolve("env:PATH");
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void testResolveEnvReferenceWithMissingVariable() {
        Exception exception = assertThrows(ConfigException.class, () -> {
            resolver.resolve("env:NON_EXISTENT_VAR_HOPEFULLY_12345");
        });

        assertTrue(exception.getMessage().contains("NON_EXISTENT_VAR_HOPEFULLY_12345"));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void testResolveWithNullInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve(null);
        });

        assertTrue(exception.getMessage().contains("reference"));
    }

    @Test
    void testResolveWithEmptyInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve("");
        });

        assertTrue(exception.getMessage().contains("reference"));
    }

    @Test
    void testResolveWithWhitespaceInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolve("   ");
        });

        assertTrue(exception.getMessage().contains("reference"));
    }

    @Test
    void testIsEnvReference() {
        assertTrue(resolver.isEnvReference("env:MODEL_VAR"));
        assertFalse(resolver.isEnvReference("direct-model"));
        assertFalse(resolver.isEnvReference(""));
        assertFalse(resolver.isEnvReference(null));
    }

    @Test
    void testExtractEnvVariableName() {
        String varName = resolver.extractEnvVariableName("env:TEST_MODEL_VAR");
        assertEquals("TEST_MODEL_VAR", varName);
    }

    @Test
    void testExtractEnvVariableNameWithInvalidFormat() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            resolver.extractEnvVariableName("env:");
        });

        assertTrue(exception.getMessage().contains("env:"));
    }

    @Test
    void testResolveWithSpecialCharactersInModelName() {
        // 模型名称可以包含特殊字符
        assertEquals("claude-sonnet-4-20250514", resolver.resolve("claude-sonnet-4-20250514"));
        assertEquals("glm-4.7", resolver.resolve("glm-4.7"));
        assertEquals("gpt-4_turbo", resolver.resolve("gpt-4_turbo"));
    }

    @Test
    void testResolveAll() {
        String[] references = {
            "glm-4.7",
            "claude-sonnet-4-20250514",
            "env:PATH"
        };

        String[] resolved = resolver.resolveAll(references);

        assertEquals(3, resolved.length);
        assertEquals("glm-4.7", resolved[0]);
        assertEquals("claude-sonnet-4-20250514", resolved[1]);
        assertNotNull(resolved[2]); // PATH 应该存在
    }

    @Test
    void testResolveAllWithNullInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            resolver.resolveAll(null);
        });

        assertTrue(exception.getMessage().contains("array"));
    }

    @Test
    void testGetEnvPrefix() {
        assertEquals("env:", ModelReferenceResolver.getEnvPrefix());
    }

    @Test
    void testResolveEnvReferenceWithTrailingSpaces() {
        // 测试带前后空格的环境变量引用
        String result = resolver.resolve("  env:PATH  ");
        // 应该解析为环境变量值，而不是原样返回
        assertNotNull(result);
        assertFalse(result.trim().isEmpty());
    }

    @Test
    void testResolveDirectModelWithTrailingSpaces() {
        // 直接模型名称会去掉前后空格
        String result = resolver.resolve("  glm-4.7  ");
        assertEquals("glm-4.7", result);
    }
}