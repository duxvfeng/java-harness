package com.chachamaru.harness.workflow.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 并行执行优化测试
 * 验证线程池管理、资源限制、错误隔离
 */
class ParallelExecutionOptimizationTest {

    private static final Logger log = LoggerFactory.getLogger(ParallelExecutionOptimizationTest.class);

    private ParallelExecutionConfig config;
    private OptimizedParallelExecutor executor;

    @BeforeEach
    void setUp() {
        config = ParallelExecutionConfig.createDefault();
        executor = new OptimizedParallelExecutor(config);
    }

    @Test
    void testBasicParallelExecution() throws Exception {
        log.info("=== 测试基本并行执行 ===");

        List<OptimizedParallelExecutor.ParallelTask<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                @Override
                public String getName() {
                    return "task-" + taskId;
                }

                @Override
                public String execute() {
                    try {
                        Thread.sleep(100); // 模拟工作
                        return "result-" + taskId;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        List<OptimizedParallelExecutor.ExecutionResult<String>> results =
            executor.executeAll(tasks, 5000);

        assertEquals(5, results.size());
        int successCount = 0;
        for (OptimizedParallelExecutor.ExecutionResult<String> result : results) {
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(5, successCount, "所有任务应该成功");
        log.info("✓ 基本并行执行通过：5个任务全部成功");
    }

    @Test
    void testErrorIsolation() throws Exception {
        log.info("=== 测试错误隔离 ===");

        List<OptimizedParallelExecutor.ParallelTask<String>> tasks = new ArrayList<>();

        // 添加3个成功任务
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                @Override
                public String getName() {
                    return "success-task-" + taskId;
                }

                @Override
                public String execute() {
                    try {
                        Thread.sleep(50);
                        return "success-" + taskId;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        // 添加2个失败任务
        for (int i = 0; i < 2; i++) {
            final int taskId = i;
            tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                @Override
                public String getName() {
                    return "fail-task-" + taskId;
                }

                @Override
                public String execute() {
                    try {
                        Thread.sleep(50);
                        throw new RuntimeException("Intentional failure " + taskId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        List<OptimizedParallelExecutor.ExecutionResult<String>> results =
            executor.executeAll(tasks, 5000);

        assertEquals(5, results.size());

        int successCount = 0;
        int failCount = 0;
        for (OptimizedParallelExecutor.ExecutionResult<String> result : results) {
            if (result.isSuccess()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        assertEquals(3, successCount, "3个任务应该成功");
        assertEquals(2, failCount, "2个任务应该失败");
        log.info("✓ 错误隔离通过：失败不影响其他任务");
    }

    @Test
    void testResourceLimiting() throws Exception {
        log.info("=== 测试资源限制 ===");

        // 使用保守配置：只有2个线程
        ParallelExecutionConfig conservativeConfig = ParallelExecutionConfig.createConservative();
        conservativeConfig.setMaxParallelThreads(2);
        OptimizedParallelExecutor limitedExecutor = new OptimizedParallelExecutor(conservativeConfig);

        try {
            List<OptimizedParallelExecutor.ParallelTask<Integer>> tasks = new ArrayList<>();

            // 提交10个任务
            for (int i = 0; i < 10; i++) {
                final int taskId = i;
                tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                    @Override
                    public String getName() {
                        return "task-" + taskId;
                    }

                    @Override
                    public Integer execute() {
                        try {
                            Thread.sleep(100);
                            return taskId * 10;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            long startTime = System.currentTimeMillis();
            List<OptimizedParallelExecutor.ExecutionResult<Integer>> results =
                limitedExecutor.executeAll(tasks, 10000);
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(10, results.size());
            assertTrue(duration < 1000, "应该在1秒内完成（线程池复用）");

            log.info("✓ 资源限制通过：10个任务在2线程池中快速完成，耗时{}ms", duration);

        } finally {
            limitedExecutor.shutdown();
        }
    }

    @Test
    void testTimeoutControl() throws Exception {
        log.info("=== 测试超时控制 ===");

        List<OptimizedParallelExecutor.ParallelTask<String>> tasks = new ArrayList<>();

        // 添加一个慢任务
        tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
            @Override
            public String getName() {
                return "slow-task";
            }

            @Override
            public String execute() {
                try {
                    Thread.sleep(5000); // 5秒，超过超时
                    return "should-not-complete";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        });

        // 添加一个快任务
        tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
            @Override
            public String getName() {
                return "fast-task";
            }

            @Override
            public String execute() {
                try {
                    Thread.sleep(50);
                    return "fast-result";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        });

        long startTime = System.currentTimeMillis();
        List<OptimizedParallelExecutor.ExecutionResult<String>> results =
            executor.executeAll(tasks, 1000); // 1秒超时
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration < 1500, "应该在超时时间内返回");
        assertEquals(2, results.size());

        // 快任务应该成功
        boolean fastTaskSucceeded = results.get(1).isSuccess();
        assertTrue(fastTaskSucceeded, "快任务应该成功");

        // 慢任务应该被取消或失败
        boolean slowTaskCancelled = results.get(0).isCancelled() || !results.get(0).isSuccess();
        assertTrue(slowTaskCancelled, "慢任务应该被取消或失败");

        log.info("✓ 超时控制通过：慢任务被正确处理");
    }

    @Test
    void testExecutionStats() throws Exception {
        log.info("=== 测试执行统计 ===");

        // 执行一些任务以产生统计数据
        List<OptimizedParallelExecutor.ParallelTask<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                @Override
                public String getName() {
                    return "stats-task-" + taskId;
                }

                @Override
                public Integer execute() {
                    try {
                        Thread.sleep(50);
                        return taskId;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        executor.executeAll(tasks, 5000);

        OptimizedParallelExecutor.ExecutionStats stats = executor.getStats();

        assertNotNull(stats);
        assertNotNull(stats.config);
        assertTrue(stats.activeThreads >= 0, "活跃线程数应该 >= 0");
        assertTrue(stats.poolSize >= 0, "线程池大小应该 >= 0");
        assertTrue(stats.queueSize >= 0, "队列大小应该 >= 0");
        assertTrue(stats.completedTasks >= 3, "完成任务数应该 >= 3");

        log.info("✓ 执行统计通过：{}", stats);
    }

    @Test
    void testHighPerformanceConfig() throws Exception {
        log.info("=== 测试高性能配置 ===");

        ParallelExecutionConfig highPerfConfig = ParallelExecutionConfig.createHighPerformance();
        OptimizedParallelExecutor highPerfExecutor = new OptimizedParallelExecutor(highPerfConfig);

        try {
            List<OptimizedParallelExecutor.ParallelTask<Integer>> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                final int taskId = i;
                tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                    @Override
                    public String getName() {
                        return "hp-task-" + taskId;
                    }

                    @Override
                    public Integer execute() {
                        try {
                            Thread.sleep(10);
                            return taskId;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                });
            }

            long startTime = System.currentTimeMillis();
            List<OptimizedParallelExecutor.ExecutionResult<Integer>> results =
                highPerfExecutor.executeAll(tasks, 5000);
            long duration = System.currentTimeMillis() - startTime;

            assertEquals(20, results.size());
            assertTrue(duration < 500, "高性能配置应该快速完成20个任务");

            log.info("✓ 高性能配置通过：20个任务在{}ms内完成", duration);

        } finally {
            highPerfExecutor.shutdown();
        }
    }

    @Test
    void testConfigValidation() {
        log.info("=== 测试配置验证 ===");

        ParallelExecutionConfig config = ParallelExecutionConfig.createDefault();

        // 测试无效值
        assertThrows(IllegalArgumentException.class, () -> config.setMaxParallelThreads(0));
        assertThrows(IllegalArgumentException.class, () -> config.setThreadKeepAliveTime(0));
        assertThrows(IllegalArgumentException.class, () -> config.setMaxQueueSize(0));
        assertThrows(IllegalArgumentException.class, () -> config.setTimeoutPerStep(0));

        log.info("✓ 配置验证通过：无效值被正确拒绝");
    }

    @Test
    void testConcurrentExecution() throws Exception {
        log.info("=== 测试并发执行 ===");

        // 创建大量并发任务
        List<OptimizedParallelExecutor.ParallelTask<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            final int taskId = i;
            tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                @Override
                public String getName() {
                    return "concurrent-task-" + taskId;
                }

                @Override
                public Integer execute() {
                    try {
                        Thread.sleep(20);
                        return taskId;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        long startTime = System.currentTimeMillis();
        List<OptimizedParallelExecutor.ExecutionResult<Integer>> results =
            executor.executeAll(tasks, 10000);
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(50, results.size());
        assertTrue(duration < 2000, "50个任务应该在2秒内完成");

        // 验证所有任务都成功
        int successCount = 0;
        for (OptimizedParallelExecutor.ExecutionResult<Integer> result : results) {
            if (result.isSuccess()) {
                successCount++;
            }
        }

        assertEquals(50, successCount, "所有50个任务都应该成功");

        log.info("✓ 并发执行通过：50个任务在{}ms内完成，成功率100%", duration);
    }

    @Test
    void testGracefulShutdown() throws Exception {
        log.info("=== 测试优雅关闭 ===");

        OptimizedParallelExecutor tempExecutor = new OptimizedParallelExecutor(
            ParallelExecutionConfig.createDefault()
        );

        // 提交一些任务
        List<OptimizedParallelExecutor.ParallelTask<String>> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                @Override
                public String getName() {
                    return "shutdown-task-" + taskId;
                }

                @Override
                public String execute() {
                    try {
                        Thread.sleep(100);
                        return "done-" + taskId;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        tempExecutor.executeAll(tasks, 5000);

        // 测试关闭
        long shutdownStart = System.currentTimeMillis();
        tempExecutor.shutdown();
        long shutdownDuration = System.currentTimeMillis() - shutdownStart;

        assertTrue(shutdownDuration < 10000, "关闭应该在10秒内完成");
        log.info("✓ 优雅关闭通过：耗时{}ms", shutdownDuration);
    }

    @Test
    void testMixedTaskDurations() throws Exception {
        log.info("=== 测试混合任务时长 ===");

        List<OptimizedParallelExecutor.ParallelTask<String>> tasks = new ArrayList<>();

        // 添加不同时长的任务
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            final int sleepTime = (i + 1) * 50; // 50, 100, 150, 200, 250ms
            tasks.add(new OptimizedParallelExecutor.ParallelTask<>() {
                @Override
                public String getName() {
                    return "mixed-task-" + taskId;
                }

                @Override
                public String execute() {
                    try {
                        Thread.sleep(sleepTime);
                        return "duration-" + sleepTime;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        long startTime = System.currentTimeMillis();
        List<OptimizedParallelExecutor.ExecutionResult<String>> results =
            executor.executeAll(tasks, 5000);
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(5, results.size());

        // 最慢的任务是250ms，但由于并行，总时间应该小于500ms
        assertTrue(duration < 500, "并行执行应该快于最慢任务的单线程时间");

        log.info("✓ 混合时长任务通过：5个任务（不同时长）在{}ms内完成", duration);
    }
}
