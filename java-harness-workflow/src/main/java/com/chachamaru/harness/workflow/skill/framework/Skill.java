package com.chachamaru.harness.workflow.skill.framework;

import java.time.Instant;

/**
 * 技能接口
 * 所有技能必须实现此接口
 */
public interface Skill {

    /**
     * 获取技能唯一标识符
     */
    String getSkillId();

    /**
     * 获取技能名称
     */
    String getSkillName();

    /**
     * 获取技能版本
     */
    String getVersion();

    /**
     * 获取技能描述
     */
    String getDescription();

    /**
     * 执行技能
     *
     * @param context 技能执行上下文
     * @return 技能执行结果
     */
    Object execute(SkillContext context) throws SkillExecutionException;

    /**
     * 技能是否支持暂停
     */
    default boolean supportsPause() {
        return false;
    }

    /**
     * 技能是否支持恢复
     */
    default boolean supportsResume() {
        return false;
    }

    /**
     * 技能是否支持取消
     */
    default boolean supportsCancel() {
        return false;
    }

    /**
     * 验证技能前置条件
     */
    default boolean validatePreconditions(SkillContext context) {
        return true;
    }
}