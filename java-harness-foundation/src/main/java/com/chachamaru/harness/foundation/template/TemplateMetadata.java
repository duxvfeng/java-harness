package com.chachamaru.harness.foundation.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import java.util.HashMap;

/**
 * 模板元数据 - 存储模板的扩展信息
 *
 * <p>元数据包含：</p>
 * <ul>
 *   <li>作者信息</li>
 *   <li>依赖关系</li>
 *   <li>标签和分类</li>
 *   <li>自定义属性</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class TemplateMetadata {

    @JsonProperty("author")
    private String author;

    @JsonProperty("license")
    private String license;

    @JsonProperty("homepage")
    private String homepage;

    @JsonProperty("repository")
    private String repository;

    @JsonProperty("dependencies")
    private Map<String, String> dependencies;

    @JsonProperty("tags")
    private Map<String, String> tags;

    @JsonProperty("custom")
    private Map<String, Object> customAttributes;

    /**
     * 默认构造函数
     */
    public TemplateMetadata() {
        this.dependencies = new HashMap<>();
        this.tags = new HashMap<>();
        this.customAttributes = new HashMap<>();
    }

    /**
     * 完整构造函数
     */
    public TemplateMetadata(String author, String license) {
        this();
        this.author = author;
        this.license = license;
    }

    // Getters and Setters

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Map<String, String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<String, String> dependencies) {
        this.dependencies = dependencies;
    }

    public void addDependency(String name, String version) {
        this.dependencies.put(name, version);
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public void addTag(String key, String value) {
        this.tags.put(key, value);
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

    public void addCustomAttribute(String key, Object value) {
        this.customAttributes.put(key, value);
    }

    @Override
    public String toString() {
        return "TemplateMetadata{" +
                "author='" + author + '\'' +
                ", license='" + license + '\'' +
                ", homepage='" + homepage + '\'' +
                ", repository='" + repository + '\'' +
                ", dependencies=" + dependencies.size() +
                ", tags=" + tags.size() +
                ", customAttributes=" + customAttributes.size() +
                '}';
    }
}