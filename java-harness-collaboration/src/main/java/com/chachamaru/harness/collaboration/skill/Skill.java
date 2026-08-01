package com.chachamaru.harness.collaboration.skill;

import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.collaboration.skill.model.SkillResult;

/**
 * Interface for skills in the collaboration layer.
 *
 * <p>Skills are executable units that can perform specific tasks within the harness workflow.
 * They support both compile-time Java skills and runtime-loaded Markdown skills.</p>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public interface Skill {

    /**
     * Executes the skill with the given context.
     *
     * <p>This method is called when the skill needs to perform its task.
     * Implementations should process the context and return an appropriate result.</p>
     *
     * @param context the skill execution context
     * @return the result of skill execution
     * @throws SkillExecutionException if the skill fails to execute
     */
    SkillResult execute(SkillContext context) throws SkillExecutionException;

    /**
     * Returns the unique identifier for this skill.
     *
     * <p>The skill ID should be unique across all registered skills.</p>
     *
     * @return the skill identifier
     */
    String getId();

    /**
     * Returns the display name for this skill.
     *
     * @return the skill name
     */
    default String getName() {
        return getId();
    }

    /**
     * Returns the description of what this skill does.
     *
     * @return the skill description
     */
    default String getDescription() {
        return "Skill: " + getName();
    }

    /**
     * Returns the version of this skill.
     *
     * @return the skill version
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Checks if this skill can handle the given context.
     *
     * <p>Default implementation returns true. Override this to provide
     * conditional skill execution based on context inspection.</p>
     *
     * @param context the skill context to check
     * @return true if this skill can handle the context, false otherwise
     */
    default boolean canExecute(SkillContext context) {
        return true;
    }

    /**
     * Returns the priority of this skill.
     *
     * <p>Higher priority skills are executed first. Default priority is 0.</p>
     *
     * @return the priority value (higher values = higher priority)
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Returns the tags associated with this skill.
     *
     * <p>Tags can be used for skill filtering and categorization.</p>
     *
     * @return the skill tags
     */
    default java.util.Set<String> getTags() {
        return java.util.Set.of();
    }
}
