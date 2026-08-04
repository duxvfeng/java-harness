package com.chachamaru.harness.workflow.agent.framework;

/**
 * Agent 状态枚举
 */
public enum AgentStatus {
    PENDING {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    RUNNING {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    SUCCESS {
        @Override
        public boolean isSuccess() { return true; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    FAILED {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return true; }
        @Override
        public boolean isPartialSuccess() { return false; }
    },
    SUCCESS_WITH_WARNINGS {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return true; }
    },
    PARTIAL_SUCCESS {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return true; }
    },
    CANCELLED {
        @Override
        public boolean isSuccess() { return false; }
        @Override
        public boolean isFailed() { return false; }
        @Override
        public boolean isPartialSuccess() { return false; }
    };

    public abstract boolean isSuccess();
    public abstract boolean isFailed();
    public abstract boolean isPartialSuccess();
}
