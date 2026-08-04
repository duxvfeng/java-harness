package com.chachamaru.harness.workflow.skill.core.model;

import java.nio.file.Path;
import java.util.Map;

/**
 * 项目上下文
 */
public class ProjectContext {
    private final Path projectRoot;
    private final String projectName;
    private final Map<String, Object> metadata;

    public ProjectContext(Path projectRoot, String projectName, Map<String, Object> metadata) {
        this.projectRoot = projectRoot;
        this.projectName = projectName;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public String getProjectName() {
        return projectName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}