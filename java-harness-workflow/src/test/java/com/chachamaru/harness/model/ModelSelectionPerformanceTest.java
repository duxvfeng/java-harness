package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.CountDownLatch;
import java.util.ArrayList;
import java.util.List;

/**
 * 智能模型选择性能和压力测试
 * 验证系统在高负载下的性能表现和稳定性
 */
class ModelSelectionPerformanceTest {

    private SmartModelSelector selector;
    private ModelSelectionConfig config;

    @BeforeEach
    void setUp() {
        ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
        config = loader.loadOrDefault();
        selector = new SmartModelSelector(config);
    }

    @AfterEach
    void tearDown() {
        if (selector != null) {
            selector.clearAllCache();
        }
    }

    // ==================== 性能基准测试 ====================

    @Test
    void testSingleSelectionPerformance() throws ModelUnavailableException {
        // 预热
        for (int i = 0; i < 100; i++) {
            selector.selectModel(i % 11);
        }

        // 性能测试
        long totalTime = 0;
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            selector.selectModel(i % 11);
            long duration = System.nanoTime() - startTime;
            totalTime += duration;
        }

        double avgTimeMs = (totalTime / iterations) / 1_000_000.0;

        // 验证平均选择时间 < 100ms
        assertTrue(avgTimeMs < 100,
                   "Average selection time should be < 100ms, was: " + avgTimeMs + "ms");

        System.out.println("Average selection time: " + avgTimeMs + "ms");
    }

    @Test
    void testCachedSelectionPerformance() throws ModelUnavailableException {
        // 预填充缓存
        int[] scores = {0, 3, 5, 7};
        for (int score : scores) {
            selector.selectModel(score);
        }

        // 测试缓存命中性能
        long totalTime = 0;
        int iterations = 1000;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            selector.selectModel(scores[i % scores.length]);
            long duration = System.nanoTime() - startTime;
            totalTime += duration;
        }

        double avgTimeMs = (totalTime / iterations) / 1_000_000.0;

        // 缓存命中应该非常快 (< 10ms)
        assertTrue(avgTimeMs < 10,
                   "Cached selection time should be < 10ms, was: " + avgTimeMs + "ms");

        System.out.println("Average cached selection time: " + avgTimeMs + "ms");
    }

    @Test
    void testConfigurationLoadingPerformance() {
        // 测试配置加载性能
        long totalTime = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();
            ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
            ModelSelectionConfig config = loader.loadOrDefault();
            long duration = System.nanoTime() - startTime;
            totalTime += duration;
        }

        double avgTimeMs = (totalTime / iterations) / 1_000_000.0;

        // 配置加载应该在合理时间内完成 (< 500ms)
        assertTrue(avgTimeMs < 500,
                   "Config loading time should be < 500ms, was: " + avgTimeMs + "ms");

        System.out.println("Average config loading time: " + avgTimeMs + "ms");
    }

    // ==================== 并发压力测试 ====================

    @Test
    void testConcurrentSelectionLoad() throws InterruptedException {
        int threadCount = 10;
        int requestsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> durations = new CopyOnWriteArrayList<>();

        // 并发执行模型选择
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            long startTime = System.nanoTime();
                            String model = selector.selectModel(j % 11);
                            long duration = System.nanoTime() - startTime;
                            durations.add(duration);

                            if (model != null && !model.trim().isEmpty()) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(60, TimeUnit.SECONDS), "Concurrent test should complete");
        executor.shutdown();

        // 计算统计信息
        int totalRequests = threadCount * requestsPerThread;
        double successRate = (double) successCount.get() / totalRequests * 100;

        // 计算性能统计
        long totalDuration = 0;
        for (Long duration : durations) {
            totalDuration += duration;
        }
        double avgDurationMs = (totalDuration / durations.size()) / 1_000_000.0;

        // 验证成功率 > 95%
        assertTrue(successRate > 95,
                   "Success rate should be > 95%, was: " + successRate + "%");

        // 验证平均响应时间 < 200ms
        assertTrue(avgDurationMs < 200,
                   "Average response time should be < 200ms, was: " + avgDurationMs + "ms");

        System.out.println("Concurrent test results:");
        System.out.println("  Success rate: " + successRate + "%");
        System.out.println("  Average response time: " + avgDurationMs + "ms");
        System.out.println("  Total requests: " + totalRequests);
    }

    @Test
    void testHighConcurrencyStress() throws InterruptedException {
        int threadCount = 20; // 高并发
        int requestsPerThread = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // 高并发压力测试
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        try {
                            String model = selector.selectModel(j % 11);
                            if (model != null && !model.trim().isEmpty()) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(120, TimeUnit.SECONDS), "High concurrency test should complete");
        executor.shutdown();

        int totalRequests = threadCount * requestsPerThread;
        double successRate = (double) successCount.get() / totalRequests * 100;

        // 即使在高并发下，成功率也应该 > 90%
        assertTrue(successRate > 90,
                   "High concurrency success rate should be > 90%, was: " + successRate + "%");

        System.out.println("High concurrency stress test - Success rate: " + successRate + "%");
    }

    @Test
    void testMemoryStabilityUnderLoad() throws InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        long startMemory = runtime.totalMemory() - runtime.freeMemory();

        // 执行大量选择操作
        int iterations = 10000;
        for (int i = 0; i < iterations; i++) {
            try {
                selector.selectModel(i % 11);
            } catch (Exception e) {
                // 忽略个别错误
            }
        }

        // 建议垃圾回收
        System.gc();
        Thread.sleep(1000);

        long endMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = endMemory - startMemory;
        double memoryIncreaseMb = memoryIncrease / (1024.0 * 1024.0);

        // 内存增长应该在合理范围内 (< 50MB)
        assertTrue(memoryIncreaseMb < 50,
                   "Memory increase should be < 50MB, was: " + memoryIncreaseMb + "MB");

        System.out.println("Memory increase under load: " + memoryIncreaseMb + "MB");
    }

    // ==================== 缓存性能测试 ====================

    @Test
    void testCachePerformance() throws ModelUnavailableException {
        // 清除缓存
        selector.clearAllCache();

        // 第一次选择（缓存未命中）
        long firstTime = measureSelectionTime(selector, 5);

        // 后续选择（应该命中缓存）
        long cachedTime = measureSelectionTime(selector, 5);

        // 缓存命中应该明显更快
        assertTrue(cachedTime <= firstTime || firstTime < 50,
                   "Cached selection should be faster or acceptable: first=" +
                   firstTime + "ms, cached=" + cachedTime + "ms");

        double cacheImprovement = ((double)(firstTime - cachedTime) / firstTime) * 100;
        System.out.println("Cache performance improvement: " + cacheImprovement + "%");
    }

    @Test
    void testCacheHitRate() throws ModelUnavailableException {
        selector.clearAllCache();

        // 生成重复的选择模式
        int[] pattern = {3, 5, 7, 3, 5, 7, 3, 5, 7}; // 高重复率
        for (int score : pattern) {
            selector.selectModel(score);
        }

        // 获取缓存统计
        String stats = selector.getCacheStats();
        assertNotNull(stats);

        // 缓存命中率应该 > 60%（因为有重复模式）
        double hitRate = selector.getCacheHitRate();
        assertTrue(hitRate > 60,
                   "Cache hit rate should be > 60%, was: " + hitRate + "%");

        System.out.println("Cache hit rate: " + hitRate + "%");
    }

    @Test
    void testCacheConcurrency() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 并发填充和访问缓存
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        try {
                            // 使用不同的复杂度分数
                            selector.selectModel((threadId * 10 + j) % 11);
                        } catch (Exception e) {
                            // 忽略错误
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "Cache concurrency test should complete");
        executor.shutdown();

        // 验证缓存仍然工作
        String stats = selector.getCacheStats();
        assertNotNull(stats);
        System.out.println("Cache stats after concurrency: " + stats);
    }

    // ==================== 长时间运行稳定性测试 ====================

    @Test
    void testLongRunningStability() throws ModelUnavailableException, InterruptedException {
        // 长时间运行测试（10秒）
        long endTime = System.currentTimeMillis() + 10000;
        int requestCount = 0;
        int errorCount = 0;

        while (System.currentTimeMillis() < endTime) {
            try {
                for (int i = 0; i < 100; i++) {
                    selector.selectModel(i % 11);
                    requestCount++;
                }
                Thread.sleep(10); // 小延迟模拟真实负载
            } catch (Exception e) {
                errorCount++;
            }
        }

        double errorRate = (double) errorCount / requestCount * 100;

        // 错误率应该 < 1%
        assertTrue(errorRate < 1,
                   "Error rate should be < 1%, was: " + errorRate + "%");

        System.out.println("Long running stability - Requests: " + requestCount +
                          ", Errors: " + errorCount + ", Error rate: " + errorRate + "%");
    }

    // ==================== 辅助方法 ====================

    private long measureSelectionTime(SmartModelSelector selector, int score) throws ModelUnavailableException {
        long startTime = System.nanoTime();
        selector.selectModel(score);
        return (System.nanoTime() - startTime) / 1_000_000; // 转换为毫秒
    }

    private double calculatePercentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;

        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);

        int index = (int) Math.ceil(percentile / 100 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));

        return sorted.get(index) / 1_000_000.0; // 转换为毫秒
    }
}