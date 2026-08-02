package com.chachamaru.harness.foundation.template;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 条件模板生成器 - 基于配置条件生成不同的模板内容
 *
 * <p>支持的模板条件：</p>
 * <ul>
 *   <li>基于项目类型的条件生成</li>
 *   <li>基于配置选项的条件生成</li>
 *   <li>基于环境变量的条件生成</li>
 *   <li>多模板组合生成</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class ConditionalTemplateGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ConditionalTemplateGenerator.class);

    private final TemplateService templateService;

    /**
     * 模板条件配置
     */
    public static class TemplateCondition {
        private final String name;
        private final Map<String, Object> conditions;
        private final String templateName;

        public TemplateCondition(String name, String templateName) {
            this.name = name;
            this.templateName = templateName;
            this.conditions = new HashMap<>();
        }

        public void addCondition(String key, Object value) {
            conditions.put(key, value);
        }

        public boolean evaluate(Map<String, Object> context) {
            for (Map.Entry<String, Object> condition : conditions.entrySet()) {
                Object contextValue = context.get(condition.getKey());
                Object requiredValue = condition.getValue();

                if (!Objects.equals(contextValue, requiredValue)) {
                    return false;
                }
            }
            return true;
        }

        public String getName() { return name; }
        public String getTemplateName() { return templateName; }
    }

    public ConditionalTemplateGenerator(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 基于条件生成模板
     */
    public String generateConditional(List<TemplateCondition> conditions, Map<String, Object> context) {
        // 找到第一个满足条件的模板
        for (TemplateCondition condition : conditions) {
            if (condition.evaluate(context)) {
                logger.info("条件匹配: {}, 使用模板: {}", condition.getName(), condition.getTemplateName());
                return templateService.generateContent(condition.getTemplateName(), context);
            }
        }

        // 没有匹配的条件，使用默认模板
        logger.warn("没有匹配的条件，使用默认模板");
        return generateDefaultTemplate(context);
    }

    /**
     * 生成默认模板
     */
    private String generateDefaultTemplate(Map<String, Object> context) {
        return templateService.generateContent("claude-md", context);
    }

    /**
     * 创建项目类型条件
     */
    public TemplateCondition createProjectTypeCondition(String projectType) {
        TemplateCondition condition = new TemplateCondition("project_type_" + projectType, getTemplateForProjectType(projectType));
        condition.addCondition("PROJECT_TYPE", projectType);
        return condition;
    }

    /**
     * 根据项目类型获取模板
     */
    private String getTemplateForProjectType(String projectType) {
        return switch (projectType.toLowerCase()) {
            case "web", "frontend" -> "web-project-template";
            case "api", "backend" -> "api-project-template";
            case "mobile" -> "mobile-project-template";
            case "library", "sdk" -> "library-project-template";
            default -> "default-project-template";
        };
    }

    /**
     * 创建框架条件
     */
    public TemplateCondition createFrameworkCondition(String framework) {
        TemplateCondition condition = new TemplateCondition("framework_" + framework, getTemplateForFramework(framework));
        condition.addCondition("FRAMEWORK", framework);
        return condition;
    }

    /**
     * 根据框架获取模板
     */
    private String getTemplateForFramework(String framework) {
        return switch (framework.toLowerCase()) {
            case "spring" -> "spring-project-template";
            case "react" -> "react-project-template";
            case "vue" -> "vue-project-template";
            case "angular" -> "angular-project-template";
            default -> "default-project-template";
        };
    }

    /**
     * 组合多个条件生成模板
     */
    public String generateCombined(List<TemplateCondition> conditions, Map<String, Object> context) {
        StringBuilder result = new StringBuilder();

        // 评估每个条件并生成对应内容
        for (TemplateCondition condition : conditions) {
            if (condition.evaluate(context)) {
                String content = templateService.generateContent(condition.getTemplateName(), context);
                result.append(content).append("\n\n");
            }
        }

        return result.toString();
    }

    /**
     * 基于配置生成条件模板
     */
    public String generateFromConfig(Map<String, Object> config) {
        List<TemplateCondition> conditions = new ArrayList<>();

        // 从配置中提取条件
        String projectType = (String) config.get("PROJECT_TYPE");
        if (projectType != null) {
            conditions.add(createProjectTypeCondition(projectType));
        }

        String framework = (String) config.get("FRAMEWORK");
        if (framework != null) {
            conditions.add(createFrameworkCondition(framework));
        }

        // 添加自定义条件
        Map<String, String> customConditions = (Map<String, String>) config.get("custom_conditions");
        if (customConditions != null) {
            for (Map.Entry<String, String> entry : customConditions.entrySet()) {
                TemplateCondition customCond = new TemplateCondition("custom_" + entry.getKey(), entry.getValue());
                customCond.addCondition(entry.getKey(), config.get(entry.getKey()));
                conditions.add(customCond);
            }
        }

        return generateConditional(conditions, config);
    }

    /**
     * 生成条件模板预览
     */
    public String previewConditional(List<TemplateCondition> conditions, Map<String, Object> context) {
        StringBuilder preview = new StringBuilder();
        preview.append("# 条件模板预览\n\n");

        for (TemplateCondition condition : conditions) {
            boolean matches = condition.evaluate(context);
            preview.append("- **").append(condition.getName()).append("**: ");
            preview.append(matches ? "✅ 匹配" : "❌ 不匹配");
            preview.append(" (模板: ").append(condition.getTemplateName()).append(")\n");
        }

        return preview.toString();
    }

    /**
     * 验证条件配置
     */
    public boolean validateConditions(List<TemplateCondition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return false;
        }

        for (TemplateCondition condition : conditions) {
            if (condition.getTemplateName() == null || condition.getTemplateName().isEmpty()) {
                return false;
            }
        }

        return true;
    }
}