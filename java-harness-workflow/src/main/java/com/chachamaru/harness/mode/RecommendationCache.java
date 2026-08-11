package com.chachamaru.harness.mode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 推荐结果缓存
 * 使用LRU策略避免内存过度使用
 */
public class RecommendationCache {

    private final LinkedHashMap<CacheKey, ModeRecommendation> cache;
    private final int maxSize;
    private long hitCount;
    private long missCount;
    private boolean lastAccessWasCached;

    /**
     * 创建默认大小缓存（100条）
     */
    public RecommendationCache() {
        this(100);
    }

    /**
     * 创建指定大小的缓存
     * @param maxSize 最大缓存条目数
     */
    public RecommendationCache(int maxSize) {
        this.maxSize = maxSize;
        this.hitCount = 0;
        this.missCount = 0;
        this.lastAccessWasCached = false;

        // 使用LRU策略的LinkedHashMap
        this.cache = new LinkedHashMap<CacheKey, ModeRecommendation>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, ModeRecommendation> eldest) {
                return size() > RecommendationCache.this.maxSize;
            }
        };
    }

    /**
     * 获取缓存或计算推荐
     * @param tasks 任务列表
     * @param files 文件列表
     * @param compute 计算函数
     * @return 推荐结果
     */
    public ModeRecommendation getOrCompute(
        List<String> tasks,
        List<String> files,
        Supplier<ModeRecommendation> compute
    ) {
        CacheKey key = new CacheKey(tasks, files);

        ModeRecommendation result = cache.get(key);
        if (result != null) {
            hitCount++;
            lastAccessWasCached = true;
            return result;
        }

        missCount++;
        lastAccessWasCached = false;
        result = compute.get();
        cache.put(key, result);
        return result;
    }

    /**
     * 检查是否包含指定键
     */
    public boolean containsKey(List<String> tasks, List<String> files) {
        return cache.containsKey(new CacheKey(tasks, files));
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.clear();
        hitCount = 0;
        missCount = 0;
    }

    /**
     * 清除过期条目（简化实现，实际可以基于时间戳）
     */
    public void clearExpired() {
        // 在实际实现中，可以基于添加时间戳清除过期条目
        // 这里简化为清除所有缓存
        cache.clear();
    }

    /**
     * 获取缓存命中率
     */
    public double getHitRate() {
        long total = hitCount + missCount;
        return total == 0 ? 0.0 : (double) hitCount / total;
    }

    /**
     * 获取命中次数
     */
    public long getHitCount() {
        return hitCount;
    }

    /**
     * 获取未命中次数
     */
    public long getMissCount() {
        return missCount;
    }

    /**
     * 上次访问是否命中缓存
     */
    public boolean wasCached() {
        return lastAccessWasCached;
    }

    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }

    /**
     * 缓存键，基于任务和文件列表
     */
    private record CacheKey(List<String> tasks, List<String> files) {
        public CacheKey {
            // 确保不可变
            tasks = List.copyOf(tasks != null ? tasks : List.of());
            files = List.copyOf(files != null ? files : List.of());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof CacheKey other)) return false;
            return tasks.equals(other.tasks) && files.equals(other.files);
        }

        @Override
        public int hashCode() {
            return tasks.hashCode() * 31 + files.hashCode();
        }
    }
}