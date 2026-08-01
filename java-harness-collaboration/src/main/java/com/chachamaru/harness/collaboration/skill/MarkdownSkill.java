package com.chachamaru.harness.collaboration.skill.loader;

import com.chachamaru.harness.collaboration.skill.CoreSkill;
import com.chachamaru.harness.collaboration.skill.SkillExecutionException;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Skill loaded from Markdown files.
 *
 * <p>MarkdownSkill represents skills that are loaded from .SKILL.md files
 * at runtime, allowing for dynamic skill definition without code changes.</p>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class MarkdownSkill extends CoreSkill {

    private static final Logger logger = LoggerFactory.getLogger(MarkdownSkill.class);

    private final String id;
    private final String name;
    private final String description;
    private final String version;
    private final Map<String, Object> metadata;
    private final String implementation;

    /**
     * Creates a MarkdownSkill.
     *
     * @param id the skill ID
     * @param name the skill name
     * @param description the skill description
     * @param version the skill version
     * @param metadata additional metadata
     * @param implementation the implementation script or reference
     */
    public MarkdownSkill(
            String id,
            String name,
            String description,
            String version,
            Map<String, Object> metadata,
            String implementation) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version != null ? version : "1.0.0";
        this.metadata = metadata;
        this.implementation = implementation;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    protected Object doExecute(SkillContext context) throws SkillExecutionException {
        logger.info("Executing MarkdownSkill: {}", id);

        // Placeholder: In real implementation, this would:
        // 1. Parse the implementation field
        // 2. Execute the script or delegate to appropriate handler
        // 3. Return execution results

        if (implementation == null || implementation.isBlank()) {
            throw new SkillExecutionException(id, "No implementation provided for skill: " + id);
        }

        // Simulate execution
        Map<String, Object> result = Map.of(
            "skillId", id,
            "name", name,
            "status", "executed",
            "implementation", implementation
        );

        logger.info("MarkdownSkill {} executed successfully", id);
        return result;
    }

    @Override
    protected void validateContext(SkillContext context) throws SkillExecutionException {
        super.validateContext(context);

        // Additional validation for markdown skills
        if (implementation == null || implementation.isBlank()) {
            throw new SkillExecutionException(id, "Skill implementation is missing");
        }
    }

    /**
     * Gets the skill metadata.
     *
     * @return unmodifiable metadata map
     */
    public Map<String, Object> getMetadata() {
        return metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    /**
     * Gets the implementation script.
     *
     * @return the implementation
     */
    public String getImplementation() {
        return implementation;
    }

    @Override
    public String toString() {
        return "MarkdownSkill{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
