package com.chachamaru.harness.foundation.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 并发状态控制器
 * 提供多会话状态并发控制
 */
public class ConcurrentStateController {

    private final ConcurrentHashMap<String, SessionState> activeSessions;
    private final ReadWriteLock stateLock;
    private volatile String currentOwner;

    public ConcurrentStateController() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.stateLock = new ReentrantReadWriteLock();
        this.currentOwner = null;
    }

    /**
     * 请求状态访问锁
     */
    public boolean requestAccess(String sessionId, AccessMode mode) {
        if (mode == AccessMode.EXCLUSIVE) {
            return requestExclusiveAccess(sessionId);
        } else {
            return requestSharedAccess(sessionId);
        }
    }

    /**
     * 释放状态访问锁
     */
    public void releaseAccess(String sessionId) {
        stateLock.writeLock().lock();
        try {
            SessionState sessionState = activeSessions.get(sessionId);
            if (sessionState != null && sessionState.getLockCount() > 0) {
                sessionState.decrementLockCount();
                if (sessionState.getLockCount() == 0) {
                    if (sessionId.equals(currentOwner)) {
                        currentOwner = null;
                    }
                    activeSessions.remove(sessionId);
                }
            }
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * 检查会话是否活跃
     */
    public boolean isSessionActive(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }

    /**
     * 获取当前活跃会话数量
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    private boolean requestExclusiveAccess(String sessionId) {
        stateLock.writeLock().lock();
        try {
            if (currentOwner != null && !currentOwner.equals(sessionId)) {
                return false; // 已被其他会话占用
            }

            SessionState sessionState = activeSessions.computeIfAbsent(
                    sessionId,
                    k -> new SessionState(sessionId, AccessMode.EXCLUSIVE)
            );
            sessionState.incrementLockCount();
            currentOwner = sessionId;
            return true;

        } finally {
            stateLock.writeLock().unlock();
        }
    }

    private boolean requestSharedAccess(String sessionId) {
        stateLock.readLock().lock();
        try {
            if (currentOwner != null) {
                return false; // 存在独占占用
            }

            SessionState sessionState = activeSessions.computeIfAbsent(
                    sessionId,
                    k -> new SessionState(sessionId, AccessMode.SHARED)
            );
            sessionState.incrementLockCount();
            return true;

        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * 会话状态
     */
    static class SessionState {
        private final String sessionId;
        private final AccessMode accessMode;
        private int lockCount;
        private final long lastAccess;

        public SessionState(String sessionId, AccessMode accessMode) {
            this.sessionId = sessionId;
            this.accessMode = accessMode;
            this.lockCount = 1;
            this.lastAccess = System.currentTimeMillis();
        }

        public String getSessionId() {
            return sessionId;
        }

        public AccessMode getAccessMode() {
            return accessMode;
        }

        public int getLockCount() {
            return lockCount;
        }

        public void incrementLockCount() {
            this.lockCount++;
        }

        public void decrementLockCount() {
            this.lockCount--;
        }

        public long getLastAccess() {
            return lastAccess;
        }
    }

    /**
     * 访问模式
     */
    public enum AccessMode {
        SHARED,
        EXCLUSIVE
    }
}
