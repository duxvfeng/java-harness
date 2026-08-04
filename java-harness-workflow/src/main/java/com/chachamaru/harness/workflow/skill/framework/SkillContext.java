package com.chachamaru.harness.workflow.skill.framework;

import java.nio.file.Path;
import java.util.*;

/**
 * 技能执行上下文
 * 包含执行技能所需的所有信息
 */
public class SkillContext {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SkillContext.class);

    private final String userIntent;
    private final Path projectRoot;
    private final PermissionMode permissionMode;
    private final Map<String, Object> contextData;
    private final Map<String, Path> files;
    private final Map<String, Object> variables;

    private SkillContext(Builder builder) {
        this.userIntent = builder.userIntent;
        this.projectRoot = builder.projectRoot;
        this.permissionMode = builder.permissionMode;
        this.contextData = Collections.unmodifiableMap(new HashMap<>(builder.contextData));
        this.files = Collections.unmodifiableMap(new HashMap<>(builder.files));
        this.variables = Collections.unmodifiableMap(new HashMap<>(builder.variables));
    }

    public String getUserIntent() {
        return userIntent;
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public PermissionMode getPermissionMode() {
        return permissionMode;
    }

    public Object getContextData(String key) {
        return contextData.get(key);
    }

    public Set<String> getContextDataKeys() {
        return contextData.keySet();
    }

    public Path getFile(String filePath) {
        return files.get(filePath);
    }

    public Set<String> getFileKeys() {
        return files.keySet();
    }

    public Object getVariable(String varName) {
        return variables.get(varName);
    }

    public Set<String> getVariableNames() {
        return variables.keySet();
    }

    public int getFileCount() {
        return files.size();
    }

    public int getVariableCount() {
        return variables.size();
    }

    public int getContextDataCount() {
        return contextData.size();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for SkillContext
     */
    public static class Builder {
        private String userIntent;
        private Path projectRoot;
        private PermissionMode permissionMode = PermissionMode.DEFAULT;
        private Map<String, Object> contextData = new HashMap<>();
        private Map<String, Path> files = new HashMap<>();
        private Map<String, Object> variables = new HashMap<>();

        public Builder userIntent(String userIntent) {
            this.userIntent = userIntent;
            return this;
        }

        public Builder projectRoot(Path projectRoot) {
            this.projectRoot = projectRoot;
            return this;
        }

        public Builder permissionMode(PermissionMode permissionMode) {
            this.permissionMode = permissionMode;
            return this;
        }

        public Builder addContextData(String key, Object value) {
            this.contextData.put(key, value);
            return this;
        }

        public Builder addAllContextData(Map<String, Object> data) {
            this.contextData.putAll(data);
            return this;
        }

        public Builder addFile(String key, Path filePath) {
            this.files.put(key, filePath);
            return this;
        }

        public Builder addAllFiles(Map<String, Path> files) {
            this.files.putAll(files);
            return this;
        }

        public Builder addVariable(String varName, Object value) {
            this.variables.put(varName, value);
            return this;
        }

        public Builder addAllVariables(Map<String, Object> variables) {
            this.variables.putAll(variables);
            return this;
        }

        public SkillContext build() {
            if (userIntent == null || userIntent.isEmpty()) {
                throw new IllegalStateException("userIntent is required");
            }
            return new SkillContext(this);
        }
    }

    /**
     * 权限模式枚举
     */
    public enum PermissionMode {
        DEFAULT,    // 默认权限模式
        RESTRICTED, // 受限权限模式
        PERMISSIVE  // 宽松权限模式
    }

    @Override
    public String toString() {
        return "SkillContext{" +
                "userIntent='" + userIntent + '\'' +
                ", projectRoot=" + projectRoot +
                ", permissionMode=" + permissionMode +
                ", contextDataCount=" + contextData.size() +
                ", fileCount=" + files.size() +
                ", variableCount=" + variables.size() +
                '}';
    }
}