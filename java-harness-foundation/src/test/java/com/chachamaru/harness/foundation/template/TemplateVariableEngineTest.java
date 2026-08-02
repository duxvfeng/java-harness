package com.chachamaru.harness.foundation.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TemplateVariableEngine 单元测试
 *
 * @since 4.0.0
 */
class TemplateVariableEngineTest {

    private Template template;
    private Map<String, Object> context;

    @BeforeEach
    void setUp() {
        template = new Template("test", "test-template", "Test template", "1.0.0", "test");
        context = new HashMap<>();
    }

    @Test
    void testBasicVariableReplacement() {
        template.setContent("Hello {{PROJECT_NAME}}!");
        context.put("PROJECT_NAME", "TestProject");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("Hello TestProject!", result);
    }

    @Test
    void testVariableWithDefaultValue() {
        template.setContent("Hello {{PROJECT_NAME:World}}!");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("Hello World!", result);
    }

    @Test
    void testMultipleVariables() {
        template.setContent("{{AUTHOR}} created {{PROJECT_NAME}} on {{DATE}}");
        context.put("AUTHOR", "John");
        context.put("PROJECT_NAME", "MyApp");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertTrue(result.contains("John"));
        assertTrue(result.contains("MyApp"));
        assertTrue(result.contains(LocalDateTime.now().toLocalDate().toString()));
    }

    @Test
    void testBuiltinVariables() {
        template.setContent("Date: {{DATE}}, Time: {{TIME}}");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertTrue(result.contains("Date:"));
        assertTrue(result.contains("Time:"));
    }

    @Test
    void testConditionalIf() {
        template.setContent("{{#if DEBUG}}Debug mode{{/if}}");
        context.put("DEBUG", true);

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("Debug mode", result);
    }

    @Test
    void testConditionalIfFalse() {
        template.setContent("{{#if DEBUG}}Debug mode{{/if}}");
        context.put("DEBUG", false);

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("", result);
    }

    @Test
    void testConditionalUnless() {
        template.setContent("{{#unless PRODUCTION}}Development mode{{/unless}}");
        context.put("PRODUCTION", false);

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("Development mode", result);
    }

    @Test
    void testLoopOverList() {
        template.setContent("Items: {{#each ITEMS}}- {{this}} {{/each}}");
        context.put("ITEMS", Arrays.asList("apple", "banana", "cherry"));

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertTrue(result.contains("apple"));
        assertTrue(result.contains("banana"));
        assertTrue(result.contains("cherry"));
    }

    @Test
    void testLoopWithIndex() {
        template.setContent("{{#each ITEMS}}{{@index}}: {{this}} {{/each}}");
        context.put("ITEMS", Arrays.asList("a", "b"));

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertTrue(result.contains("0: a"));
        assertTrue(result.contains("1: b"));
    }

    @Test
    void testEmptyLoop() {
        template.setContent("{{#each ITEMS}}Item: {{this}}{{/each}}");
        context.put("ITEMS", Collections.emptyList());

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("", result);
    }

    @Test
    void testRequiredVariableValidation() {
        template.setContent("Hello {{PROJECT_NAME}}");

        TemplateVariable var = new TemplateVariable("PROJECT_NAME", "Project Name",
            TemplateVariable.VariableType.STRING, true);
        template.addVariable("PROJECT_NAME", var);

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);

        assertThrows(TemplateRegistryException.class, engine::render);
    }

    @Test
    void testRequiredVariableWithDefault() {
        template.setContent("Hello {{PROJECT_NAME}}");

        TemplateVariable var = new TemplateVariable("PROJECT_NAME", "Project Name",
            TemplateVariable.VariableType.STRING, true);
        var.setDefaultValue("DefaultProject");
        template.addVariable("PROJECT_NAME", var);

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("Hello DefaultProject", result);
    }

    @Test
    void testVariableValidation() {
        template.setContent("{{PROJECT_NAME}}");

        TemplateVariable var = new TemplateVariable("PROJECT_NAME", "Project Name",
            TemplateVariable.VariableType.STRING, true);
        var.setPattern("[a-zA-Z]+");
        template.addVariable("PROJECT_NAME", var);

        context.put("PROJECT_NAME", "ValidName");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("ValidName", result);
    }

    @Test
    void testVariableValidationFail() {
        template.setContent("{{PROJECT_NAME}}");

        TemplateVariable var = new TemplateVariable("PROJECT_NAME", "Project Name",
            TemplateVariable.VariableType.STRING, true);
        var.setPattern("[a-zA-Z]+");
        var.setDefaultValue("DefaultName");
        template.addVariable("PROJECT_NAME", var);

        context.put("PROJECT_NAME", "123Invalid");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        // Should sanitize to empty since pattern doesn't match
        // (sanitizeReplacement removes invalid chars)
        assertEquals("", result);
    }

    @Test
    void testGetUsedVariables() {
        template.setContent("{{PROJECT_NAME}} {{AUTHOR}} {{DATE}}");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        Set<String> usedVars = engine.getUsedVariables();

        assertTrue(usedVars.contains("PROJECT_NAME"));
        assertTrue(usedVars.contains("AUTHOR"));
        assertTrue(usedVars.contains("DATE"));
    }

    @Test
    void testSetVariable() {
        template.setContent("{{NEW_VAR}}");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        engine.setVariable("NEW_VAR", "test_value");

        String result = engine.render();
        assertEquals("test_value", result);
    }

    @Test
    void testSetVariables() {
        template.setContent("{{VAR1}} {{VAR2}}");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);

        Map<String, Object> newVars = new HashMap<>();
        newVars.put("VAR1", "value1");
        newVars.put("VAR2", "value2");

        engine.setVariables(newVars);
        String result = engine.render();

        assertEquals("value1 value2", result);
    }

    @Test
    void testPreview() {
        template.setContent("{{PROJECT_NAME}}");

        TemplateVariable var = new TemplateVariable("PROJECT_NAME", "Project Name",
            TemplateVariable.VariableType.STRING, true);
        template.addVariable("PROJECT_NAME", var);

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);

        // Preview should process variables but not validate required ones
        String result = engine.preview();

        // Since PROJECT_NAME is not in context, it returns empty string
        assertEquals("", result);
    }

    @Test
    void testEmptyTemplate() {
        template.setContent("");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertEquals("", result);
    }

    @Test
    void testNullTemplate() {
        TemplateVariableEngine engine = new TemplateVariableEngine(null, context);
        String result = engine.render();

        assertEquals("", result);
    }

    @Test
    void testSecurityTemplateInjection() {
        template.setContent("Safe: {{USER_INPUT}}");

        // Try to inject template syntax
        context.put("USER_INPUT", "Hello {{MALICIOUS}} World");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        // Should escape the template syntax
        assertFalse(result.contains("{{"));
        assertFalse(result.contains("}}"));
    }

    @Test
    void testSecurityXSSPrevention() {
        template.setContent("Output: {{USER_INPUT}}");

        // Try to inject HTML/JavaScript
        context.put("USER_INPUT", "<script>alert('XSS')</script>");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        // Should remove dangerous characters
        assertFalse(result.contains("<script>"));
        assertFalse(result.contains("</script>"));
        assertFalse(result.contains("<>"));
    }

    @Test
    void testSecurityInvalidVariableName() {
        template.setContent("Test content");

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);

        // Try to set variable with invalid name
        assertThrows(TemplateRegistryException.class, () -> {
            engine.setVariable("invalid-name", "value");
        });

        assertThrows(TemplateRegistryException.class, () -> {
            engine.setVariable("invalid name", "value");
        });

        assertThrows(TemplateRegistryException.class, () -> {
            engine.setVariable("", "value");
        });
    }

    @Test
    void testSecurityLengthLimit() {
        template.setContent("Output: {{LONG_VAR}}");

        // Create very long string
        StringBuilder longString = new StringBuilder();
        for (int i = 0; i < 15000; i++) {
            longString.append("a");
        }

        context.put("LONG_VAR", longString.toString());

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        // Should be truncated to 10000 characters
        assertTrue(result.length() <= 10020); // "Output: " + 10000 chars
    }

    @Test
    void testComplexTemplate() {
        template.setContent("# {{PROJECT_NAME}}\n" +
                "{{#if DEBUG}}Debug Build{{/if}}\n" +
                "{{#unless PRODUCTION}}Development{{/unless}}\n" +
                "Author: {{AUTHOR:Unknown}}\n" +
                "Date: {{DATE}}\n" +
                "Files:\n" +
                "{{#each FILES}}- {{this}}\n" +
                "{{/each}}");

        context.put("PROJECT_NAME", "TestProject");
        context.put("DEBUG", true);
        context.put("PRODUCTION", false);
        context.put("FILES", Arrays.asList("file1.java", "file2.java", "README.md"));

        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        String result = engine.render();

        assertTrue(result.contains("# TestProject"));
        assertTrue(result.contains("Debug Build"));
        assertTrue(result.contains("Development"));
        assertTrue(result.contains("Author:"));
        assertTrue(result.contains("Date:"));
        assertTrue(result.contains("file1.java"));
        assertTrue(result.contains("file2.java"));
        assertTrue(result.contains("README.md"));
    }

    @Test
    void testGetContext() {
        TemplateVariableEngine engine = new TemplateVariableEngine(template, context);
        Map<String, Object> retrievedContext = engine.getContext();

        // Should return unmodifiable map
        assertThrows(UnsupportedOperationException.class, () -> {
            retrievedContext.put("new_key", "value");
        });
    }
}