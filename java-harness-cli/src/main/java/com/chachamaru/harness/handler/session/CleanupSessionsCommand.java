package com.chachamaru.harness.handler.session;

import com.chachamaru.harness.session.model.SessionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handler for /harness-cleanup-sessions command.
 *
 * <p>Cleans up old session saves with options for age threshold and keep count.</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class CleanupSessionsCommand extends BaseSessionCommand {

    private static final Logger logger = LoggerFactory.getLogger(CleanupSessionsCommand.class);

    @Override
    protected void executeCommand(String[] args) throws Exception {
        // Parse arguments
        int olderThanHours = parseOlderThanHours(args);
        int keepCount = parseKeepCount(args);
        boolean dryRun = parseDryRunFlag(args);

        logger.info("Cleaning up sessions: olderThan={}h, keep={}, dryRun={}", olderThanHours, keepCount, dryRun);

        // Get all sessions
        List<SessionMetadata> allSessions = storage.listSessions();

        if (allSessions.isEmpty()) {
            displayEmptyList();
            return;
        }

        // Determine which sessions to delete
        List<SessionMetadata> toDelete = findSessionsToDelete(allSessions, olderThanHours, keepCount);
        List<SessionMetadata> toKeep = findSessionsToKeep(allSessions, olderThanHours, keepCount);

        // Display cleanup plan
        displayCleanupPlan(toDelete, toKeep, olderThanHours, keepCount, dryRun);

        // Perform cleanup if not dry run
        if (!dryRun && !toDelete.isEmpty()) {
            performCleanup(toDelete);
        }
    }

    /**
     * Parse older-than hours from arguments
     */
    private int parseOlderThanHours(String[] args) {
        return parseIntValue(args, "--older-than", 168); // 7 days default
    }

    /**
     * Parse keep count from arguments
     */
    private int parseKeepCount(String[] args) {
        return parseIntValue(args, "--keep", 10);
    }

    /**
     * Parse dry-run flag from arguments
     */
    private boolean parseDryRunFlag(String[] args) {
        return parseFlag(args, "--dry-run", false);
    }

    /**
     * Find sessions that should be deleted
     */
    private List<SessionMetadata> findSessionsToDelete(List<SessionMetadata> allSessions, int olderThanHours, int keepCount) {
        Instant cutoffTime = Instant.now().minus(olderThanHours, ChronoUnit.HOURS);

        return allSessions.stream()
                .filter(session -> {
                    // Delete if older than cutoff AND not in protected recent sessions
                    boolean isOld = session.getTimestamp().isBefore(cutoffTime);
                    boolean isProtected = allSessions.indexOf(session) < keepCount;
                    return isOld && !isProtected;
                })
                .collect(Collectors.toList());
    }

    /**
     * Find sessions that should be kept
     */
    private List<SessionMetadata> findSessionsToKeep(List<SessionMetadata> allSessions, int olderThanHours, int keepCount) {
        Instant cutoffTime = Instant.now().minus(olderThanHours, ChronoUnit.HOURS);

        return allSessions.stream()
                .filter(session -> {
                    // Keep if recent OR in protected recent sessions
                    boolean isRecent = !session.getTimestamp().isBefore(cutoffTime);
                    boolean isProtected = allSessions.indexOf(session) < keepCount;
                    return isRecent || isProtected;
                })
                .collect(Collectors.toList());
    }

    /**
     * Display cleanup plan
     */
    private void displayCleanupPlan(List<SessionMetadata> toDelete, List<SessionMetadata> toKeep,
                                   int olderThanHours, int keepCount, boolean dryRun) {
        System.out.println("\n🧹 会话清理操作");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        System.out.println("\n清理条件:");
        System.out.println("- 时间限制: 超过" + olderThanHours + "小时的会话");
        System.out.println("- 保留数量: 最近" + keepCount + "个会话");
        System.out.println("- 操作模式: " + (dryRun ? "预览模式 (dry-run)" : "实际删除"));

        System.out.println("\n扫描结果:");
        System.out.println("总会话数: " + (toDelete.size() + toKeep.size()) + "个");
        System.out.println("符合删除条件: " + toDelete.size() + "个");
        System.out.println("受保护会话: " + toKeep.size() + "个");

        if (!toDelete.isEmpty()) {
            System.out.println("\n将要删除的会话:");
            for (int i = 0; i < toDelete.size(); i++) {
                SessionMetadata session = toDelete.get(i);
                System.out.println((i + 1) + ". " + session.getSaveId() + " (" + formatRelativeTime(session.getTimestamp()) + ")");
            }

            // Calculate estimated space savings
            long estimatedSavings = toDelete.stream()
                    .mapToLong(this::estimateSessionSize)
                    .sum();
            System.out.println("\n释放空间: 约 " + formatBytes(estimatedSavings));

            if (dryRun) {
                System.out.println("\n确认删除？移除 --dry-run 参数执行实际删除");
            }
        } else {
            System.out.println("\n✅ 无需删除的会话");
        }

        System.out.println();
    }

    /**
     * Perform actual cleanup
     */
    private void performCleanup(List<SessionMetadata> toDelete) {
        System.out.println("正在执行清理...");

        int successCount = 0;
        int failCount = 0;

        for (SessionMetadata session : toDelete) {
            try {
                // Delete session from storage
                boolean deleted = storage.deleteSession(session.getSaveId());

                if (deleted) {
                    successCount++;
                    logger.info("Deleted session: {}", session.getSaveId());
                } else {
                    failCount++;
                    logger.warn("Failed to delete session: {}", session.getSaveId());
                }
            } catch (Exception e) {
                failCount++;
                logger.error("Error deleting session: {}", session.getSaveId(), e);
            }
        }

        System.out.println("\n清理完成:");
        System.out.println("成功删除: " + successCount + "个会话");
        if (failCount > 0) {
            System.out.println("删除失败: " + failCount + "个会话");
        }
        System.out.println();
    }

    /**
     * Display empty list message
     */
    private void displayEmptyList() {
        System.out.println("\n🧹 会话清理操作");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("暂无已保存的会话，无需清理");
        System.out.println();
    }

    /**
     * Format relative time string
     */
    private String formatRelativeTime(Instant timestamp) {
        Instant now = Instant.now();
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);

        if (hours < 1) {
            return "不到1小时前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else if (days < 7) {
            return days + "天前";
        } else {
            long weeks = days / 7;
            return weeks + "周前";
        }
    }

    /**
     * Estimate session size (rough approximation)
     */
    private long estimateSessionSize(SessionMetadata session) {
        // Rough estimation: Token count * 100 bytes + metadata overhead
        return (long) session.getTokenUsage() * 100 + 1024; // Add 1KB overhead
    }

    /**
     * Format bytes for display
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}