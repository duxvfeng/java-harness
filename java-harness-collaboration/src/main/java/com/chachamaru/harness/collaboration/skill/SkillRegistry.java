package com.chachamaru.harness.collaboration.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing skills in the collaboration layer.
 *
 * <p>The SkillRegistry provides:
 * <ul>
 *   <li>Skill registration and unregistration</li>
 *   <li>Skill lookup by ID</li>
 *   <li>Skill discovery by tags</li>
 *   <li>Thread-safe skill management</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class SkillRegistry {

    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, Skill> skillsById;
    private final Map<String, Set<Skill>> skillsByTag;

    /**
     * Creates a new skill registry.
     */
    public SkillRegistry() {
        this.skillsById = new ConcurrentHashMap<>();
        this.skillsByTag = new ConcurrentHashMap<>();
    }

    /**
     * Registers a skill.
     *
     * @param skill the skill to register
     * @throws IllegalArgumentException if skill ID is already registered
     */
    public void register(Skill skill) {
        Objects.requireNonNull(skill, "skill cannot be null");

        String id = skill.getId();
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Skill ID cannot be null or blank");
        }

        if (skillsById.containsKey(id)) {
            throw new IllegalArgumentException("Skill already registered: " + id);
        }

        logger.info("Registering skill: {} ({})", skill.getName(), id);
        skillsById.put(id, skill);

        // Index by tags
        for (String tag : skill.getTags()) {
            if (tag != null && !tag.isBlank()) {
                skillsByTag.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(skill);
            }
        }
    }

    /**
     * Unregisters a skill.
     *
     * @param skillId the ID of the skill to unregister
     * @return true if the skill was registered and removed, false otherwise
     */
    public boolean unregister(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return false;
        }

        Skill skill = skillsById.remove(skillId);
        if (skill == null) {
            logger.warn("Attempted to unregister non-existent skill: {}", skillId);
            return false;
        }

        logger.info("Unregistering skill: {}", skillId);

        // Remove from tag index
        for (String tag : skill.getTags()) {
            Set<Skill> taggedSkills = skillsByTag.get(tag);
            if (taggedSkills != null) {
                taggedSkills.remove(skill);
                if (taggedSkills.isEmpty()) {
                    skillsByTag.remove(tag);
                }
            }
        }

        return true;
    }

    /**
     * Gets a skill by ID.
     *
     * @param skillId the skill ID
     * @return the skill, or null if not found
     */
    public Skill getSkill(String skillId) {
        return skillsById.get(skillId);
    }

    /**
     * Checks if a skill is registered.
     *
     * @param skillId the skill ID
     * @return true if the skill is registered, false otherwise
     */
    public boolean hasSkill(String skillId) {
        return skillsById.containsKey(skillId);
    }

    /**
     * Gets all registered skills.
     *
     * @return unmodifiable collection of all skills
     */
    public Collection<Skill> getAllSkills() {
        return Collections.unmodifiableCollection(skillsById.values());
    }

    /**
     * Finds skills by tag.
     *
     * @param tag the tag to search for
     * @return collection of skills with the given tag
     */
    public Collection<Skill> findByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return Collections.emptyList();
        }

        Set<Skill> skills = skillsByTag.get(tag);
        return skills != null ? Collections.unmodifiableSet(skills) : Collections.emptyList();
    }

    /**
     * Finds skills that match all given tags.
     *
     * @param tags the tags to match
     * @return collection of skills matching all tags
     */
    public Collection<Skill> findByTags(String... tags) {
        if (tags == null || tags.length == 0) {
            return Collections.emptyList();
        }

        Set<Skill> result = new HashSet<>(getAllSkills());

        for (String tag : tags) {
            if (tag != null && !tag.isBlank()) {
                Set<Skill> taggedSkills = skillsByTag.get(tag);
                if (taggedSkills != null) {
                    result.retainAll(taggedSkills);
                } else {
                    // Tag doesn't exist, no results
                    return Collections.emptyList();
                }
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Gets the count of registered skills.
     *
     * @return the number of registered skills
     */
    public int getSkillCount() {
        return skillsById.size();
    }

    /**
     * Clears all registered skills.
     */
    public void clear() {
        logger.info("Clearing all skills from registry");
        skillsById.clear();
        skillsByTag.clear();
    }

    /**
     * Gets all tags in the registry.
     *
     * @return unmodifiable set of all tags
     */
    public Set<String> getAllTags() {
        return Collections.unmodifiableSet(skillsByTag.keySet());
    }
}
