package com.chachamaru.harness.foundation.template;

import java.util.Map;
import java.util.HashMap;

/**
 * 内存模板 - decisions.md 和 patterns.md 模板支持
 *
 * <p>支持记录架构决策和设计模式的内存模板：</p>
 * <ul>
 *   <li>decisions.md: 架构决策记录 (ADR)</li>
 *   <li>patterns.md: 设计模式文档</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class MemoryTemplate extends Template {

    private static final String DECISIONS_TEMPLATE = TemplateResourceLoader.load(
        "templates/memory/decisions-java.template");

    private static final String PATTERNS_TEMPLATE = TemplateResourceLoader.load(
        "templates/memory/patterns-java.template");

    public MemoryTemplate() {
        super();
    }

    /**
     * 创建 decisions.md 模板
     */
    public static MemoryTemplate createDecisionsTemplate() {
        MemoryTemplate template = new MemoryTemplate();
        template.setId("memory-decisions");
        template.setName("decisions");
        template.setCategory("memory");
        template.setVersion("1.0.0");
        template.setDescription("架构决策记录模板");
        template.setContent(DECISIONS_TEMPLATE);

        // 添加变量定义
        Map<String, TemplateVariable> variables = new HashMap<>();
        variables.put("decision_number", new TemplateVariable("decision_number",
            "决策编号", TemplateVariable.VariableType.NUMBER, true));
        variables.put("title", new TemplateVariable("title",
            "决策标题", TemplateVariable.VariableType.STRING, true));
        variables.put("date", new TemplateVariable("date",
            "决策日期", TemplateVariable.VariableType.DATE, true));
        variables.put("status", new TemplateVariable("status",
            "决策状态", TemplateVariable.VariableType.STRING, true));
        variables.put("author", new TemplateVariable("author",
            "决策者", TemplateVariable.VariableType.STRING, false));
        variables.put("context", new TemplateVariable("context",
            "决策上下文", TemplateVariable.VariableType.STRING, true));
        variables.put("decision", new TemplateVariable("decision",
            "决策内容", TemplateVariable.VariableType.STRING, true));
        variables.put("consequences", new TemplateVariable("consequences",
            "决策后果", TemplateVariable.VariableType.STRING, true));
        variables.put("related_decision_1", new TemplateVariable("related_decision_1",
            "相关决策1", TemplateVariable.VariableType.STRING, false));
        variables.put("related_decision_2", new TemplateVariable("related_decision_2",
            "相关决策2", TemplateVariable.VariableType.STRING, false));

        template.setVariables(variables);
        return template;
    }

    /**
     * 创建 patterns.md 模板
     */
    public static MemoryTemplate createPatternsTemplate() {
        MemoryTemplate template = new MemoryTemplate();
        template.setId("memory-patterns");
        template.setName("patterns");
        template.setCategory("memory");
        template.setVersion("1.0.0");
        template.setDescription("设计模式文档模板");
        template.setContent(PATTERNS_TEMPLATE);

        // 添加变量定义
        Map<String, TemplateVariable> variables = new HashMap<>();
        variables.put("pattern_name", new TemplateVariable("pattern_name",
            "模式名称", TemplateVariable.VariableType.STRING, true));
        variables.put("category", new TemplateVariable("category",
            "模式分类", TemplateVariable.VariableType.STRING, true));
        variables.put("intent", new TemplateVariable("intent",
            "模式意图", TemplateVariable.VariableType.STRING, true));
        variables.put("applicability", new TemplateVariable("applicability",
            "适用场景", TemplateVariable.VariableType.STRING, true));
        variables.put("structure", new TemplateVariable("structure",
            "模式结构", TemplateVariable.VariableType.STRING, true));
        variables.put("participant_1", new TemplateVariable("participant_1",
            "参与者1名称", TemplateVariable.VariableType.STRING, false));
        variables.put("role_1", new TemplateVariable("role_1",
            "参与者1角色", TemplateVariable.VariableType.STRING, false));
        variables.put("participant_2", new TemplateVariable("participant_2",
            "参与者2名称", TemplateVariable.VariableType.STRING, false));
        variables.put("role_2", new TemplateVariable("role_2",
            "参与者2角色", TemplateVariable.VariableType.STRING, false));
        variables.put("collaboration", new TemplateVariable("collaboration",
            "协作关系", TemplateVariable.VariableType.STRING, true));
        variables.put("consequences", new TemplateVariable("consequences",
            "模式效果", TemplateVariable.VariableType.STRING, true));
        variables.put("implementation_notes", new TemplateVariable("implementation_notes",
            "实现说明", TemplateVariable.VariableType.STRING, false));
        variables.put("example_code", new TemplateVariable("example_code",
            "示例代码", TemplateVariable.VariableType.STRING, false));
        variables.put("related_pattern_1", new TemplateVariable("related_pattern_1",
            "相关模式1", TemplateVariable.VariableType.STRING, false));
        variables.put("related_pattern_2", new TemplateVariable("related_pattern_2",
            "相关模式2", TemplateVariable.VariableType.STRING, false));

        template.setVariables(variables);
        return template;
    }

    /**
     * 获取所有内存模板类型
     */
    public static String[] getSupportedTypes() {
        return new String[]{"decisions", "patterns"};
    }

    /**
     * 根据类型创建内存模板
     */
    public static MemoryTemplate createByType(String type) {
        switch (type.toLowerCase()) {
            case "decisions":
                return createDecisionsTemplate();
            case "patterns":
                return createPatternsTemplate();
            default:
                throw new TemplateRegistryException(
                    TemplateRegistryException.ErrorCode.INVALID_TEMPLATE_DEFINITION,
                    "不支持的内存模板类型: " + type
                );
        }
    }
}
