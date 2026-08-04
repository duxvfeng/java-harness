package com.chachamaru.harness.workflow.agent.framework;

import java.util.List;

/**
 * Agent 接口
 * 所有 Agent 必须实现此接口
 */
public interface Agent extends AgentLifecycle {

    /**
     * 获取 Agent 唯一标识符
     */
    String getAgentId();

    /**
     * 获取 Agent 名称
     */
    String getAgentName();

    /**
     * 获取 Agent 版本
     */
    String getVersion();

    /**
     * 获取 Agent 描述
     */
    String getDescription();

    /**
     * 获取 Agent 类型
     */
    AgentType getAgentType();

    /**
     * 获取 Agent 所需的能力
     */
    List<String> getRequiredSkills();

    /**
     * 执行 Agent 任务（核心方法）
     */
    AgentResult execute(AgentContext context) throws AgentExecutionException;

    /**
     * 验证前置条件
     */
    default boolean validatePreconditions(AgentContext context) {
        return true;
    }

    /**
     * 获取 Agent 配置
     */
    default AgentConfig getConfig() {
        return AgentConfig.defaultConfig();
    }
}
