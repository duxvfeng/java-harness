package com.chachamaru.harness.foundation.template;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

/**
 * 模板版本信息 - 跟踪模板的版本历史
 *
 * @since 4.0.0
 */
public class TemplateVersion {

    @JsonProperty("template_id")
    private String templateId;

    @JsonProperty("version")
    private String version;

    @JsonProperty("description")
    private String description;

    @JsonProperty("compatibility")
    private String compatibility;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("checksum")
    private String checksum;

    @JsonProperty("changes")
    private Map<String, String> changes;

    public TemplateVersion() {
        this.changes = new HashMap<>();
        this.createdAt = LocalDateTime.now();
    }

    public TemplateVersion(String templateId, String version, String description) {
        this();
        this.templateId = templateId;
        this.version = version;
        this.description = description;
    }

    // Getters and Setters
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCompatibility() { return compatibility; }
    public void setCompatibility(String compatibility) { this.compatibility = compatibility; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public Map<String, String> getChanges() { return changes; }
    public void setChanges(Map<String, String> changes) { this.changes = changes; }

    public void addChange(String type, String description) {
        this.changes.put(type, description);
    }
}