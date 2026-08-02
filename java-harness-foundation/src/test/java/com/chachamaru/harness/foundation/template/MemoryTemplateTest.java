package com.chachamaru.harness.foundation.template;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryTemplate 单元测试
 *
 * @since 4.0.0
 */
class MemoryTemplateTest {

    @Test
    void testCreateDecisionsTemplate() {
        MemoryTemplate template = MemoryTemplate.createDecisionsTemplate();

        assertNotNull(template);
        assertEquals("memory-decisions", template.getId());
        assertEquals("decisions", template.getName());
        assertEquals("memory", template.getCategory());
        assertEquals("1.0.0", template.getVersion());
        assertNotNull(template.getContent());
        assertTrue(template.getContent().contains("# 架构决策记录"));

        // 验证必需变量
        assertTrue(template.getVariables().containsKey("decision_number"));
        assertTrue(template.getVariables().containsKey("title"));
        assertTrue(template.getVariables().containsKey("date"));
        assertTrue(template.getVariables().containsKey("context"));
        assertTrue(template.getVariables().containsKey("decision"));
    }

    @Test
    void testCreatePatternsTemplate() {
        MemoryTemplate template = MemoryTemplate.createPatternsTemplate();

        assertNotNull(template);
        assertEquals("memory-patterns", template.getId());
        assertEquals("patterns", template.getName());
        assertEquals("memory", template.getCategory());
        assertEquals("1.0.0", template.getVersion());
        assertNotNull(template.getContent());
        assertTrue(template.getContent().contains("# 设计模式文档"));

        // 验证必需变量
        assertTrue(template.getVariables().containsKey("pattern_name"));
        assertTrue(template.getVariables().containsKey("category"));
        assertTrue(template.getVariables().containsKey("intent"));
        assertTrue(template.getVariables().containsKey("structure"));
    }

    @Test
    void testGetSupportedTypes() {
        String[] types = MemoryTemplate.getSupportedTypes();

        assertNotNull(types);
        assertEquals(2, types.length);
        assertTrue(java.util.Arrays.asList(types).contains("decisions"));
        assertTrue(java.util.Arrays.asList(types).contains("patterns"));
    }

    @Test
    void testCreateByType() {
        MemoryTemplate decisionsTemplate = MemoryTemplate.createByType("decisions");
        assertNotNull(decisionsTemplate);
        assertEquals("decisions", decisionsTemplate.getName());

        MemoryTemplate patternsTemplate = MemoryTemplate.createByType("patterns");
        assertNotNull(patternsTemplate);
        assertEquals("patterns", patternsTemplate.getName());
    }

    @Test
    void testCreateByTypeInvalid() {
        assertThrows(TemplateRegistryException.class, () -> {
            MemoryTemplate.createByType("invalid_type");
        });
    }

    @Test
    void testDecisionsTemplateVariables() {
        MemoryTemplate template = MemoryTemplate.createDecisionsTemplate();

        TemplateVariable decisionNumberVar = template.getVariables().get("decision_number");
        assertNotNull(decisionNumberVar);
        assertEquals(TemplateVariable.VariableType.NUMBER, decisionNumberVar.getType());
        assertTrue(decisionNumberVar.isRequired());

        TemplateVariable contextVar = template.getVariables().get("context");
        assertNotNull(contextVar);
        assertEquals(TemplateVariable.VariableType.STRING, contextVar.getType());
        assertTrue(contextVar.isRequired());
    }

    @Test
    void testPatternsTemplateVariables() {
        MemoryTemplate template = MemoryTemplate.createPatternsTemplate();

        TemplateVariable patternNameVar = template.getVariables().get("pattern_name");
        assertNotNull(patternNameVar);
        assertEquals(TemplateVariable.VariableType.STRING, patternNameVar.getType());
        assertTrue(patternNameVar.isRequired());

        TemplateVariable exampleCodeVar = template.getVariables().get("example_code");
        assertNotNull(exampleCodeVar);
        assertEquals(TemplateVariable.VariableType.STRING, exampleCodeVar.getType());
        assertFalse(exampleCodeVar.isRequired());
    }
}