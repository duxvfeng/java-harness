package com.chachamaru.harness.foundation.template;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 模板变量定义 - 描述模板中的可替换变量
 *
 * <p>变量属性包括：</p>
 * <ul>
 *   <li>变量名和描述</li>
 *   <li>数据类型</li>
 *   <li>默认值</li>
 *   <li>是否必填</li>
 *   <li>验证规则</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class TemplateVariable {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("type")
    private VariableType type;

    @JsonProperty("default_value")
    private String defaultValue;

    @JsonProperty("required")
    private boolean required;

    @JsonProperty("pattern")
    private String pattern;

    @JsonProperty("example")
    private String example;

    /**
     * 变量类型枚举
     */
    public enum VariableType {
        STRING,
        NUMBER,
        BOOLEAN,
        DATE,
        PATH,
        URL,
        EMAIL,
        JSON
    }

    /**
     * 默认构造函数
     */
    public TemplateVariable() {
        this.type = VariableType.STRING;
        this.required = false;
    }

    /**
     * 完整构造函数
     */
    public TemplateVariable(String name, String description, VariableType type, boolean required) {
        this();
        this.name = name;
        this.description = description;
        this.type = type;
        this.required = required;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public VariableType getType() {
        return type;
    }

    public void setType(VariableType type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    /**
     * 验证变量值是否符合要求
     */
    public boolean validate(String value) {
        if (required && (value == null || value.isEmpty())) {
            return false;
        }

        if (pattern != null && !pattern.isEmpty() && value != null) {
            return value.matches(pattern);
        }

        return true;
    }

    @Override
    public String toString() {
        return "TemplateVariable{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", required=" + required +
                '}';
    }
}