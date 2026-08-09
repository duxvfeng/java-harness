package com.chachamaru.harness.handler.session;

import com.chachamaru.harness.session.model.SessionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Handler for /harness-list-sessions command.
 *
 * <p>Lists all saved sessions with options for recent and all listings.</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class ListSessionsCommand extends BaseSessionCommand {

    private static final Logger logger = LoggerFactory.getLogger(ListSessionsCommand.class);

    @Override
    protected void executeCommand(String[] args) throws Exception {
        // Parse arguments
        int recentCount = parseRecentCount(args);
        boolean showAll = parseShowAllFlag(args);

        logger.info("Listing sessions: recent={}, all={}", recentCount, showAll);

        // Get session list
        List<SessionMetadata> sessions = storage.listSessions();

        if (sessions.isEmpty()) {
            displayEmptyList();
            return;
        }

        // Filter sessions based on arguments
        List<SessionMetadata> displaySessions = filterSessions(sessions, recentCount, showAll);

        // Display sessions
        displaySessions(displaySessions);
    }

    /**
     * Parse recent count from arguments
     */
    private int parseRecentCount(String[] args) {
        return parseIntValue(args, "--recent", 5);
    }

    /**
     * Parse show-all flag from arguments
     */
    private boolean parseShowAllFlag(String[] args) {
        return parseFlag(args, "--all", false);
    }

    /**
     * Filter sessions based on display options
     */
    private List<SessionMetadata> filterSessions(List<SessionMetadata> sessions, int recentCount, boolean showAll) {
        if (showAll) {
            return sessions;
        } else {
            int count = Math.min(recentCount, sessions.size());
            return sessions.subList(0, count);
        }
    }

    /**
     * Display list of sessions
     */
    private void displaySessions(List<SessionMetadata> sessions) {
        String displayMode = sessions.size() == storage.listSessions().size() ? "所有" : "最近" + sessions.size();
        System.out.println("\n💾 已保存的会话 (" + displayMode + "个)");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (int i = 0; i < sessions.size(); i++) {
            SessionMetadata session = sessions.get(i);
            displaySessionItem(i + 1, session);
        }

        System.out.println();
        System.out.println("总计: " + sessions.size() + " 个会话");
        System.out.println("使用 /harness-show-session <id> 查看详细信息");
        System.out.println("使用 /harness-restore-session <id> 恢复会话");
        System.out.println();
    }

    /**
     * Display individual session item
     */
    private void displaySessionItem(int index, SessionMetadata session) {
        System.out.println("\n" + index + ". " + session.getSaveId());

        // Time information
        String timeStr = formatRelativeTime(session.getTimestamp());
        String exactTime = LocalDateTime.ofInstant(session.getTimestamp(), java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("   时间: " + exactTime + " (" + timeStr + ")");

        // Summary
        if (session.getSaveReason() != null && !session.getSaveReason().isEmpty()) {
            System.out.println("   保存原因: " + session.getSaveReason());
        }
        if (session.getSummary() != null && !session.getSummary().isEmpty()) {
            System.out.println("   会话摘要: " + session.getSummary());
        }

        // Token usage
        System.out.println("   Token使用率: " + session.getTokenUsage() + "%");

        // Task progress
        if (session.getTaskContext() != null) {
            SessionMetadata.TaskContext taskContext = session.getTaskContext();
            System.out.println("   任务进度: " + taskContext.getCurrentPhase() + ", " +
                             taskContext.getCurrentTask() + "/" + taskContext.getTotalTasks());
        }
    }

    /**
     * Display empty list message
     */
    private void displayEmptyList() {
        System.out.println("\n💾 已保存的会话");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("暂无已保存的会话");
        System.out.println();
        System.out.println("💡 提示:");
        System.out.println("  使用 /harness-save-session 保存当前会话");
        System.out.println("  自动保存功能将在Token使用率高时自动触发");
        System.out.println();
    }

    /**
     * Format relative time string
     */
    private String formatRelativeTime(java.time.Instant timestamp) {
        java.time.Instant now = java.time.Instant.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);

        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else if (days < 7) {
            return days + "天前";
        } else {
            long weeks = days / 7;
            return weeks + "周前";
        }
    }
}