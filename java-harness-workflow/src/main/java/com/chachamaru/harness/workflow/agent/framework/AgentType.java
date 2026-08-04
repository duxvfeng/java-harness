package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 类型枚举
 */
public enum AgentType {
    WORKER("工作代理", "执行具体任务的代理"),
    REVIEWER("审查代理", "审查和评审工作的代理"),
    ADVISOR("顾问代理", "提供建议和指导的代理"),
    PLANNER("规划代理", "制定计划的代理"),
    CRITIC("批评代理", "评审和提出改进的代理");

    private final String displayName;
    private final String description;

    AgentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}