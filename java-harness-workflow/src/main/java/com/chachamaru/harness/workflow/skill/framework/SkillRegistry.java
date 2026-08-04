package com.chachamaru.harness.workflow.skill.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能注册表
 * 管理技能的元数据和注册信息
 */
public class SkillRegistry {
    private static final Logger logger = LoggerFactory.getLogger(SkillRegistry.class);

    private final Map<String, SkillMetadata> registry;
    private final Map<String, Skill> skillInstances;

    public SkillRegistry() {
        this.registry = new ConcurrentHashMap<>();
        this.skillInstances = new ConcurrentHashMap<>();
    }

    /**
     * 注册技能
     *
     * @param skill 技能实例
     */
    public void register(Skill skill) {
        String skillId = skill.getSkillId();
        SkillMetadata metadata = new SkillMetadata(
                skillId,
                skill.getSkillName(),
                skill.getVersion(),
                skill.getDescription()
        );

        registry.put(skillId, metadata);
        skillInstances.put(skillId, skill);

        logger.info("Registered skill: {} (version: {})", skillId, skill.getVersion());
    }

    /**
     * 注册技能元数据
     *
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param version 版本
     * @param description 描述
     */
    public void register(String skillId, String skillName, String version, String description) {
        SkillMetadata metadata = new SkillMetadata(skillId, skillName, version, description);
        registry.put(skillId, metadata);

        logger.info("Registered skill metadata: {} (version: {})", skillId, version);
    }

    /**
     * 获取技能元数据
     *
     * @param skillId 技能ID
     * @return 技能元数据
     */
    public SkillMetadata getMetadata(String skillId) {
        return registry.get(skillId);
    }

    /**
     * 获取技能实例
     *
     * @param skillId 技能ID
     * @return 技能实例
     */
    public Skill getSkill(String skillId) {
        return skillInstances.get(skillId);
    }

    /**
     * 获取所有技能元数据
     *
     * @return 技能元数据映射
     */
    public Map<String, SkillMetadata> getAllSkills() {
        return Collections.unmodifiableMap(new HashMap<>(registry));
    }

    /**
     * 获取所有技能实例
     *
     * @return 技能实例映射
     */
    public Map<String, Skill> getAllSkillInstances() {
        return Collections.unmodifiableMap(new HashMap<>(skillInstances));
    }

    /**
     * 检查技能是否已注册
     *
     * @param skillId 技能ID
     * @return 是否已注册
     */
    public boolean isRegistered(String skillId) {
        return registry.containsKey(skillId);
    }

    /**
     * 获取技能数量
     *
     * @return 技能数量
     */
    public int getSkillCount() {
        return registry.size();
    }

    /**
     * 注销技能
     *
     * @param skillId 技能ID
     */
    public void unregister(String skillId) {
        SkillMetadata removed = registry.remove(skillId);
        Skill removedInstance = skillInstances.remove(skillId);

        if (removed != null) {
            logger.info("Unregistered skill: {}", skillId);
        }
    }

    /**
     * 清空注册表
     */
    public void clear() {
        int size = registry.size();
        registry.clear();
        skillInstances.clear();

        logger.info("Cleared {} skills from registry", size);
    }

    /**
     * 技能元数据
     */
    public static class SkillMetadata {
        private final String skillId;
        private final String skillName;
        private final String version;
        private final String description;

        public SkillMetadata(String skillId, String skillName, String version, String description) {
            this.skillId = Objects.requireNonNull(skillId, "skillId cannot be null");
            this.skillName = Objects.requireNonNull(skillName, "skillName cannot be null");
            this.version = Objects.requireNonNull(version, "version cannot be null");
            this.description = description;
        }

        public String getSkillId() {
            return skillId;
        }

        public String getSkillName() {
            return skillName;
        }

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SkillMetadata that = (SkillMetadata) o;
            return Objects.equals(skillId, that.skillId) &&
                    Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(skillId, version);
        }

        @Override
        public String toString() {
            return "SkillMetadata{" +
                    "skillId='" + skillId + '\'' +
                    ", skillName='" + skillName + '\'' +
                    ", version='" + version + '\'' +
                    ", description='" + description + '\'' +
                    '}';
        }
    }
}