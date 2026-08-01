package com.chachamaru.harness.cli.guardrail.cache;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.hook.HookInput;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple LRU cache for guardrail evaluation results
 */
public class EvaluationCache {
    private static final int MAX_CACHE_SIZE = 1000;
    private final Map<String, CacheEntry> cache;
    private final int maxSize;

    public EvaluationCache() {
        this(MAX_CACHE_SIZE);
    }

    public EvaluationCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>(maxSize);
    }

    /**
     * Generate cache key from input
     */
    private String generateKey(HookInput input) {
        StringBuilder key = new StringBuilder();
        key.append(input.toolName());

        if (input.toolInput() != null) {
            input.toolInput().forEach((k, v) -> {
                key.append("|").append(k).append("=").append(v != null ? v.toString() : "null");
            });
        }

        return key.toString();
    }

    /**
     * Get cached result if available
     */
    public GuardrailResult get(HookInput input) {
        String key = generateKey(input);
        CacheEntry entry = cache.get(key);

        if (entry != null && !entry.isExpired()) {
            entry.access();
            return entry.getResult();
        }

        return null;
    }

    /**
     * Put result in cache
     */
    public void put(HookInput input, GuardrailResult result) {
        String key = generateKey(input);

        // Evict oldest if cache is full
        if (cache.size() >= maxSize) {
            evictOldest();
        }

        cache.put(key, new CacheEntry(result));
    }

    /**
     * Clear the cache
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        return new CacheStats(cache.size(), maxSize);
    }

    private void evictOldest() {
        Map.Entry<String, CacheEntry> oldest = null;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (oldest == null || entry.getValue().getLastAccess() < oldest.getValue().getLastAccess()) {
                oldest = entry;
            }
        }

        if (oldest != null) {
            cache.remove(oldest.getKey());
        }
    }

    private static class CacheEntry {
        private final GuardrailResult result;
        private final long createdAt;
        private long lastAccess;
        private static final long TTL_MS = 5 * 60 * 1000; // 5 minutes TTL

        public CacheEntry(GuardrailResult result) {
            this.result = result;
            this.createdAt = System.currentTimeMillis();
            this.lastAccess = createdAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TTL_MS;
        }

        public void access() {
            this.lastAccess = System.currentTimeMillis();
        }

        public GuardrailResult getResult() {
            return result;
        }

        public long getLastAccess() {
            return lastAccess;
        }
    }

    public static class CacheStats {
        private final int currentSize;
        private final int maxSize;

        public CacheStats(int currentSize, int maxSize) {
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }

        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
        public double getUsagePercentage() {
            return maxSize > 0 ? (currentSize * 100.0 / maxSize) : 0;
        }
    }
}