package com.chachamaru.harness.workflow.skill.framework;

/**
 * 技能执行异常
 */
public class SkillExecutionException extends Exception {

    private final String skillId;
    private final String executionId;

    public SkillExecutionException(String message) {
        super(message);
        this.skillId = null;
        this.executionId = null;
    }

    public SkillExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.skillId = null;
        this.executionId = null;
    }

    public SkillExecutionException(String skillId, String executionId, String message) {
        super(message);
        this.skillId = skillId;
        this.executionId = executionId;
    }

    public SkillExecutionException(String skillId, String executionId, String message, Throwable cause) {
        super(message, cause);
        this.skillId = skillId;
        this.executionId = executionId;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getExecutionId() {
        return executionId;
    }
}