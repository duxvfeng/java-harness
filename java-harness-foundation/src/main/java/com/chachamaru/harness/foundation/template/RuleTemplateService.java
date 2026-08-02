package com.chachamaru.harness.foundation.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 规则模板服务 - 管理规则模板的生成和配置
 *
 * <p>支持12个规则文件的模板生成：</p>
 * <ul>
 *   <li>基础规则模板</li>
 *   <li>安全规则模板</li>
 *   <li>架构规则模板</li>
 *   <li>性能规则模板</li>
 *   <li>迁移规则模板</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class RuleTemplateService {

    private static final Logger logger = LoggerFactory.getLogger(RuleTemplateService.class);

    private final TemplateService templateService;
    private final Map<String, RuleTemplateConfig> ruleConfigs;

    /**
     * 规则模板配置
     */
    public static class RuleTemplateConfig {
        private final String templateName;
        private final String category;
        private final String defaultVersion;
        private final List<String> requiredVariables;

        public RuleTemplateConfig(String templateName, String category, String defaultVersion) {
            this.templateName = templateName;
            this.category = category;
            this.defaultVersion = defaultVersion;
            this.requiredVariables = new ArrayList<>();
        }

        public String getTemplateName() { return templateName; }
        public String getCategory() { return category; }
        public String getDefaultVersion() { return defaultVersion; }
        public List<String> getRequiredVariables() { return requiredVariables; }

        public void addRequiredVariable(String var) {
            requiredVariables.add(var);
        }
    }

    /**
     * 构造函数
     */
    public RuleTemplateService(TemplateService templateService) {
        this.templateService = templateService;
        this.ruleConfigs = new HashMap<>();

        // 初始化规则配置
        initializeRuleConfigs();
    }

    /**
     * 初始化规则配置
     */
    private void initializeRuleConfigs() {
        // 规则模板配置
        RuleTemplateConfig ruleConfig = new RuleTemplateConfig("rule-md", "rules", "1.0.0");
        ruleConfig.addRequiredVariable("RULE_NAME");
        ruleConfig.addRequiredVariable("RULE_ID");
        ruleConfig.addRequiredVariable("RULE_VERSION");
        ruleConfigs.put("rule-md", ruleConfig);

        // 安全规则配置
        RuleTemplateConfig securityConfig = new RuleTemplateConfig("security-rule", "rules", "1.0.0");
        securityConfig.addRequiredVariable("SECURITY_RULE_NAME");
        securityConfig.addRequiredVariable("SECURITY_LEVEL");
        securityConfig.addRequiredVariable("THREAT_TYPE");
        ruleConfigs.put("security-rule", securityConfig);

        // Guardrail 规则配置
        RuleTemplateConfig guardrailConfig = new RuleTemplateConfig("guardrail-rule", "rules", "1.0.0");
        guardrailConfig.addRequiredVariable("RULE_NUMBER");
        guardrailConfig.addRequiredVariable("RULE_TITLE");
        guardrailConfig.addRequiredVariable("RULE_DESCRIPTION");
        ruleConfigs.put("guardrail-rule", guardrailConfig);
    }

    /**
     * 生成规则文件
     *
     * @param ruleTemplateType 规则模板类型
     * @param variables 变量上下文
     * @return 生成的规则内容
     */
    public String generateRule(String ruleTemplateType, Map<String, Object> variables) {
        // 验证必需变量
        validateRequiredVariables(ruleTemplateType, variables);

        // 创建变量副本以避免修改原始 Map
        Map<String, Object> workingVariables = new HashMap<>(variables);

        // 添加默认变量
        addDefaultVariables(workingVariables);

        // 生成内容
        return templateService.generateContent(ruleTemplateType, workingVariables);
    }

    /**
     * 生成规则文件
     *
     * @param ruleTemplateType 规则模板类型
     * @param variables 变量上下文
     * @param outputFile 输出文件路径
     */
    public void generateRuleFile(String ruleTemplateType, Map<String, Object> variables, Path outputFile) {
        String content = generateRule(ruleTemplateType, variables);

        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, content);
            logger.info("生成规则文件: {}", outputFile);
        } catch (IOException e) {
            logger.error("写入规则文件失败: {}", outputFile, e);
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_SAVE_FAILED,
                "写入规则文件失败: " + e.getMessage()
            );
        }
    }

    /**
     * 批量生成规则文件
     *
     * @param ruleConfigs 规则配置列表
     * @param outputDirectory 输出目录
     */
    public void generateRuleFiles(List<Map<String, Object>> ruleConfigs, Path outputDirectory) {
        for (Map<String, Object> ruleConfig : ruleConfigs) {
            String ruleType = (String) ruleConfig.get("rule_type");
            String fileName = (String) ruleConfig.get("file_name");

            if (ruleType != null && fileName != null) {
                Path outputFile = outputDirectory.resolve(fileName);
                generateRuleFile(ruleType, ruleConfig, outputFile);
            }
        }
    }

    /**
     * 验证必需变量
     */
    private void validateRequiredVariables(String ruleTemplateType, Map<String, Object> variables) {
        if (variables == null) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.VARIABLE_VALIDATION_FAILED,
                "变量上下文不能为空"
            );
        }

        RuleTemplateConfig config = ruleConfigs.get(ruleTemplateType);
        if (config != null) {
            List<String> missing = new ArrayList<>();
            for (String requiredVar : config.getRequiredVariables()) {
                if (!variables.containsKey(requiredVar)) {
                    missing.add(requiredVar);
                }
            }

            if (!missing.isEmpty()) {
                throw new TemplateRegistryException(
                    TemplateRegistryException.ErrorCode.VARIABLE_VALIDATION_FAILED,
                    "缺少必需变量: " + String.join(", ", missing)
                );
            }
        }
    }

    /**
     * 添加默认变量
     */
    private void addDefaultVariables(Map<String, Object> variables) {
        // 创建新的可变 Map 以避免不可变 Map 的问题
        Map<String, Object> mutableVars = new HashMap<>(variables);

        if (!mutableVars.containsKey("DATE")) {
            mutableVars.put("DATE", java.time.LocalDate.now().toString());
        }
        if (!mutableVars.containsKey("TIMESTAMP")) {
            mutableVars.put("TIMESTAMP", java.time.LocalDateTime.now().toString());
        }
        if (!mutableVars.containsKey("RULE_VERSION")) {
            mutableVars.put("RULE_VERSION", "1.0.0");
        }
        if (!mutableVars.containsKey("RULE_STATUS")) {
            mutableVars.put("RULE_STATUS", "active");
        }
        if (!mutableVars.containsKey("PROJECT_NAME")) {
            mutableVars.put("PROJECT_NAME", "RuleProject");
        }
        if (!mutableVars.containsKey("AUTHOR")) {
            mutableVars.put("AUTHOR", "Claude Code");
        }

        // 清空原始 Map 并添加所有变量
        variables.clear();
        variables.putAll(mutableVars);
    }

    /**
     * 获取规则模板配置
     */
    public RuleTemplateConfig getRuleConfig(String ruleTemplateType) {
        return ruleConfigs.get(ruleTemplateType);
    }

    /**
     * 列出所有可用的规则模板类型
     */
    public List<String> listRuleTemplates() {
        return new ArrayList<>(ruleConfigs.keySet());
    }

    /**
     * 获取规则模板的必需变量列表
     */
    public List<String> getRequiredVariables(String ruleTemplateType) {
        RuleTemplateConfig config = ruleConfigs.get(ruleTemplateType);
        return config != null ? new ArrayList<>(config.getRequiredVariables()) : new ArrayList<>();
    }

    /**
     * 创建示例规则配置
     */
    public Map<String, Object> createSampleRuleConfig(String ruleTemplateType) {
        Map<String, Object> config = new HashMap<>();

        switch (ruleTemplateType) {
            case "rule-md":
                config.put("RULE_NAME", "示例规则");
                config.put("RULE_ID", "RULE-001");
                config.put("RULE_VERSION", "1.0.0");
                config.put("RULE_STATUS", "active");
                config.put("RULE_CATEGORY", "安全");
                config.put("RULE_DESCRIPTION", "这是一个示例规则描述");
                config.put("TRIGGER_CONDITION_1", "条件1");
                config.put("TRIGGER_CONDITION_2", "条件2");
                break;

            case "security-rule":
                config.put("SECURITY_RULE_NAME", "数据保护规则");
                config.put("SECURITY_LEVEL", "高");
                config.put("THREAT_TYPE", "数据泄露");
                config.put("ATTACK_VECTOR", "外部攻击");
                break;

            case "guardrail-rule":
                config.put("RULE_NUMBER", "R01");
                config.put("RULE_TITLE", "阻止危险操作");
                config.put("RULE_DESCRIPTION", "阻止可能造成系统损害的操作");
                config.put("RULE_VERSION", "1.0.0");
                break;

            default:
                // 通用配置
                config.put("RULE_NAME", "示例规则");
                config.put("RULE_VERSION", "1.0.0");
        }

        return config;
    }

    /**
     * 验证规则模板
     */
    public boolean validateRuleTemplate(String ruleTemplateType) {
        try {
            Map<String, Object> sampleConfig = createSampleRuleConfig(ruleTemplateType);
            String content = generateRule(ruleTemplateType, sampleConfig);
            return content != null && !content.isEmpty();
        } catch (Exception e) {
            logger.warn("规则模板验证失败: {}", ruleTemplateType, e);
            return false;
        }
    }

    /**
     * 获取规则模板统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_rule_templates", ruleConfigs.size());
        stats.put("available_types", ruleConfigs.keySet());

        Map<String, Integer> variableCounts = new HashMap<>();
        for (RuleTemplateConfig config : ruleConfigs.values()) {
            variableCounts.put(config.getTemplateName(), config.getRequiredVariables().size());
        }
        stats.put("variable_counts", variableCounts);

        return stats;
    }
}