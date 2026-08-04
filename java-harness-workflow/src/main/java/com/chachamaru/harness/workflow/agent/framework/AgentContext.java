package com.chachamaru.harness.workflow.agent.framework;

import com.chachamaru.harness.workflow.skill.framework.SkillContext;
import com.chachamaru.harness.workflow.skill.framework.SkillFramework;
import com.chachamaru.harness.workflow.skill.framework.SkillResult;
import java.nio.file.Path;
import java.util.*;

/**
 * Agent 执行上下文
 * 扩展 SkillContext，添加 Agent 特有功能
 */
public class AgentContext {
    private final SkillContext skillContext;
    private final SkillFramework skillFramework;
    private final Map<String, Object> sharedState;
    private final List<AgentMessage> inbox;
    private final AgentConfig config;
    private final String taskId;

    private AgentContext(Builder builder) {
        this.skillContext = builder.skillContext;
        this.skillFramework = builder.skillFramework;
        this.sharedState = Collections.unmodifiableMap(builder.sharedState);
        this.inbox = Collections.unmodifiableList(builder.inbox);
        this.config = builder.config;
        this.taskId = builder.taskId;
    }

    // Delegate to SkillContext
    public String getUserIntent() {
        return skillContext.getUserIntent();
    }

    public Path getProjectRoot() {
        return skillContext.getProjectRoot();
    }

    public SkillContext.PermissionMode getPermissionMode() {
        return skillContext.getPermissionMode();
    }

    public Object getContextData(String key) {
        return skillContext.getContextData(key);
    }

    public Path getFile(String filePath) {
        return skillContext.getFile(filePath);
    }

    public Object getVariable(String varName) {
        return skillContext.getVariable(varName);
    }

    public int getFileCount() {
        return skillContext.getFileCount();
    }

    public int getVariableCount() {
        return skillContext.getVariableCount();
    }

    // Agent-specific methods
    public SkillFramework getSkillFramework() {
        return skillFramework;
    }

    public Object getSharedState(String key) {
        return sharedState.get(key);
    }

    public Map<String, Object> getSharedState() {
        return sharedState;
    }

    public List<AgentMessage> getInbox() {
        return inbox;
    }

    public AgentConfig getConfig() {
        return config;
    }

    public String getTaskId() {
        return taskId;
    }

    /**
     * 调用 Skill（便捷方法）
     */
    public SkillResult callSkill(String skillId) {
        return callSkill(skillId, skillContext);
    }

    /**
     * 调用 Skill（自定义上下文）
     */
    public SkillResult callSkill(String skillId, SkillContext skillContext) {
        try {
            return skillFramework.executeSkill(skillId, skillContext);
        } catch (com.chachamaru.harness.workflow.skill.framework.SkillExecutionException e) {
            return SkillResult.builder()
                    .skillId(skillId)
                    .status(SkillResult.SkillStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SkillContext skillContext;
        private SkillFramework skillFramework;
        private Map<String, Object> sharedState = new HashMap<>();
        private List<AgentMessage> inbox = new ArrayList<>();
        private AgentConfig config = AgentConfig.defaultConfig();
        private String taskId;

        public Builder skillContext(SkillContext skillContext) {
            this.skillContext = skillContext;
            return this;
        }

        public Builder skillFramework(SkillFramework skillFramework) {
            this.skillFramework = skillFramework;
            return this;
        }

        public Builder userIntent(String userIntent) {
            if (this.skillContext == null) {
                this.skillContext = SkillContext.builder().userIntent(userIntent).build();
            }
            return this;
        }

        public Builder projectRoot(Path projectRoot) {
            if (this.skillContext == null) {
                this.skillContext = SkillContext.builder().projectRoot(projectRoot).build();
            }
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder addSharedState(String key, Object value) {
            this.sharedState.put(key, value);
            return this;
        }

        public Builder sharedState(Map<String, Object> sharedState) {
            this.sharedState = new HashMap<>(sharedState);
            return this;
        }

        public Builder addMessage(AgentMessage message) {
            this.inbox.add(message);
            return this;
        }

        public Builder config(AgentConfig config) {
            this.config = config;
            return this;
        }

        public AgentContext build() {
            if (skillFramework == null) {
                throw new IllegalStateException("skillFramework is required");
            }
            if (skillContext == null) {
                throw new IllegalStateException("skillContext is required");
            }
            return new AgentContext(this);
        }
    }
}
