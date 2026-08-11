package com.chachamaru.harness.mode;

/**
 * 任务依赖关系类型枚举
 * 描述任务之间的依赖关系，影响执行模式的选择
 */
public enum DependencyType {
    /**
     * 独立 - 任务之间没有依赖关系，可以并行执行
     * 典型场景：多个独立的 bug 修复、独立的 feature 开发
     */
    INDEPENDENT,

    /**
     * 顺序 - 任务之间有严格的前后依赖关系，必须串行执行
     * 典型场景：依赖模块升级、数据库迁移、API 版本升级
     */
    SEQUENTIAL,

    /**
     * 混合 - 任务之间有部分依赖关系，需要协调执行顺序
     * 典型场景：相关模块重构、跨功能开发、系统集成
     */
    MIXED
}