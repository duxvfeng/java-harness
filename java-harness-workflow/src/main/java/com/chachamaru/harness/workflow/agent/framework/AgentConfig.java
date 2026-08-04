package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 配置类
 * 阶段1：简单配置
 */
public class AgentConfig {
    private final boolean parallelExecutionEnabled;
    private final int timeoutSeconds;
    private final int maxRetries;

    private AgentConfig(Builder builder) {
        this.parallelExecutionEnabled = builder.parallelExecutionEnabled;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.maxRetries = builder.maxRetries;
    }

    public boolean isParallelExecutionEnabled() {
        return parallelExecutionEnabled;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public static AgentConfig defaultConfig() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean parallelExecutionEnabled = false;
        private int timeoutSeconds = 300;
        private int maxRetries = 0;

        public Builder parallelExecutionEnabled(boolean enabled) {
            this.parallelExecutionEnabled = enabled;
            return this;
        }

        public Builder timeoutSeconds(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public Builder maxRetries(int retries) {
            this.maxRetries = retries;
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(this);
        }
    }
}
