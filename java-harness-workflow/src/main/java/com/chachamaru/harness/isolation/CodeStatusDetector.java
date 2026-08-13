package com.chachamaru.harness.isolation;

import com.chachamaru.harness.isolation.model.CodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Detector for code status in isolated branches.
 * Checks for uncommitted changes, branch cleanliness, and commit information.
 */
public class CodeStatusDetector {

    private static final Logger logger = LoggerFactory.getLogger(CodeStatusDetector.class);

    /**
     * Detect current code status in the specified worktree path
     */
    public CodeStatus detectCodeStatus(String worktreePath) {
        logger.debug("Detecting code status in worktree: {}", worktreePath);

        CodeStatus.Builder builder = CodeStatus.builder();

        try {
            // Validate worktree path
            File worktreeDir = new File(worktreePath);
            if (!worktreeDir.exists() || !worktreeDir.isDirectory()) {
                throw new IllegalArgumentException("Invalid worktree path: " + worktreePath);
            }

            // 1. Check for uncommitted changes
            boolean hasUncommittedChanges = checkUncommittedChanges(worktreePath);
            builder.hasUncommittedChanges(hasUncommittedChanges);

            // 2. Get last commit information
            GitCommitInfo lastCommit = getLastCommitInfo(worktreePath);
            if (lastCommit != null) {
                builder.lastCommitTime(lastCommit.getTimestamp());
                builder.lastCommitMessage(lastCommit.getMessage());
            }

            // 3. Check branch cleanliness
            boolean branchClean = checkBranchCleanliness(worktreePath);
            builder.branchClean(branchClean);

            // 4. Get changed files
            List<String> changedFiles = getChangedFiles(worktreePath);
            builder.filesChanged(changedFiles);

            // 5. Count commits in branch
            int commitsCount = countCommitsInBranch(worktreePath);
            builder.commitsCount(commitsCount);

            // 6. Count untracked files
            int untrackedFilesCount = countUntrackedFiles(worktreePath);
            builder.untrackedFilesCount(untrackedFilesCount);

            logger.info("Code status detected: uncommitted={}, clean={}, commits={}",
                       hasUncommittedChanges, branchClean, commitsCount);

        } catch (Exception e) {
            logger.error("Failed to detect code status in worktree: {}", worktreePath, e);
            builder.detectionError(e.getMessage());

            // Conservative defaults when detection fails
            builder.hasUncommittedChanges(true)
                   .branchClean(false);
        }

        return builder.build();
    }

    /**
     * Check if there are uncommitted changes in the worktree
     */
    private boolean checkUncommittedChanges(String worktreePath) {
        try {
            String result = executeGitCommand(worktreePath, "status", "--porcelain");
            return result != null && !result.trim().isEmpty();
        } catch (Exception e) {
            logger.warn("Failed to check for uncommitted changes", e);
            return true; // Conservative assumption
        }
    }

    /**
     * Check if branch is clean (no changes relative to base)
     */
    private boolean checkBranchCleanliness(String worktreePath) {
        try {
            // First check if there are any changes at all
            String statusResult = executeGitCommand(worktreePath, "status", "--porcelain");
            if (statusResult == null || statusResult.trim().isEmpty()) {
                return true; // No changes means branch is clean
            }

            // If there are changes, we need to compare with the base reference
            // For now, we'll consider branch "not clean" if there are any changes
            return false;

        } catch (Exception e) {
            logger.warn("Failed to check branch cleanliness", e);
            return false; // Conservative assumption
        }
    }

    /**
     * Get list of changed files in the worktree
     */
    private List<String> getChangedFiles(String worktreePath) {
        List<String> changedFiles = new ArrayList<>();
        try {
            String result = executeGitCommand(worktreePath, "status", "--porcelain");
            if (result != null && !result.trim().isEmpty()) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    if (line.length() < 3) {
                        continue;
                    }
                    String status = line.substring(0, 2);
                    if (!"  ".equals(status)) {
                        String path = line.substring(3).trim();
                        if (path.contains(" -> ")) {
                            String[] rename = path.split(" -> ", 2);
                            changedFiles.add(rename[0]);
                            changedFiles.add(rename[1]);
                        } else {
                            changedFiles.add(path);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to get changed files", e);
        }
        return changedFiles;
    }

    /**
     * Count untracked files in the worktree
     */
    private int countUntrackedFiles(String worktreePath) {
        int count = 0;
        try {
            String result = executeGitCommand(worktreePath, "status", "--porcelain");
            if (result != null && !result.trim().isEmpty()) {
                String[] lines = result.split("\n");
                for (String line : lines) {
                    if (line.startsWith("??")) {
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to count untracked files", e);
        }
        return count;
    }

    /**
     * Count number of commits in the current branch
     */
    private int countCommitsInBranch(String worktreePath) {
        try {
            String result = executeGitCommand(worktreePath, "rev-list", "--count", "HEAD");
            if (result != null && !result.trim().isEmpty()) {
                return Integer.parseInt(result.trim());
            }
        } catch (Exception e) {
            logger.warn("Failed to count commits in branch", e);
        }
        return 0;
    }

    /**
     * Get information about the last commit
     */
    private GitCommitInfo getLastCommitInfo(String worktreePath) {
        try {
            // Get commit timestamp
            String timestampResult = executeGitCommand(worktreePath, "log", "-1", "--format=%ct");
            Long timestamp = null;
            if (timestampResult != null && !timestampResult.trim().isEmpty()) {
                try {
                    timestamp = Long.parseLong(timestampResult.trim()) * 1000; // Convert to milliseconds
                } catch (NumberFormatException e) {
                    logger.warn("Failed to parse commit timestamp: {}", timestampResult);
                }
            }

            // Get commit message
            String messageResult = executeGitCommand(worktreePath, "log", "-1", "--format=%s");
            String message = (messageResult != null) ? messageResult.trim() : "";

            // Get commit hash
            String hashResult = executeGitCommand(worktreePath, "log", "-1", "--format=%H");
            String hash = (hashResult != null) ? hashResult.trim() : "";

            return new GitCommitInfo(timestamp, message, hash);

        } catch (Exception e) {
            logger.warn("Failed to get last commit info", e);
            return null;
        }
    }

    /**
     * Execute git command in the specified worktree
     */
    private String executeGitCommand(String worktreePath, String... commands) throws Exception {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add("git");
        fullCommand.add("-C");
        fullCommand.add(worktreePath);

        // Add the actual command
        fullCommand.addAll(java.util.Arrays.asList(commands));

        try {
            ProcessBuilder pb = new ProcessBuilder(fullCommand);
            pb.directory(new File(worktreePath));
            pb.redirectErrorStream(true);

            logger.trace("Executing git command: {}", String.join(" ", fullCommand));

            Process process = pb.start();

            // Read output with timeout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new Exception("Git command timed out after 5 seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new Exception("Git command failed with exit code: " + exitCode);
            }

            return output.toString();

        } catch (Exception e) {
            logger.error("Failed to execute git command: {}", String.join(" ", fullCommand), e);
            throw e;
        }
    }

    /**
     * Simple class to hold commit information
     */
    private static class GitCommitInfo {
        private final Long timestamp;
        private final String message;
        private final String hash;

        public GitCommitInfo(Long timestamp, String message, String hash) {
            this.timestamp = timestamp;
            this.message = message;
            this.hash = hash;
        }

        public LocalDateTime getTimestamp() {
            return timestamp != null ?
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(timestamp), ZoneOffset.UTC) : null;
        }

        public String getMessage() {
            return message;
        }

        public String getHash() {
            return hash;
        }
    }

    /**
     * Validate if a path is a valid git worktree
     */
    public boolean isValidGitWorktree(String worktreePath) {
        try {
            // Test git commands work
            executeGitCommand(worktreePath, "rev-parse", "--is-inside-work-tree");
            return true;

        } catch (Exception e) {
            logger.debug("Path is not a valid git worktree: {}", worktreePath);
            return false;
        }
    }

    /**
     * Get current branch name in worktree
     */
    public String getCurrentBranch(String worktreePath) {
        try {
            String result = executeGitCommand(worktreePath, "rev-parse", "--abbrev-ref", "HEAD");
            return (result != null) ? result.trim() : "unknown";
        } catch (Exception e) {
            logger.warn("Failed to get current branch", e);
            return "unknown";
        }
    }

    /**
     * Get base reference (parent commit) for the current worktree
     */
    public String getBaseReference(String worktreePath) {
        try {
            String result = executeGitCommand(worktreePath, "rev-parse", "HEAD^");
            return (result != null) ? result.trim() : null;
        } catch (Exception e) {
            logger.warn("Failed to get base reference", e);
            return null;
        }
    }
}
