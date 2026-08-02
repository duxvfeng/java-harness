package com.chachamaru.harness.foundation.template;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * Frontmatter 元数据 - 解析文件头部的版本和模板信息
 *
 * <p>支持的 Frontmatter 字段：</p>
 * <ul>
 *   <li>_harness_version: Harness 版本要求</li>
 *   <li>_harness_template: 使用的模板名称和版本</li>
 *   <li>_created: 创建时间</li>
 *   <li>_modified: 修改时间</li>
 *   <li>_author: 作者信息</li>
 *   <li>_metadata: 其他自定义元数据</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class FrontmatterMetadata {

    @JsonProperty("_harness_version")
    private String harnessVersion;

    @JsonProperty("_harness_template")
    private TemplateReference templateReference;

    @JsonProperty("_created")
    private LocalDateTime created;

    @JsonProperty("_modified")
    private LocalDateTime modified;

    @JsonProperty("_author")
    private String author;

    @JsonProperty("_metadata")
    private Map<String, Object> customMetadata;

    /**
     * 模板引用 - 指向使用的模板
     */
    public static class TemplateReference {
        @JsonProperty("name")
        private String name;

        @JsonProperty("version")
        private String version;

        @JsonProperty("category")
        private String category;

        public TemplateReference() {
            this.category = "core";
        }

        public TemplateReference(String name, String version) {
            this();
            this.name = name;
            this.version = version;
        }

        public TemplateReference(String category, String name, String version) {
            this.category = category;
            this.name = name;
            this.version = version;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getFullName() {
            return category + "/" + name;
        }

        @Override
        public String toString() {
            return "TemplateReference{" +
                    "name='" + name + '\'' +
                    ", version='" + version + '\'' +
                    ", category='" + category + '\'' +
                    '}';
        }
    }

    /**
     * 默认构造函数
     */
    public FrontmatterMetadata() {
        this.customMetadata = new HashMap<>();
        this.created = LocalDateTime.now();
        this.modified = LocalDateTime.now();
    }

    /**
     * 完整构造函数
     */
    public FrontmatterMetadata(String harnessVersion, TemplateReference templateReference) {
        this();
        this.harnessVersion = harnessVersion;
        this.templateReference = templateReference;
    }

    // Getters and Setters

    public String getHarnessVersion() {
        return harnessVersion;
    }

    public void setHarnessVersion(String harnessVersion) {
        this.harnessVersion = harnessVersion;
    }

    public TemplateReference getTemplateReference() {
        return templateReference;
    }

    public void setTemplateReference(TemplateReference templateReference) {
        this.templateReference = templateReference;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Map<String, Object> getCustomMetadata() {
        return customMetadata;
    }

    public void setCustomMetadata(Map<String, Object> customMetadata) {
        this.customMetadata = customMetadata;
    }

    public void addCustomMetadata(String key, Object value) {
        this.customMetadata.put(key, value);
    }

    /**
     * 更新修改时间
     */
    public void updateModified() {
        this.modified = LocalDateTime.now();
    }

    /**
     * 检查是否包含模板信息
     */
    @JsonIgnore
    public boolean hasTemplateReference() {
        return templateReference != null &&
               templateReference.getName() != null &&
               !templateReference.getName().isEmpty();
    }

    /**
     * 检查是否包含版本信息
     */
    @JsonIgnore
    public boolean hasVersionInfo() {
        return harnessVersion != null && !harnessVersion.isEmpty();
    }

    @Override
    public String toString() {
        return "FrontmatterMetadata{" +
                "harnessVersion='" + harnessVersion + '\'' +
                ", templateReference=" + templateReference +
                ", created=" + created +
                ", modified=" + modified +
                ", author='" + author + '\'' +
                ", customMetadata=" + customMetadata.size() +
                '}';
    }
}