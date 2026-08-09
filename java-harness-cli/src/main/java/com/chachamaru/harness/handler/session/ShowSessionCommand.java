package com.chachamaru.harness.handler.session;

import com.chachamaru.harness.session.model.SessionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Handler for /harness-show-session command.
 *
 * <p>Shows detailed information about a specific saved session.</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class ShowSessionCommand extends BaseSessionCommand {

    private static final Logger logger = LoggerFactory.getLogger(ShowSessionCommand.class);

    @Override
    protected void executeCommand(String[] args) throws Exception {
        // Parse arguments
        String saveId = parseSaveId(args);

        if (saveId == null || saveId.isEmpty()) {
            throw new IllegalArgumentException("保存ID不能为空");
        }

        logger.info("Showing session details: {}", saveId);

        // Load session metadata
        Optional<SessionMetadata> metadataOpt = storage.loadMetadata(saveId);

        if (metadataOpt.isEmpty()) {
            displayNotFound(saveId);
            return;
        }

        SessionMetadata metadata = metadataOpt.get();

        // Display detailed information
        displaySessionDetails(saveId, metadata);
    }

    /**
     * Parse save ID from arguments
     */
    private String parseSaveId(String[] args) {
        // First non-flag argument is treated as save ID
        for (String arg : args) {
            if (!arg.startsWith("--")) {
                return arg;
            }
        }
        return null;
    }

    /**
     * Display detailed session information
     */
    private void displaySessionDetails(String saveId, SessionMetadata metadata) {
        System.out.println("\n📄 会话详细信息: " + saveId);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Basic information
        System.out.println("\n基本信息:");
        System.out.println("保存ID: " + metadata.getSaveId());
        System.out.println("保存时间: " + formatTimestamp(metadata.getTimestamp()));
        if (metadata.getSaveReason() != null && !metadata.getSaveReason().isEmpty()) {
            System.out.println("保存原因: " + metadata.getSaveReason());
        }
        if (metadata.getSummary() != null && !metadata.getSummary().isEmpty()) {
            System.out.println("会话摘要: " + metadata.getSummary());
        }
        System.out.println("Token使用率: " + metadata.getTokenUsage() + "%");

        // Task state
        if (metadata.getTaskContext() != null) {
            System.out.println("\n任务状态:");
            SessionMetadata.TaskContext taskContext = metadata.getTaskContext();
            System.out.println("当前阶段: " + taskContext.getCurrentPhase());
            System.out.println("当前任务: " + taskContext.getCurrentTask());
            System.out.println("总任务数: " + taskContext.getTotalTasks());
            if (taskContext.getCompletedTasks() != null && !taskContext.getCompletedTasks().isEmpty()) {
                System.out.println("已完成任务: " + String.join(", ", taskContext.getCompletedTasks()));
            }
        }

        // Git state
        if (metadata.getGitState() != null) {
            System.out.println("\nGit状态:");
            SessionMetadata.GitState gitState = metadata.getGitState();
            System.out.println("分支: " + gitState.getBranch());
            System.out.println("最新提交: " + gitState.getCommit());
            System.out.println("未提交修改: " + (gitState.hasUncommittedChanges() ? "是" : "否"));
            System.out.println("修改文件数: " + gitState.getModifiedFiles());
        }

        // Size information
        if (metadata.getSize() != null) {
            System.out.println("\n存储信息:");
            System.out.println("未压缩大小: " + metadata.getSize().getUncompressedSize());
            System.out.println("压缩大小: " + metadata.getSize().getCompressedSize());
            System.out.println("文件数量: " + metadata.getSize().getTotalFiles());
        }

        // Storage path
        System.out.println("存储路径: .claude/state/session-saves/" + metadata.getSaveId() + "/");

        // Integrity check
        boolean isValid = restoreManager.validateSaveIntegrity(metadata.getSaveId());
        System.out.println("完整性: " + (isValid ? "✅ 验证通过" : "❌ 验证失败"));

        System.out.println();
    }

    /**
     * Display session not found error
     */
    private void displayNotFound(String saveId) {
        System.err.println("\n❌ 会话不存在");
        System.err.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.err.println("保存ID: " + saveId);
        System.err.println();
        System.err.println("💡 提示:");
        System.err.println("  1. 使用 /harness-list-sessions 查看可用的会话");
        System.err.println("  2. 确认保存ID正确无误");
        System.err.println("  3. 检查会话文件是否被删除");
        System.err.println();
    }

    /**
     * Format timestamp for display
     */
    private String formatTimestamp(java.time.Instant timestamp) {
        return LocalDateTime.ofInstant(timestamp, java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}