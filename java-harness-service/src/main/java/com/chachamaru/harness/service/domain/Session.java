package com.chachamaru.harness.service.domain;

import java.time.LocalDateTime;

/**
 * Session domain model
 */
public class Session {
    private String id;
    private String projectRoot;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String metadata;

    public Session() {}

    public Session(String id, String projectRoot) {
        this.id = id;
        this.projectRoot = projectRoot;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectRoot() { return projectRoot; }
    public void setProjectRoot(String projectRoot) { this.projectRoot = projectRoot; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
