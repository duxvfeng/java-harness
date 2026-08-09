package com.chachamaru.harness.handler.session;

import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSaveResult;
import com.chachamaru.harness.session.manager.SessionSaveManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for /harness-save-session command.
 *
 * <p>Saves current session state to local storage with optional summary and force flag.</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class SaveSessionCommand extends BaseSessionCommand {

    private static final Logger logger = LoggerFactory.getLogger(SaveSessionCommand.class);

    @Override
    protected void executeCommand(String[] args) throws Exception {
        // Parse arguments
        String summary = parseSummary(args);
        boolean force = parseForceFlag(args);

        logger.info("Saving session with summary: '{}', force: {}", summary, force);

        // Create session context (simplified for now)
        java.util.Map<String, Object> sessionData = new java.util.HashMap<>();

        // Create minimal metadata for the save
        SessionMetadata.TaskContext taskContext = new SessionMetadata.TaskContext(
                "Unknown",  // currentPhase
                java.util.Collections.emptyList(),  // completedTasks
                "Manual save",  // currentTask
                0  // totalTasks
        );

        SessionMetadata.GitState gitState = new SessionMetadata.GitState(
                "master",  // branch
                "unknown",  // commit
                0,  // modifiedFiles
                false  // uncommittedChanges
        );

        SessionMetadata.SaveSize saveSize = new SessionMetadata.SaveSize(
                0,  // totalFiles
                "0B",  // compressedSize
                "0B"  // uncompressedSize
        );

        SessionMetadata metadata = new SessionMetadata(
                "pending",  // saveId (will be generated)
                java.time.Instant.now(),  // timestamp
                summary,  // saveReason
                85,  // tokenUsage (placeholder)
                taskContext,  // taskContext
                gitState,  // gitState
                summary,  // summary
                saveSize  // size
        );

        SessionSaveManager.SessionContext context = new SessionSaveManager.SessionContext(sessionData, metadata);

        // Execute save
        SessionSaveResult result;
        if (force) {
            result = saveManager.forceSave(context, summary);
        } else {
            result = saveManager.saveSession(context, summary);
        }

        // Handle result
        if (result.isSuccess()) {
            displaySuccess(result);
        } else {
            displayFailure(result);
        }
    }

    /**
     * Parse session summary from arguments
     */
    private String parseSummary(String[] args) {
        // First non-flag argument is treated as summary
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                return arg;
            }
        }
        return "会话保存 - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    /**
     * Parse force flag from arguments
     */
    private boolean parseForceFlag(String[] args) {
        return parseFlag(args, "--force", false);
    }

    /**
     * Display successful save result
     */
    private void displaySuccess(SessionSaveResult result) {
        System.out.println("\n✅ 会话保存成功");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("保存ID: " + result.getSaveId());
        System.out.println("时间: " + formatTimestamp(result.getTimestamp()));
        System.out.println("消息: " + result.getMessage());
        if (result.getSize() != null) {
            System.out.println("数据大小: " + formatBytes(result.getSize()));
        }
        System.out.println();
    }

    /**
     * Display failed save result
     */
    private void displayFailure(SessionSaveResult result) {
        System.err.println("\n❌ 会话保存失败");
        System.err.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.err.println("错误: " + (result.getErrorMessage() != null ? result.getErrorMessage() : "未知错误"));

        System.err.println();
        System.err.println("💡 提示:");
        System.err.println("  1. 检查磁盘空间是否充足");
        System.err.println("  2. 验证目录权限是否正确");
        System.err.println("  3. 使用 --force 绕过保存间隔限制");
        System.err.println();
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(java.time.Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
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