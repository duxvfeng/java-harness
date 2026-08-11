package com.chachamaru.harness.mode;

/**
 * 执行模式枚举
 * 定义 Java Harness 支持的三种任务执行模式
 */
public enum ExecutionMode {
    /**
     * Solo 模式 - 单任务直接执行，开销最小
     * 适用场景：单个任务，简单复杂度，无依赖关系
     */
    SOLO,

    /**
     * Parallel 模式 - 并行执行 2-3 个任务
     * 适用场景：2-3 个独立任务，中等复杂度
     */
    PARALLEL,

    /**
     * Breezing 模式 - Lead/Worker/Reviewer 团队协作
     * 适用场景：4 个以上任务，复杂任务，需要代码审查
     */
    BREEZING
}