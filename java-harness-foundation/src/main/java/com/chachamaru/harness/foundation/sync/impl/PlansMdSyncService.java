package com.chachamaru.harness.foundation.sync.impl;

import com.chachamaru.harness.foundation.sync.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plans.md 同步服务实现
 * 提供状态与 Plans.md 之间的双向同步功能
 */
public class PlansMdSyncService implements StateSynchronizationEngine {

    private static final Logger logger = LoggerFactory.getLogger(PlansMdSyncService.class);
    // 匹配 Plans.md 中的任务状态行（6列格式：Task | 内容 | DoD | Depends | Status）
    private static final Pattern STATUS_PATTERN = Pattern.compile(
        "\\|\\s*(\\d+\\.\\d+\\.\\d+)\\s*\\|[^\\|]+\\|[^\\|]+\\|[^\\|]+\\|([^\\|\\n]+)\\|"
    );
    private static final int MAX_TASK_ID_LENGTH = 50; // 防止 ReDoS 攻击

    @Override
    public SyncResult syncToPlans(StateSnapshot state, Path plansMdPath) throws SyncException {
        validateInputs(state, plansMdPath);

        try {
            // 原子读取：使用 try-with-resources 和同步访问
            String content;
            synchronized (this) {
                if (!Files.exists(plansMdPath)) {
                    throw new SyncException("Plans.md file not found: " + plansMdPath,
                            SyncException.ErrorType.READ_ERROR);
                }
                content = Files.readString(plansMdPath);
            }

            List<TaskSyncChange> changes = new ArrayList<>();
            String originalContent = content;
            Map<String, Integer> lineOccurrenceCounter = new HashMap<>();

            for (TaskState taskState : state.getTaskStates().values()) {
                String taskId = taskState.getTaskId();

                // 验证 taskId 长度防止 ReDoS
                if (taskId.length() > MAX_TASK_ID_LENGTH) {
                    logger.warn("Task ID too long, skipping: {}", taskId);
                    continue;
                }

                String newStatus = taskState.getStatus();

                // 使用更安全的行定位策略：行号 + 任务ID
                Optional<int[]> lineInfo = findTaskLine(content, taskId);
                if (lineInfo.isPresent()) {
                    int[] positions = lineInfo.get();
                    int lineStart = positions[0];
                    int lineEnd = positions[1];
                    String oldLine = content.substring(lineStart, lineEnd);

                    // 使用行号跟踪避免重复替换
                    String lineKey = taskId + ":" + lineStart;
                    int occurrence = lineOccurrenceCounter.getOrDefault(lineKey, 0);
                    lineOccurrenceCounter.put(lineKey, occurrence + 1);

                    if (occurrence == 0) { // 只替换第一次出现
                        String newLine = replaceStatusInLine(oldLine, newStatus);
                        if (!oldLine.equals(newLine)) {
                            content = content.substring(0, lineStart) + newLine + content.substring(lineEnd);
                            changes.add(new TaskSyncChange(taskId,
                                    TaskSyncChange.ChangeType.STATUS_CHANGED,
                                    extractStatus(oldLine), newStatus));
                        }
                    } else {
                        logger.warn("Duplicate task line detected: {} at position {}", taskId, lineStart);
                    }
                } else {
                    // 任务行不存在，追加新行到表格末尾
                    String newRow = String.format("| %s | | | - | %s |", taskId, newStatus);
                    content = appendAfterLastTableRow(content, newRow);
                    changes.add(new TaskSyncChange(taskId,
                            TaskSyncChange.ChangeType.STATUS_CHANGED,
                            null, newStatus));
                }
            }

            // 原子写入
            if (!content.equals(originalContent)) {
                synchronized (this) {
                    // 写入前再次检查文件是否存在（TOCTOU 保护）
                    if (!Files.exists(plansMdPath)) {
                        throw new SyncException("Plans.md was deleted during sync",
                                SyncException.ErrorType.WRITE_ERROR);
                    }
                    Files.writeString(plansMdPath, content);
                }
                logger.info("Synced {} changes to Plans.md", changes.size());
            }

            String newHash = calculateHash(content);
            return SyncResult.success(changes, newHash);

        } catch (IOException e) {
            throw new SyncException("Failed to sync to Plans.md: " + plansMdPath,
                    SyncException.ErrorType.WRITE_ERROR, e);
        }
    }

    @Override
    public SyncResult syncFromPlans(Path plansMdPath) throws SyncException {
        validatePath(plansMdPath);

        try {
            String content;
            synchronized (this) {
                if (!Files.exists(plansMdPath)) {
                    throw new SyncException("Plans.md file not found: " + plansMdPath,
                            SyncException.ErrorType.READ_ERROR);
                }
                content = Files.readString(plansMdPath);
            }

            Map<String, TaskState> taskStates = new HashMap<>();
            List<TaskSyncChange> changes = new ArrayList<>();

            // 解析所有任务状态
            Matcher matcher = STATUS_PATTERN.matcher(content);
            while (matcher.find()) {
                String taskId = matcher.group(1); // 任务ID
                String status = matcher.group(2).trim(); // 状态

                // 过滤掉表头行
                if (!status.isEmpty() && !status.equals("Status") && !status.equals("DoD") && !status.equals("Depends")) {
                    TaskState taskState = new TaskState(taskId, status, "Synced from Plans.md");
                    taskStates.put(taskId, taskState);

                    changes.add(new TaskSyncChange(taskId,
                            TaskSyncChange.ChangeType.STATUS_CHANGED,
                            null, status));
                }
            }

            logger.info("Synced {} tasks from Plans.md", taskStates.size());

            String hash = calculateHash(content);
            StateSnapshot snapshot = new StateSnapshot(taskStates, "sync-from-plans");

            logger.info("Synced {} tasks from Plans.md", taskStates.size());
            return SyncResult.success(changes, hash);

        } catch (IOException e) {
            throw new SyncException("Failed to sync from Plans.md: " + plansMdPath,
                    SyncException.ErrorType.READ_ERROR, e);
        }
    }

    @Override
    public SyncResult bidirectionalSync(StateSnapshot state, Path plansMdPath) throws SyncException {
        validateInputs(state, plansMdPath);

        try {
            // 1. 从 Plans.md 读取当前状态
            SyncResult fromPlansResult = syncFromPlans(plansMdPath);
            Map<String, TaskState> plansTaskStates = new HashMap<>();

            for (TaskSyncChange change : fromPlansResult.getChanges()) {
                TaskState taskState = new TaskState(
                        change.getTaskId(),
                        change.getNewValue(),
                        "From Plans.md"
                );
                plansTaskStates.put(change.getTaskId(), taskState);
            }

            // 2. 检测冲突
            List<SyncConflict> conflicts = detectConflicts(state, plansTaskStates);

            // 3. 合并状态（Plans.md 优先，但记录被丢弃的本地变更）
            Map<String, TaskState> mergedStates = new HashMap<>(plansTaskStates);
            List<TaskSyncChange> discardedChanges = new ArrayList<>();

            for (TaskState localState : state.getTaskStates().values()) {
                String taskId = localState.getTaskId();

                // 检查是否有冲突
                boolean hasConflict = conflicts.stream()
                        .anyMatch(c -> c.getTaskId().equals(taskId));

                if (!hasConflict) {
                    // 无冲突，使用本地状态
                    mergedStates.put(taskId, localState);
                } else {
                    // 有冲突，记录被丢弃的本地变更
                    discardedChanges.add(new TaskSyncChange(taskId,
                            TaskSyncChange.ChangeType.STATUS_CHANGED,
                            localState.getStatus(),
                            plansTaskStates.get(taskId).getStatus()));

                    logger.warn("Conflict detected for task {}: local={}, remote={}. Using remote value.",
                            taskId, localState.getStatus(), plansTaskStates.get(taskId).getStatus());
                }
            }

            // 4. 写回 Plans.md
            StateSnapshot mergedSnapshot = new StateSnapshot(mergedStates, state.getSessionId());
            SyncResult result = syncToPlans(mergedSnapshot, plansMdPath);

            // 如果有被丢弃的变更，添加到结果中
            if (!discardedChanges.isEmpty()) {
                List<SyncConflict> resolvedConflicts = new ArrayList<>();
                for (SyncConflict conflict : conflicts) {
                    resolvedConflicts.add(conflict.withResolution(SyncConflict.ConflictResolution.REMOTE_WINS));
                }
                // 返回带冲突的结果，标记为已解决（remote wins）
                return SyncResult.withConflicts(result.getChanges(), resolvedConflicts, result.getPlansHash());
            }

            return result;

        } catch (Exception e) {
            throw new SyncException("Failed bidirectional sync: " + e.getMessage(),
                    SyncException.ErrorType.CONFLICT_ERROR, e);
        }
    }

    @Override
    public boolean hasUnsyncedChanges(Path plansMdPath, String lastKnownHash) {
        try {
            String currentHash = getPlansHash(plansMdPath);
            return !currentHash.equals(lastKnownHash);
        } catch (SyncException e) {
            logger.warn("Failed to check for unsynced changes", e);
            return true; // 出错时假设有变更
        }
    }

    @Override
    public String getPlansHash(Path plansMdPath) throws SyncException {
        validatePath(plansMdPath);

        try {
            String content;
            synchronized (this) {
                if (!Files.exists(plansMdPath)) {
                    throw new SyncException("Plans.md file not found: " + plansMdPath,
                            SyncException.ErrorType.READ_ERROR);
                }
                content = Files.readString(plansMdPath);
            }
            return calculateHash(content);
        } catch (IOException e) {
            throw new SyncException("Failed to calculate Plans.md hash: " + plansMdPath,
                    SyncException.ErrorType.READ_ERROR, e);
        }
    }

    /**
     * 查找任务行的位置
     */
    private Optional<int[]> findTaskLine(String content, String taskId) {
        // 使用更安全的模式匹配（6列格式）
        Pattern taskPattern = Pattern.compile(
                "^\\|\\s*" + Pattern.quote(taskId) + "\\s*\\|[^\\|]*\\|[^\\|]*\\|[^\\|]*\\|\\s*[^\\|]*?\\s*\\|$",
                Pattern.MULTILINE
        );

        Matcher matcher = taskPattern.matcher(content);
        if (matcher.find()) {
            return Optional.of(new int[]{matcher.start(), matcher.end()});
        }
        return Optional.empty();
    }

    /**
     * 替换行中的状态
     */
    private String replaceStatusInLine(String line, String newStatus) {
        // 只替换最后一列（状态列）
        int lastPipeIndex = line.lastIndexOf('|');
        if (lastPipeIndex > 0) {
            int prevPipeIndex = line.lastIndexOf('|', lastPipeIndex - 1);
            if (prevPipeIndex >= 0) {
                return line.substring(0, prevPipeIndex + 1) + " " + newStatus + " |";
            }
        }
        return line;
    }

    /**
     * 在表格最后一行后追加新任务行
     */
    private String appendAfterLastTableRow(String content, String newRow) {
        Matcher matcher = STATUS_PATTERN.matcher(content);
        int lastEnd = -1;
        while (matcher.find()) {
            lastEnd = matcher.end();
        }

        if (lastEnd >= 0) {
            return content.substring(0, lastEnd) + "\n" + newRow + content.substring(lastEnd);
        }

        // 如果没有任务行，回退到在内容末尾追加
        return content.trim() + "\n" + newRow;
    }

    /**
     * 检测状态冲突
     */
    private List<SyncConflict> detectConflicts(StateSnapshot localState, Map<String, TaskState> remoteStates) {
        List<SyncConflict> conflicts = new ArrayList<>();

        for (TaskState localTask : localState.getTaskStates().values()) {
            String taskId = localTask.getTaskId();
            TaskState remoteTask = remoteStates.get(taskId);

            if (remoteTask != null) {
                // 检查状态是否不同
                if (!localTask.getStatus().equals(remoteTask.getStatus())) {
                    conflicts.add(new SyncConflict(
                            taskId,
                            SyncConflict.ConflictType.STATUS_CONFLICT,
                            localTask.getStatus(),
                            remoteTask.getStatus(),
                            "Status differs between local state and Plans.md"
                    ));
                }
            }
        }

        return conflicts;
    }

    /**
     * 从表格行中提取状态
     */
    private String extractStatus(String line) {
        int lastPipeIndex = line.lastIndexOf('|');
        if (lastPipeIndex > 0) {
            int prevPipeIndex = line.lastIndexOf('|', lastPipeIndex - 1);
            if (prevPipeIndex >= 0) {
                return line.substring(prevPipeIndex + 1, lastPipeIndex).trim();
            }
        }
        return "";
    }

    /**
     * 计算文件内容的哈希值
     */
    private String calculateHash(String content) throws SyncException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new SyncException("Failed to calculate hash", SyncException.ErrorType.UNKNOWN_ERROR, e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 验证输入参数
     */
    private void validateInputs(StateSnapshot state, Path plansMdPath) throws SyncException {
        if (state == null) {
            throw new SyncException("State snapshot cannot be null",
                    SyncException.ErrorType.VALIDATION_ERROR);
        }
        validatePath(plansMdPath);
    }

    /**
     * 验证文件路径
     */
    private void validatePath(Path plansMdPath) throws SyncException {
        if (plansMdPath == null) {
            throw new SyncException("Plans.md path cannot be null",
                    SyncException.ErrorType.VALIDATION_ERROR);
        }
    }
}
