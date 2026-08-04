package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 生命周期接口
 */
public interface AgentLifecycle {

    /**
     * 初始化 Agent
     */
    void initialize();

    /**
     * 是否支持暂停
     */
    default boolean supportsPause() {
        return false;
    }

    /**
     * 是否支持恢复
     */
    default boolean supportsResume() {
        return false;
    }

    /**
     * 暂停执行
     */
    default void pause() throws AgentLifecycleException {
        throw new UnsupportedOperationException("Pause not supported");
    }

    /**
     * 恢复执行
     */
    default void resume() throws AgentLifecycleException {
        throw new UnsupportedOperationException("Resume not supported");
    }

    /**
     * 取消执行
     */
    default void cancel() throws AgentLifecycleException {
        throw new UnsupportedOperationException("Cancel not supported");
    }

    /**
     * 清理资源
     */
    default void cleanup() {
    }
}
