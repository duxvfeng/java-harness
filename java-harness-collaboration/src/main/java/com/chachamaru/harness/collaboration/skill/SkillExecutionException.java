package com.chachamaru.harness.collaboration.skill;

/**
 * Exception thrown when a skill fails to execute.
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class SkillExecutionException extends Exception {

    private final String skillId;

    /**
     * Creates a new skill execution exception.
     *
     * @param skillId the skill ID that failed
     * @param message the error message
     */
    public SkillExecutionException(String skillId, String message) {
        super(message);
        this.skillId = skillId;
    }

    /**
     * Creates a new skill execution exception with a cause.
     *
     * @param skillId the skill ID that failed
     * @param message the error message
     * @param cause the underlying cause
     */
    public SkillExecutionException(String skillId, String message, Throwable cause) {
        super(message, cause);
        this.skillId = skillId;
    }

    /**
     * Returns the skill ID that failed.
     *
     * @return the skill ID
     */
    public String getSkillId() {
        return skillId;
    }
}
