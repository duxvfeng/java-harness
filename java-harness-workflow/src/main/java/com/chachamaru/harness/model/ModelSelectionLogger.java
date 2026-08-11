package com.chachamaru.harness.model;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能模型选择系统日志记录器
 * 提供结构化的日志记录和监控功能
 *
 * <p>主要功能：</p>
 * <ul>
 *   <li>结构化日志记录（INFO、WARN、ERROR、DEBUG级别）</li>
 *   <li>性能监控和统计</li>
 *   <li>错误追踪和分析</li>
 *   <li>日志文件持久化</li>
 *   <li>实时监控指标</li>
 * </ul>
 *
 * <p>日志级别：</p>
 * <ul>
 *   <li>DEBUG: 详细的调试信息</li>
 *   <li>INFO: 一般信息性消息</li>
 *   <li>WARN: 警告消息（可恢复的异常）</li>
 *   <li>ERROR: 错误消息（影响功能的异常）</li>
 * </ul>
 */
public class ModelSelectionLogger {

    private static final String LOG_DIR = ".claude/logs";
    private static final String LOG_FILE = "model-selection.log";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static volatile ModelSelectionLogger instance;
    private final boolean enableFileLogging;
    private final boolean enableConsoleLogging;

    // 监控统计
    private final AtomicLong totalSelections = new AtomicLong(0);
    private final AtomicLong successfulSelections = new AtomicLong(0);
    private final AtomicLong failedSelections = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    /**
     * 获取日志记录器实例
     *
     * @return 日志记录器实例
     */
    public static ModelSelectionLogger getInstance() {
        if (instance == null) {
            synchronized (ModelSelectionLogger.class) {
                if (instance == null) {
                    instance = new ModelSelectionLogger(true, false);
                }
            }
        }
        return instance;
    }

    /**
     * 创建日志记录器
     */
    public ModelSelectionLogger() {
        this(true, false);
    }

    /**
     * 创建日志记录器
     *
     * @param enableFileLogging 是否启用文件日志
     * @param enableConsoleLogging 是否启用控制台日志
     */
    public ModelSelectionLogger(boolean enableFileLogging, boolean enableConsoleLogging) {
        this.enableFileLogging = enableFileLogging;
        this.enableConsoleLogging = enableConsoleLogging;

        if (enableFileLogging) {
            ensureLogDirectory();
        }
    }

    /**
     * 记录 INFO 级别日志
     *
     * @param message 日志消息
     */
    public void info(String message) {
        log("INFO", message, null);
    }

    /**
     * 记录 WARN 级别日志
     *
     * @param message 警告消息
     */
    public void warn(String message) {
        log("WARN", message, null);
    }

    /**
     * 记录 ERROR 级别日志
     *
     * @param message 错误消息
     */
    public void error(String message) {
        log("ERROR", message, null);
        trackError(message);
    }

    /**
     * 记录 ERROR 级别日志（带异常）
     *
     * @param message 错误消息
     * @param throwable 异常对象
     */
    public void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
        trackError(message);
    }

    /**
     * 记录 DEBUG 级别日志
     *
     * @param message 调试消息
     */
    public void debug(String message) {
        log("DEBUG", message, null);
    }

    /**
     * 记录模型选择事件
     *
     * @param complexityScore 复杂度分数
     * @param selectedModel 选择的模型
     * @param selectionTimeMs 选择耗时（毫秒）
     */
    public void logModelSelection(int complexityScore, String selectedModel, long selectionTimeMs) {
        totalSelections.incrementAndGet();
        if (selectedModel != null && !selectedModel.isEmpty()) {
            successfulSelections.incrementAndGet();
            info(String.format("Model selected: score=%d, model=%s, time=%dms",
                complexityScore, selectedModel, selectionTimeMs));
        } else {
            failedSelections.incrementAndGet();
            error(String.format("Model selection failed: score=%d", complexityScore));
        }
    }

    /**
     * 记录缓存事件
     *
     * @param hit 是否命中缓存
     * @param cacheType 缓存类型
     */
    public void logCacheEvent(boolean hit, String cacheType) {
        if (hit) {
            cacheHits.incrementAndGet();
            debug("Cache hit: " + cacheType);
        } else {
            cacheMisses.incrementAndGet();
            debug("Cache miss: " + cacheType);
        }
    }

    /**
     * 记录配置加载事件
     *
     * @param source 配置来源
     * @param success 是否成功
     */
    public void logConfigLoading(String source, boolean success) {
        if (success) {
            info("Configuration loaded successfully from: " + source);
        } else {
            warn("Configuration loading failed from: " + source + ", using defaults");
        }
    }

    /**
     * 记录降级链执行事件
     *
     * @param tier 模型等级
     * @param attempts 尝试次数
     * @param success 是否成功
     */
    public void logFallbackChainExecution(ModelTier tier, int attempts, boolean success) {
        if (success) {
            info(String.format("Fallback chain succeeded: tier=%s, attempts=%d", tier, attempts));
        } else {
            error(String.format("Fallback chain exhausted: tier=%s, attempts=%d", tier, attempts));
        }
    }

    /**
     * 获取监控统计信息
     *
     * @return 统计信息字符串
     */
    public String getStatistics() {
        long total = totalSelections.get();
        long successful = successfulSelections.get();
        long failed = failedSelections.get();
        long hits = cacheHits.get();
        long misses = cacheMisses.get();

        double successRate = total > 0 ? (double) successful / total * 100.0 : 0.0;
        double cacheHitRate = (hits + misses) > 0 ? (double) hits / (hits + misses) * 100.0 : 0.0;

        return String.format(
            "Model Selection Statistics:\n" +
            "  Total Selections: %d\n" +
            "  Successful: %d (%.1f%%)\n" +
            "  Failed: %d (%.1f%%)\n" +
            "  Cache Hits: %d\n" +
            "  Cache Misses: %d\n" +
            "  Cache Hit Rate: %.1f%%\n" +
            "  Error Types: %d",
            total, successful, successRate, failed, (100.0 - successRate),
            hits, misses, cacheHitRate, errorCounts.size()
        );
    }

    /**
     * 重置统计信息
     */
    public void resetStatistics() {
        totalSelections.set(0);
        successfulSelections.set(0);
        failedSelections.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        errorCounts.clear();
        info("Statistics reset");
    }

    /**
     * 记录日志（核心方法）
     *
     * @param level 日志级别
     * @param message 日志消息
     * @param throwable 异常对象（可选）
     */
    private void log(String level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String logMessage = String.format("[%s] [%s] [%s] %s",
            timestamp, level, "ModelSelection", message);

        // 添加异常信息
        if (throwable != null) {
            logMessage += " | " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
        }

        // 控制台输出
        if (enableConsoleLogging) {
            System.out.println(logMessage);
            if (throwable != null && enableConsoleLogging) {
                throwable.printStackTrace(System.out);
            }
        }

        // 文件输出
        if (enableFileLogging) {
            writeToFile(logMessage);
            if (throwable != null) {
                writeToFile(throwable.toString());
            }
        }
    }

    /**
     * 跟踪错误
     *
     * @param message 错误消息
     */
    private void trackError(String message) {
        String errorType = extractErrorType(message);
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 提取错误类型
     *
     * @param message 错误消息
     * @return 错误类型
     */
    private String extractErrorType(String message) {
        if (message == null || message.isEmpty()) {
            return "unknown";
        }

        String lower = message.toLowerCase();
        if (lower.contains("config")) return "config_error";
        if (lower.contains("model") && lower.contains("unavailable")) return "model_unavailable";
        if (lower.contains("timeout")) return "timeout";
        if (lower.contains("network")) return "network_error";
        if (lower.contains("parse") || lower.contains("format")) return "parse_error";

        return "other_error";
    }

    /**
     * 写入日志文件
     *
     * @param message 日志消息
     */
    private void writeToFile(String message) {
        try {
            Path logPath = Paths.get(LOG_DIR, LOG_FILE);
            Files.writeString(logPath, message + "\n",
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 避免递归日志错误，只在控制台输出
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * 确保日志目录存在
     */
    private void ensureLogDirectory() {
        try {
            Path logDirPath = Paths.get(LOG_DIR);
            if (!Files.exists(logDirPath)) {
                Files.createDirectories(logDirPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }

    /**
     * 清理旧日志文件
     *
     * @param daysToKeep 保留天数
     */
    public void cleanupOldLogs(int daysToKeep) {
        // 简化实现：这里可以添加日志清理逻辑
        info("Old log cleanup requested (keep " + daysToKeep + " days)");
    }
}