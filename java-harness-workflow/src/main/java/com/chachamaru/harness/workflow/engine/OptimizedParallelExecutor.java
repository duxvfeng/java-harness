package com.chachamaru.harness.workflow.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 优化的并行执行引擎
 * 提供更好的资源管理、错误隔离和性能监控
 */
public class OptimizedParallelExecutor {

    private static final Logger log = LoggerFactory.getLogger(OptimizedParallelExecutor.class);

    private final ExecutorService executorService;
    private final ParallelExecutionConfig config;
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public OptimizedParallelExecutor(ParallelExecutionConfig config) {
        this.config = config;
        this.executorService = createOptimizedExecutorService(config);
    }

    /**
     * 创建优化的线程池
     */
    private ExecutorService createOptimizedExecutorService(ParallelExecutionConfig config) {
        // 使用有界队列防止资源耗尽
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(config.getMaxQueueSize());

        // 自定义线程工厂，设置有意义的线程名
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("workflow-parallel-" + activeTasks.incrementAndGet());
            thread.setDaemon(false); // 工作线程应该是非守护线程
            return thread;
        };

        return new ThreadPoolExecutor(
            config.getMaxParallelThreads(), // 核心线程数
            config.getMaxParallelThreads(), // 最大线程数
            config.getThreadKeepAliveTime(), // 空闲线程存活时间
            TimeUnit.SECONDS,
            workQueue,
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时由调用线程执行
        );
    }

    /**
     * 并行执行多个任务，带有错误隔离和超时控制
     */
    public <T> List<ExecutionResult<T>> executeAll(
        List<ParallelTask<T>> tasks,
        long timeoutMs
    ) {
        List<ExecutionResult<T>> results = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) {
            return results;
        }

        log.info("Executing {} parallel tasks with config: {}", tasks.size(), config.getSummary());

        try {
            List<CompletableFuture<ExecutionResult<T>>> futures = new ArrayList<>();

            // 提交所有任务
            for (ParallelTask<T> task : tasks) {
                CompletableFuture<ExecutionResult<T>> future =
                    CompletableFuture.supplyAsync(() -> executeTaskWithTimeout(task), executorService);
                futures.add(future);
            }

            // 等待所有任务完成（带超时）
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.warn("Parallel execution timeout after {}ms", timeoutMs);
                // 取消未完成的任务
                futures.forEach(future -> future.cancel(true));
            }

            // 收集结果
            for (CompletableFuture<ExecutionResult<T>> future : futures) {
                try {
                    if (!future.isCancelled()) {
                        results.add(future.get());
                    } else {
                        // 任务被取消（超时）
                        results.add(ExecutionResult.cancelled("Execution timeout"));
                    }
                } catch (Exception e) {
                    results.add(ExecutionResult.failed("Collection error: " + e.getMessage()));
                }
            }

        } catch (Exception e) {
            log.error("Error in parallel execution", e);
        }

        return results;
    }

    /**
     * 执行单个任务（带超时控制）
     */
    private <T> ExecutionResult<T> executeTaskWithTimeout(ParallelTask<T> task) {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("Executing task: {}", task.getName());

            // 执行任务
            T result = task.execute();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Task {} completed in {}ms", task.getName(), duration);

            return ExecutionResult.success(result, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Task {} failed after {}ms: {}", task.getName(), duration, e.getMessage());
            return ExecutionResult.failed("Task failed: " + e.getMessage());
        }
    }

    /**
     * 获取活跃线程数
     */
    public int getActiveThreadCount() {
        if (executorService instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) executorService).getActiveCount();
        }
        return -1;
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        if (executorService instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) executorService).getQueue().size();
        }
        return -1;
    }

    /**
     * 获取执行统计信息
     */
    public ExecutionStats getStats() {
        ExecutionStats stats = new ExecutionStats();
        stats.config = config.getSummary();

        if (executorService instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) executorService;
            stats.activeThreads = tpe.getActiveCount();
            stats.poolSize = tpe.getPoolSize();
            stats.queueSize = tpe.getQueue().size();
            stats.completedTasks = tpe.getCompletedTaskCount();
        }

        return stats;
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        log.info("Shutting down parallel executor");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Executor did not terminate in 30s, forcing shutdown");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Executor did not terminate after forced shutdown");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 执行结果封装
     */
    public static class ExecutionResult<T> {
        private final boolean success;
        private final T result;
        private final String errorMessage;
        private final long duration;
        private final boolean cancelled;

        private ExecutionResult(boolean success, T result, String errorMessage, long duration, boolean cancelled) {
            this.success = success;
            this.result = result;
            this.errorMessage = errorMessage;
            this.duration = duration;
            this.cancelled = cancelled;
        }

        public static <T> ExecutionResult<T> success(T result, long duration) {
            return new ExecutionResult<>(true, result, null, duration, false);
        }

        public static <T> ExecutionResult<T> failed(String errorMessage) {
            return new ExecutionResult<>(false, null, errorMessage, 0, false);
        }

        public static <T> ExecutionResult<T> cancelled(String errorMessage) {
            return new ExecutionResult<>(false, null, errorMessage, 0, true);
        }

        public boolean isSuccess() {
            return success;
        }

        public T getResult() {
            return result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getDuration() {
            return duration;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }

    /**
     * 并行任务接口
     */
    public interface ParallelTask<T> {
        String getName();
        T execute() throws Exception;
    }

    /**
     * 执行统计信息
     */
    public static class ExecutionStats {
        public String config;
        public int activeThreads;
        public int poolSize;
        public int queueSize;
        public long completedTasks;

        @Override
        public String toString() {
            return String.format(
                "ExecutionStats{config='%s', activeThreads=%d, poolSize=%d, queueSize=%d, completedTasks=%d}",
                config, activeThreads, poolSize, queueSize, completedTasks
            );
        }
    }
}
