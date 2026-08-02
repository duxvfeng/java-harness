package com.chachamaru.harness.foundation.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 模板实体类 - 表示单个模板
 *
 * <p>模板包含以下核心信息：</p>
 * <ul>
 *   <li>模板ID和名称</li>
 *   <li>版本信息</li>
 *   <li>模板元数据</li>
 *   <li>变量定义</li>
 *   <li>模板内容</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class Template {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("version")
    private String version;

    @JsonProperty("category")
    private String category;

    @JsonProperty("metadata")
    private TemplateMetadata metadata;

    @JsonProperty("variables")
    private Map<String, TemplateVariable> variables;

    @JsonProperty("content")
    private String content;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 默认构造函数
     */
    public Template() {
        this.variables = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完整构造函数
     */
    public Template(String id, String name, String description, String version, String category) {
        this();
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.category = category;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public TemplateMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(TemplateMetadata metadata) {
        this.metadata = metadata;
    }

    public Map<String, TemplateVariable> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, TemplateVariable> variables) {
        this.variables = variables;
    }

    public void addVariable(String key, TemplateVariable variable) {
        this.variables.put(key, variable);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 更新时间戳
     */
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取模板的完整标识符（category/name）
     * 使用 name 字段的 slug 形式而不是原始 name
     */
    @JsonIgnore
    public String getFullName() {
        String slugName = name != null ? name.toLowerCase().replaceAll("[^a-z0-9-]+", "-") : id;
        return category + "/" + slugName;
    }

    /**
     * 获取模板的 slug 名称
     */
    @JsonIgnore
    public String getSlugName() {
        return name != null ? name.toLowerCase().replaceAll("[^a-z0-9-]+", "-") : id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Template template = (Template) o;

        return id != null ? id.equals(template.id) : template.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Template{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                ", category='" + category + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}