package com.chachamaru.harness.workflow.skill.framework;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

/**
 * 技能框架核心
 * 负责技能的注册、管理和执行
 */
public class SkillFramework implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SkillFramework.class);

    private final SkillRegistry registry;
    private final SkillExecutor executor;

    private boolean initialized = false;

    /**
     * 构造函数
     */
    public SkillFramework() {
        this.registry = new SkillRegistry();
        this.executor = new SkillExecutor();
        logger.info("SkillFramework created");
    }

    /**
     * 构造函数（带自定义执行器）
     */
    public SkillFramework(SkillExecutor executor) {
        this.registry = new SkillRegistry();
        this.executor = executor != null ? executor : new SkillExecutor();
        logger.info("SkillFramework created with custom executor");
    }

    /**
     * 初始化核心技能
     * 在子类中重写此方法以注册特定技能
     */
    public void initialize() {
        if (initialized) {
            logger.warn("SkillFramework already initialized");
            return;
        }

        logger.info("Initializing SkillFramework");
        initializeCoreSkills();
        initialized = true;

        logger.info("SkillFramework initialized with {} skills", registry.getSkillCount());
    }

    /**
     * 初始化核心技能
     * 子类可以重写此方法来注册特定技能
     */
    protected void initializeCoreSkills() {
        logger.debug("No core skills to initialize in base implementation");
    }

    /**
     * 注册技能
     *
     * @param skill 技能实例
     */
    public void registerSkill(Skill skill) {
        registry.register(skill);
        logger.debug("Registered skill: {}", skill.getSkillId());
    }

    /**
     * 注册技能元数据
     *
     * @param skillId 技能ID
     * @param skillName 技能名称
     * @param version 版本
     * @param description 描述
     */
    public void registerSkill(String skillId, String skillName, String version, String description) {
        registry.register(skillId, skillName, version, description);
        logger.debug("Registered skill metadata: {}", skillId);
    }

    /**
     * 执行技能
     *
     * @param skillId 技能ID
     * @param context 执行上下文
     * @return 执行结果
     * @throws SkillExecutionException 执行异常
     * @throws SkillNotFoundException 技能未找到异常
     */
    public SkillResult executeSkill(String skillId, SkillContext context) throws SkillExecutionException {
        ensureInitialized();

        Skill skill = findSkill(skillId)
                .orElseThrow(() -> new SkillNotFoundException(skillId));

        logger.info("Executing skill: {} for intent: {}", skillId, context.getUserIntent());

        return executor.execute(skill, context);
    }

    /**
     * 查找技能
     *
     * @param skillId 技能ID
     * @return 技能实例（Optional）
     */
    public Optional<Skill> findSkill(String skillId) {
        return Optional.ofNullable(registry.getSkill(skillId));
    }

    /**
     * 获取技能元数据
     *
     * @param skillId 技能ID
     * @return 技能元数据（Optional）
     */
    public Optional<SkillRegistry.SkillMetadata> getSkillMetadata(String skillId) {
        return Optional.ofNullable(registry.getMetadata(skillId));
    }

    /**
     * 获取所有注册的技能
     *
     * @return 技能映射
     */
    public Map<String, SkillRegistry.SkillMetadata> getRegisteredSkills() {
        return registry.getAllSkills();
    }

    /**
     * 获取所有技能实例
     *
     * @return 技能实例映射
     */
    public Map<String, Skill> getSkillInstances() {
        return registry.getAllSkillInstances();
    }

    /**
     * 检查技能是否已注册
     *
     * @param skillId 技能ID
     * @return 是否已注册
     */
    public boolean isSkillRegistered(String skillId) {
        return registry.isRegistered(skillId);
    }

    /**
     * 获取技能数量
     *
     * @return 技能数量
     */
    public int getSkillCount() {
        return registry.getSkillCount();
    }

    /**
     * 注销技能
     *
     * @param skillId 技能ID
     */
    public void unregisterSkill(String skillId) {
        registry.unregister(skillId);
        logger.info("Unregistered skill: {}", skillId);
    }

    /**
     * 获取活跃执行数量
     *
     * @return 活跃执行数量
     */
    public int getActiveExecutionCount() {
        return executor.getActiveExecutionCount();
    }

    /**
     * 确保框架已初始化
     */
    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("SkillFramework not initialized. Call initialize() first.");
        }
    }

    /**
     * 获取技能注册表
     *
     * @return 技能注册表
     */
    protected SkillRegistry getRegistry() {
        return registry;
    }

    /**
     * 获取技能执行器
     *
     * @return 技能执行器
     */
    protected SkillExecutor getExecutor() {
        return executor;
    }

    @Override
    public void close() {
        logger.info("Shutting down SkillFramework");
        registry.clear();
        initialized = false;
    }

    /**
     * 获取初始化状态
     *
     * @return 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }
}