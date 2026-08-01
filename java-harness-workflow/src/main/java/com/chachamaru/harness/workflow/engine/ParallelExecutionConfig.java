package com.chachamaru.harness.workflow.engine;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并行执行配置
 * 控制工作流并行执行的资源使用和性能
 */
public class ParallelExecutionConfig {

    private int maxParallelThreads = Runtime.getRuntime().availableProcessors();
    private int threadKeepAliveTime = 60; // seconds
    private int maxQueueSize = 100;
    private boolean continueOnFailure = true;
    private long timeoutPerStep = 300; // seconds (5 minutes default)

    /**
     * 创建默认配置
     */
    public static ParallelExecutionConfig createDefault() {
        return new ParallelExecutionConfig();
    }

    /**
     * 创建高性能配置（用于CPU密集型任务）
     */
    public static ParallelExecutionConfig createHighPerformance() {
        ParallelExecutionConfig config = new ParallelExecutionConfig();
        config.maxParallelThreads = Runtime.getRuntime().availableProcessors() * 2;
        config.threadKeepAliveTime = 30;
        return config;
    }

    /**
     * 创建保守配置（用于IO密集型或资源受限场景）
     */
    public static ParallelExecutionConfig createConservative() {
        ParallelExecutionConfig config = new ParallelExecutionConfig();
        config.maxParallelThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        config.maxQueueSize = 50;
        return config;
    }

    public int getMaxParallelThreads() {
        return maxParallelThreads;
    }

    public void setMaxParallelThreads(int maxParallelThreads) {
        if (maxParallelThreads < 1) {
            throw new IllegalArgumentException("maxParallelThreads must be at least 1");
        }
        this.maxParallelThreads = maxParallelThreads;
    }

    public int getThreadKeepAliveTime() {
        return threadKeepAliveTime;
    }

    public void setThreadKeepAliveTime(int threadKeepAliveTime) {
        if (threadKeepAliveTime < 1) {
            throw new IllegalArgumentException("threadKeepAliveTime must be at least 1");
        }
        this.threadKeepAliveTime = threadKeepAliveTime;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        if (maxQueueSize < 1) {
            throw new IllegalArgumentException("maxQueueSize must be at least 1");
        }
        this.maxQueueSize = maxQueueSize;
    }

    public boolean isContinueOnFailure() {
        return continueOnFailure;
    }

    public void setContinueOnFailure(boolean continueOnFailure) {
        this.continueOnFailure = continueOnFailure;
    }

    public long getTimeoutPerStep() {
        return timeoutPerStep;
    }

    public void setTimeoutPerStep(long timeoutPerStep) {
        if (timeoutPerStep < 1) {
            throw new IllegalArgumentException("timeoutPerStep must be at least 1");
        }
        this.timeoutPerStep = timeoutPerStep;
    }

    /**
     * 获取配置摘要
     */
    public String getSummary() {
        return String.format(
            "ParallelExecutionConfig{maxThreads=%d, keepAlive=%ds, maxQueue=%d, continueOnFailure=%s, timeout=%ds}",
            maxParallelThreads, threadKeepAliveTime, maxQueueSize, continueOnFailure, timeoutPerStep
        );
    }
}
