package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import com.chachamaru.harness.shared.constants.HookConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PermissionRequest hook handler
 * <p>
 * Handles automatic permission approval based on configured policies.
 * Supports safe operation whitelisting and pattern-based approval.
 * </p>
 */
public class PermissionRequestHandler implements HookHandler {
    private static final Logger log = LoggerFactory.getLogger(PermissionRequestHandler.class);
    private static final String PERMISSION_REQUEST = "PermissionRequest";

    // Safe tools that can be auto-approved
    private static final Set<String> SAFE_TOOLS = Set.of(
        "Read", "Glob", "Grep", "WebFetch", "WebSearch"
    );

    // Safe file patterns that can be auto-approved
    private static final List<String> SAFE_PATTERNS = List.of(
        "**/*.java",
        "**/*.md",
        "**/README*",
        "**/*.txt",
        "**/*.json",
        "**/*.xml",
        "**/*.yaml",
        "**/*.yml"
    );

    // Dangerous operations that require explicit approval
    private static final Set<String> DANGEROUS_OPERATIONS = Set.of(
        "rm", "delete", "format", "wipe", "destroy", "purge"
    );

    @Override
    public String getEventName() {
        return PERMISSION_REQUEST;
    }

    @Override
    public HookOutput handle(HookInput input) throws IOException {
        log.debug("PermissionRequest for tool: {}", input.toolName());

        // Check if this is a safe tool
        if (SAFE_TOOLS.contains(input.toolName())) {
            log.debug("Auto-approving safe tool: {}", input.toolName());
            return HookOutput.allow();
        }

        // Check for dangerous operations in tool input
        if (containsDangerousOperation(input)) {
            log.warn("Denying dangerous operation in tool: {}", input.toolName());
            return HookOutput.deny("Dangerous operation detected - requires explicit approval");
        }

        // Check file patterns for write operations
        if (isWriteOperation(input.toolName())) {
            if (matchesSafePattern(input)) {
                log.debug("Auto-approving write to safe file pattern");
                return HookOutput.allow();
            }
        }

        // Default to defer (ask user)
        log.debug("Deferring permission request for: {}", input.toolName());
        return HookOutput.defer("Requires user approval");
    }

    /**
     * Check if tool input contains dangerous operations
     */
    private boolean containsDangerousOperation(HookInput input) {
        if (input.toolInput() == null) {
            return false;
        }

        // Check for dangerous keywords in command inputs
        Object commandObj = input.toolInput().get("command");
        if (commandObj instanceof String) {
            String command = ((String) commandObj).toLowerCase();
            for (String dangerous : DANGEROUS_OPERATIONS) {
                if (command.contains(dangerous)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if this is a write operation
     */
    private boolean isWriteOperation(String toolName) {
        return "Write".equals(toolName) ||
               "Edit".equals(toolName) ||
               "NotebookEdit".equals(toolName);
    }

    /**
     * Check if file path matches safe patterns
     */
    private boolean matchesSafePattern(HookInput input) {
        String filePath = extractFilePath(input);
        if (filePath == null) {
            return false;
        }

        // Normalize path separators
        String normalizedPath = filePath.replace('\\', '/');

        // Simple extension-based matching
        for (String pattern : SAFE_PATTERNS) {
            if (matchesGlobPattern(normalizedPath, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Simple glob pattern matching
     */
    private boolean matchesGlobPattern(String path, String pattern) {
        // Handle simple extension patterns like **/*.java
        if (pattern.startsWith("**/")) {
            String extension = pattern.substring(3); // Remove **/
            if (extension.startsWith("*.")) {
                // Match by extension
                String ext = extension.substring(1); // Remove *
                return path.endsWith(ext);
            }
        }

        // Handle simple filename patterns like README*
        if (pattern.startsWith("README")) {
            String fileName = getFileName(path);
            if (fileName.startsWith("README")) {
                return true;
            }
        }

        // Handle *.md, *.txt etc.
        if (pattern.startsWith("*.")) {
            String ext = pattern.substring(1); // Remove *
            return path.endsWith(ext);
        }

        // Default: try regex matching
        return matchesPattern(path, pattern);
    }

    /**
     * Extract filename from path
     */
    private String getFileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return path.substring(lastSlash + 1);
        }
        return path;
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
     * Simple pattern matching (supports ** and * wildcards)
     */
    private boolean matchesPattern(String path, String pattern) {
        // Convert glob pattern to regex
        String regex = pattern
            .replace("**", ".*")    // ** matches anything including path separators
            .replace("*", "[^/]*")  // * matches single path component
            .replace(".", "\\.");   // Escape literal dots

        // Match path against regex with anchors (case-insensitive)
        return path.matches("(?i)^" + regex + "$");
    }
}
