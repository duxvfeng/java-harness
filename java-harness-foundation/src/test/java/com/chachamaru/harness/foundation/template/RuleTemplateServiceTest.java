package com.chachamaru.harness.foundation.template;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleTemplateService 单元测试
 *
 * @since 4.0.0
 */
class RuleTemplateServiceTest {

    private RuleTemplateService ruleTemplateService;
    private TemplateService templateService;
    private TemplateRegistry registry;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        String registryPath = tempDir.resolve("template-registry.json").toString();
        String templatesDir = tempDir.resolve("templates").toString();
        registry = new TemplateRegistry(registryPath, templatesDir);
        templateService = new TemplateService(registry);
        ruleTemplateService = new RuleTemplateService(templateService);
    }

    @Test
    void testGenerateRule() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("RULE_NAME", "测试规则");
        variables.put("RULE_ID", "RULE-001");
        variables.put("RULE_VERSION", "1.0.0");
        variables.put("RULE_STATUS", "active");
        variables.put("RULE_CATEGORY", "安全");
        variables.put("RULE_DESCRIPTION", "测试规则描述");

        String content = ruleTemplateService.generateRule("rule-md", variables);

        assertNotNull(content);
        assertTrue(content.contains("测试规则"));
        assertTrue(content.contains("RULE-001"));
        assertTrue(content.contains("安全"));
    }

    @Test
    void testGenerateSecurityRule() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("SECURITY_RULE_NAME", "数据保护规则");
        variables.put("SECURITY_LEVEL", "高");
        variables.put("THREAT_TYPE", "数据泄露");

        String content = ruleTemplateService.generateRule("security-rule", variables);

        assertNotNull(content);
        assertTrue(content.contains("数据保护规则"));
        assertTrue(content.contains("高"));
        assertTrue(content.contains("数据泄露"));
    }

    @Test
    void testGenerateGuardrailRule() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("RULE_NUMBER", "R01");
        variables.put("RULE_TITLE", "阻止危险操作");
        variables.put("RULE_DESCRIPTION", "阻止可能造成系统损害的操作");

        String content = ruleTemplateService.generateRule("guardrail-rule", variables);

        assertNotNull(content);
        assertTrue(content.contains("R01"));
        assertTrue(content.contains("阻止危险操作"));
    }

    @Test
    void testInvalidRuleTemplateType() {
        Map<String, Object> variables = new HashMap<>();

        TemplateRegistryException exception = assertThrows(TemplateRegistryException.class, () -> {
            ruleTemplateService.generateRule("non-existent-type", variables);
        });

        assertTrue(exception.getMessage().contains("未知的规则模板类型"));
        assertTrue(exception.getMessage().contains("non-existent-type"));
    }

    @Test
    void testGenerateRuleFile() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("RULE_NAME", "文件测试规则");
        variables.put("RULE_ID", "RULE-FILE");
        variables.put("RULE_VERSION", "1.0.0");
        variables.put("RULE_STATUS", "active");
        variables.put("RULE_CATEGORY", "文件");
        variables.put("RULE_DESCRIPTION", "文件测试描述");

        Path outputFile = tempDir.resolve("rules").resolve("test-rule.md");

        ruleTemplateService.generateRuleFile("rule-md", variables, outputFile);

        assertTrue(Files.exists(outputFile));
        String content = Files.readString(outputFile);
        assertTrue(content.contains("文件测试规则"));
    }

    @Test
    void testValidateRequiredVariables() {
        Map<String, Object> variables = new HashMap<>();
        // 缺少必需变量

        assertThrows(TemplateRegistryException.class, () -> {
            ruleTemplateService.generateRule("rule-md", variables);
        });
    }

    @Test
    void testListRuleTemplates() {
        List<String> templates = ruleTemplateService.listRuleTemplates();

        assertNotNull(templates);
        assertFalse(templates.isEmpty());
        assertTrue(templates.contains("rule-md"));
        assertTrue(templates.contains("security-rule"));
        assertTrue(templates.contains("guardrail-rule"));
    }

    @Test
    void testGetRequiredVariables() {
        List<String> requiredVars = ruleTemplateService.getRequiredVariables("rule-md");

        assertNotNull(requiredVars);
        assertTrue(requiredVars.contains("RULE_NAME"));
        assertTrue(requiredVars.contains("RULE_ID"));
        assertTrue(requiredVars.contains("RULE_VERSION"));
    }

    @Test
    void testCreateSampleRuleConfig() {
        Map<String, Object> sampleConfig = ruleTemplateService.createSampleRuleConfig("rule-md");

        assertNotNull(sampleConfig);
        assertTrue(sampleConfig.containsKey("RULE_NAME"));
        assertTrue(sampleConfig.containsKey("RULE_ID"));
        assertEquals("示例规则", sampleConfig.get("RULE_NAME"));
    }

    @Test
    void testCreateSampleSecurityConfig() {
        Map<String, Object> sampleConfig = ruleTemplateService.createSampleRuleConfig("security-rule");

        assertNotNull(sampleConfig);
        assertTrue(sampleConfig.containsKey("SECURITY_RULE_NAME"));
        assertTrue(sampleConfig.containsKey("SECURITY_LEVEL"));
        assertEquals("数据保护规则", sampleConfig.get("SECURITY_RULE_NAME"));
    }

    @Test
    void testValidateRuleTemplate() {
        assertTrue(ruleTemplateService.validateRuleTemplate("rule-md"));
        assertTrue(ruleTemplateService.validateRuleTemplate("security-rule"));
        assertTrue(ruleTemplateService.validateRuleTemplate("guardrail-rule"));
    }

    @Test
    void testGetRuleConfig() {
        RuleTemplateService.RuleTemplateConfig config = ruleTemplateService.getRuleConfig("rule-md");

        assertNotNull(config);
        assertEquals("rule-md", config.getTemplateName());
        assertEquals("rules", config.getCategory());
        assertEquals("1.0.0", config.getDefaultVersion());
    }

    @Test
    void testGetStatistics() {
        Map<String, Object> stats = ruleTemplateService.getStatistics();

        assertNotNull(stats);
        assertTrue(stats.containsKey("total_rule_templates"));
        assertTrue(stats.containsKey("available_types"));
        assertTrue(stats.containsKey("variable_counts"));

        assertTrue((Integer) stats.get("total_rule_templates") >= 3);
    }

    @Test
    void testAddDefaultVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("RULE_NAME", "默认变量测试");
        variables.put("RULE_ID", "RULE-DEFAULT");
        variables.put("RULE_VERSION", "1.0.0");
        variables.put("RULE_STATUS", "active");
        variables.put("RULE_CATEGORY", "测试");
        variables.put("RULE_DESCRIPTION", "测试");

        String content = ruleTemplateService.generateRule("rule-md", variables);

        assertNotNull(content);
        // 应该包含默认添加的 DATE 变量
        assertTrue(content.contains("20")); // 年份
    }

    @Test
    void testBatchGenerateRuleFiles() throws Exception {
        List<Map<String, Object>> ruleConfigs = List.of(
            Map.of(
                "rule_type", "rule-md",
                "file_name", "rule1.md",
                "RULE_NAME", "批量规则1",
                "RULE_ID", "RULE-BATCH-1",
                "RULE_VERSION", "1.0.0",
                "RULE_STATUS", "active",
                "RULE_CATEGORY", "批量",
                "RULE_DESCRIPTION", "批量生成测试1"
            ),
            Map.of(
                "rule_type", "rule-md",
                "file_name", "rule2.md",
                "RULE_NAME", "批量规则2",
                "RULE_ID", "RULE-BATCH-2",
                "RULE_VERSION", "1.0.0",
                "RULE_STATUS", "active",
                "RULE_CATEGORY", "批量",
                "RULE_DESCRIPTION", "批量生成测试2"
            )
        );

        Path outputDir = tempDir.resolve("batch-rules");

        ruleTemplateService.generateRuleFiles(ruleConfigs, outputDir);

        assertTrue(Files.exists(outputDir.resolve("rule1.md")));
        assertTrue(Files.exists(outputDir.resolve("rule2.md")));

        String content1 = Files.readString(outputDir.resolve("rule1.md"));
        String content2 = Files.readString(outputDir.resolve("rule2.md"));

        assertTrue(content1.contains("批量规则1"));
        assertTrue(content2.contains("批量规则2"));
    }

    @Test
    void testNullVariablesValidation() {
        assertThrows(TemplateRegistryException.class, () -> {
            ruleTemplateService.generateRule("rule-md", null);
        });
    }

    @Test
    void testRuleConfigRequiredVariables() {
        RuleTemplateService.RuleTemplateConfig config = ruleTemplateService.getRuleConfig("rule-md");

        assertNotNull(config);
        assertFalse(config.getRequiredVariables().isEmpty());
        assertTrue(config.getRequiredVariables().contains("RULE_NAME"));
    }

    @Test
    void testSecurityRuleWithAllRequiredVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("SECURITY_RULE_NAME", "完整安全规则");
        variables.put("SECURITY_LEVEL", "中");
        variables.put("THREAT_TYPE", "注入攻击");

        String content = ruleTemplateService.generateRule("security-rule", variables);

        assertNotNull(content);
        assertTrue(content.contains("完整安全规则"));
        assertTrue(content.contains("中"));
        assertTrue(content.contains("注入攻击"));
        assertTrue(content.contains("安全等级"));
    }

    @Test
    void testGenerateRuleInSubdirectory() throws Exception {
        Map<String, Object> variables = new HashMap<>();
        variables.put("RULE_NAME", "子目录规则");
        variables.put("RULE_ID", "RULE-SUBDIR");
        variables.put("RULE_VERSION", "1.0.0");
        variables.put("RULE_STATUS", "active");
        variables.put("RULE_CATEGORY", "测试");
        variables.put("RULE_DESCRIPTION", "子目录测试");

        Path outputFile = tempDir.resolve("level1").resolve("level2").resolve("rule.md");

        ruleTemplateService.generateRuleFile("rule-md", variables, outputFile);

        assertTrue(Files.exists(outputFile));
        assertTrue(Files.exists(outputFile.getParent()));
    }
}