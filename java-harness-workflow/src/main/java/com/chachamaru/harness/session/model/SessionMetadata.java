package com.chachamaru.harness.session.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 会话保存元数据
 *
 * <p>记录会话保存时的完整元数据信息，包括保存原因、任务上下文、Git状态等。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SessionMetadata {

    @JsonProperty("saveId")
    private final String saveId;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private final Instant timestamp;

    @JsonProperty("saveReason")
    private final String saveReason;

    @JsonProperty("tokenUsage")
    private final int tokenUsage;

    @JsonProperty("taskContext")
    private final TaskContext taskContext;

    @JsonProperty("gitState")
    private final GitState gitState;

    @JsonProperty("summary")
    private final String summary;

    @JsonProperty("size")
    private final SaveSize size;

    /**
     * 构造 SessionMetadata
     *
     * @param saveId 保存ID，格式: yyyyMMdd-HHmmss-reason
     * @param timestamp 保存时间戳
     * @param saveReason 保存原因
     * @param tokenUsage 当前 token 使用率 (0-100)
     * @param taskContext 任务上下文信息
     * @param gitState Git 状态信息
     * @param summary 会话摘要
     * @param size 保存大小信息
     */
    @JsonCreator
    public SessionMetadata(
            @JsonProperty("saveId") String saveId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("saveReason") String saveReason,
            @JsonProperty("tokenUsage") int tokenUsage,
            @JsonProperty("taskContext") TaskContext taskContext,
            @JsonProperty("gitState") GitState gitState,
            @JsonProperty("summary") String summary,
            @JsonProperty("size") SaveSize size) {
        this.saveId = saveId;
        this.timestamp = timestamp;
        this.saveReason = saveReason;
        this.tokenUsage = tokenUsage;
        this.taskContext = taskContext;
        this.gitState = gitState;
        this.summary = summary;
        this.size = size;
    }

    // Getters
    public String getSaveId() { return saveId; }
    public Instant getTimestamp() { return timestamp; }
    public String getSaveReason() { return saveReason; }
    public int getTokenUsage() { return tokenUsage; }
    public TaskContext getTaskContext() { return taskContext; }
    public GitState getGitState() { return gitState; }
    public String getSummary() { return summary; }
    public SaveSize getSize() { return size; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionMetadata that = (SessionMetadata) o;
        return Objects.equals(saveId, that.saveId) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveId, timestamp);
    }

    @Override
    public String toString() {
        return "SessionMetadata{" +
                "saveId='" + saveId + '\'' +
                ", timestamp=" + timestamp +
                ", saveReason='" + saveReason + '\'' +
                ", tokenUsage=" + tokenUsage +
                ", summary='" + summary + '\'' +
                '}';
    }

    /**
     * 任务上下文信息
     */
    public static class TaskContext {
        @JsonProperty("currentPhase")
        private final String currentPhase;

        @JsonProperty("completedTasks")
        private final List<String> completedTasks;

        @JsonProperty("currentTask")
        private final String currentTask;

        @JsonProperty("totalTasks")
        private final int totalTasks;

        @JsonCreator
        public TaskContext(
                @JsonProperty("currentPhase") String currentPhase,
                @JsonProperty("completedTasks") List<String> completedTasks,
                @JsonProperty("currentTask") String currentTask,
                @JsonProperty("totalTasks") int totalTasks) {
            this.currentPhase = currentPhase;
            this.completedTasks = completedTasks;
            this.currentTask = currentTask;
            this.totalTasks = totalTasks;
        }

        public String getCurrentPhase() { return currentPhase; }
        public List<String> getCompletedTasks() { return completedTasks; }
        public String getCurrentTask() { return currentTask; }
        public int getTotalTasks() { return totalTasks; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TaskContext that = (TaskContext) o;
            return totalTasks == that.totalTasks &&
                   Objects.equals(currentPhase, that.currentPhase) &&
                   Objects.equals(completedTasks, that.completedTasks) &&
                   Objects.equals(currentTask, that.currentTask);
        }

        @Override
        public int hashCode() {
            return Objects.hash(currentPhase, completedTasks, currentTask, totalTasks);
        }
    }

    /**
     * Git 状态信息
     */
    public static class GitState {
        @JsonProperty("branch")
        private final String branch;

        @JsonProperty("commit")
        private final String commit;

        @JsonProperty("modifiedFiles")
        private final int modifiedFiles;

        @JsonProperty("uncommittedChanges")
        private final boolean uncommittedChanges;

        @JsonCreator
        public GitState(
                @JsonProperty("branch") String branch,
                @JsonProperty("commit") String commit,
                @JsonProperty("modifiedFiles") int modifiedFiles,
                @JsonProperty("uncommittedChanges") boolean uncommittedChanges) {
            this.branch = branch;
            this.commit = commit;
            this.modifiedFiles = modifiedFiles;
            this.uncommittedChanges = uncommittedChanges;
        }

        public String getBranch() { return branch; }
        public String getCommit() { return commit; }
        public int getModifiedFiles() { return modifiedFiles; }
        public boolean hasUncommittedChanges() { return uncommittedChanges; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GitState gitState = (GitState) o;
            return modifiedFiles == gitState.modifiedFiles &&
                   uncommittedChanges == gitState.uncommittedChanges &&
                   Objects.equals(branch, gitState.branch) &&
                   Objects.equals(commit, gitState.commit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(branch, commit, modifiedFiles, uncommittedChanges);
        }
    }

    /**
     * 保存大小信息
     */
    public static class SaveSize {
        @JsonProperty("totalFiles")
        private final int totalFiles;

        @JsonProperty("compressedSize")
        private final String compressedSize;

        @JsonProperty("uncompressedSize")
        private final String uncompressedSize;

        @JsonCreator
        public SaveSize(
                @JsonProperty("totalFiles") int totalFiles,
                @JsonProperty("compressedSize") String compressedSize,
                @JsonProperty("uncompressedSize") String uncompressedSize) {
            this.totalFiles = totalFiles;
            this.compressedSize = compressedSize;
            this.uncompressedSize = uncompressedSize;
        }

        public int getTotalFiles() { return totalFiles; }
        public String getCompressedSize() { return compressedSize; }
        public String getUncompressedSize() { return uncompressedSize; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SaveSize saveSize = (SaveSize) o;
            return totalFiles == saveSize.totalFiles &&
                   Objects.equals(compressedSize, saveSize.compressedSize) &&
                   Objects.equals(uncompressedSize, saveSize.uncompressedSize);
        }

        @Override
        public int hashCode() {
            return Objects.hash(totalFiles, compressedSize, uncompressedSize);
        }
    }
}