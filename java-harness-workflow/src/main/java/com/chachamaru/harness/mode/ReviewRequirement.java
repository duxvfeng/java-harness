package com.chachamaru.harness.mode;

/**
 * 代码审查需求枚举
 * 描述任务对代码审查的需求程度
 */
public enum ReviewRequirement {
    /**
     * 无需审查 - 低风险变更，不需要正式的代码审查
     * 典型场景：文档修改、注释更新、格式调整
     */
    NONE,

    /**
     * 可选审查 - 中等风险变更，建议进行代码审查但非强制
     * 典型场景：bug 修复、小功能添加、配置调整
     */
    OPTIONAL,

    /**
     * 必须审查 - 高风险变更，必须进行正式的代码审查
     * 典型场景：核心功能修改、安全相关、架构调整
     */
    REQUIRED
}