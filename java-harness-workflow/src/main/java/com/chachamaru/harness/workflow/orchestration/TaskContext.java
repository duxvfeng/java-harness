package com.chachamaru.harness.workflow.orchestration;

/**
 * 任务上下文
 * 包含用于复杂度评分和 effort tier 决定的所有信息
 */
public class TaskContext {

    private final int fileCount;
    private final int directoryCount;
    private final boolean containsKeywords;
    private final boolean hasFailureHistory;

    /**
     * 创建任务上下文
     *
     * @param fileCount 受影响的文件数量
     * @param directoryCount 受影响的目录数量（包含 core/guardrails/security）
     * @param containsKeywords 是否包含关键字（architecture/security/design/migration）
     * @param hasFailureHistory 是否有失败历史
     */
    public TaskContext(int fileCount, int directoryCount, boolean containsKeywords, boolean hasFailureHistory) {
        this.fileCount = fileCount;
        this.directoryCount = directoryCount;
        this.containsKeywords = containsKeywords;
        this.hasFailureHistory = hasFailureHistory;
    }

    /**
     * 获取受影响的文件数量
     * @return 文件数量
     */
    public int getFileCount() {
        return fileCount;
    }

    /**
     * 获取受影响的目录数量
     * @return 目录数量
     */
    public int getDirectoryCount() {
        return directoryCount;
    }

    /**
     * 检查是否包含关键字
     * @return 如果包含关键字返回 true
     */
    public boolean containsKeywords() {
        return containsKeywords;
    }

    /**
     * 检查是否有失败历史
     * @return 如果有失败历史返回 true
     */
    public boolean hasFailureHistory() {
        return hasFailureHistory;
    }

    /**
     * 获取任务描述（用于日志）
     * @return 任务描述
     */
    public String getDescription() {
        return String.format("TaskContext{files=%d, dirs=%d, keywords=%s, failures=%s}",
                fileCount, directoryCount, containsKeywords, hasFailureHistory);
    }

    @Override
    public String toString() {
        return getDescription();
    }
}