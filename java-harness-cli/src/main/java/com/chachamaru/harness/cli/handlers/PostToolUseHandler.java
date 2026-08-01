package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.*;

/**
 * PostToolUse hook handler
 * <p>
 * Handles post-tool validation including file tampering detection.
 * </p>
 */
public class PostToolUseHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PostToolUseHandler.class);
    private static final String POST_TOOL_USE = "PostToolUse";

    // Track file modifications for tampering detection
    private static final Map<String, FileSnapshot> fileSnapshots = new HashMap<>();

    @Override
    public String getEventName() {
        return POST_TOOL_USE;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.debug("PostToolUse triggered for tool: {}", input.toolName());

        // Detect file tampering for write/edit operations
        if (isWriteOperation(input.toolName())) {
            String tamperingStatus = detectTampering(input);
            if (tamperingStatus != null) {
                log.warn("File tampering detected: {}", tamperingStatus);
                return new HookOutput(POST_TOOL_USE, "allow", null, tamperingStatus);
            }
        }

        // Record tool usage statistics
        recordToolUsage(input);

        // Update session state
        updateSessionState(input);

        return HookOutput.allow();
    }

    /**
     * Check if this is a write/edit operation
     */
    private boolean isWriteOperation(String toolName) {
        return "Write".equals(toolName) ||
               "Edit".equals(toolName) ||
               "NotebookEdit".equals(toolName);
    }

    /**
     * Detect file tampering by comparing before/after states
     */
    private String detectTampering(HookInput input) throws IOException {
        String filePath = extractFilePath(input);
        if (filePath == null) {
            return null;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return null; // File doesn't exist, no tampering to detect
        }

        FileTime currentModTime = Files.getLastModifiedTime(path);
        String currentHash = calculateSimpleHash(path);
        long currentTime = System.currentTimeMillis();

        // Check if we have a previous snapshot
        FileSnapshot previousSnapshot = fileSnapshots.get(filePath);

        if (previousSnapshot != null) {
            // Check if file was modified externally (after our last operation)
            boolean modTimeChanged = !currentModTime.equals(previousSnapshot.modTime());
            boolean contentChanged = !currentHash.equals(previousSnapshot.contentHash());

            if (modTimeChanged && contentChanged) {
                // File was modified externally between our operations
                // Update snapshot to acknowledge the external change
                fileSnapshots.put(filePath, new FileSnapshot(currentModTime, currentHash, currentTime));
                return String.format("File '%s' was modified externally (content and modification time changed)",
                    path.getFileName());
            }
        }

        // Update snapshot for future comparison
        fileSnapshots.put(filePath, new FileSnapshot(currentModTime, currentHash, currentTime));

        return null;
    }

    /**
     * Extract file path from tool input
     */
    private String extractFilePath(HookInput input) {
        if (input.toolInput() == null) {
            return null;
        }

        Object pathObj = input.toolInput().get("file_path");
        if (pathObj instanceof String) {
            return (String) pathObj;
        }

        Object localPathObj = input.toolInput().get("localPath");
        if (localPathObj instanceof String) {
            return (String) localPathObj;
        }

        return null;
    }

    /**
     * Calculate a simple hash for content comparison
     */
    private String calculateSimpleHash(Path path) throws IOException {
        byte[] content = Files.readAllBytes(path);
        return String.valueOf(content.length) + "-" + Arrays.hashCode(content);
    }

    /**
     * Record tool usage statistics
     */
    private void recordToolUsage(HookInput input) {
        // Could be extended to track metrics
        log.debug("Tool usage recorded: {} for session {}",
            input.toolName(), input.sessionId());
    }

    /**
     * Update session state
     */
    private void updateSessionState(HookInput input) {
        // Could be extended to update session tracking
        log.debug("Session state updated for: {}", input.sessionId());
    }

    /**
     * Clear snapshots (for testing or session reset)
     */
    public static void clearSnapshots() {
        fileSnapshots.clear();
    }

    /**
     * File snapshot record
     */
    private static record FileSnapshot(FileTime modTime, String contentHash, long timestamp) {}
}