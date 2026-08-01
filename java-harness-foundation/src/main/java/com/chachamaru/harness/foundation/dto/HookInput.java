package com.chachamaru.harness.foundation.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Hook input DTO - represents input data for hook event handlers.
 *
 * <p>This is the primary input format for all hook event processing in the Harness system.
 * It contains session context, tool information, and event-specific payload.</p>
 *
 * @spec_reference spec.md#Data Models
 */
public record HookInput(
    @JsonProperty("session_id")
    String sessionId,

    @JsonProperty("transcript_path")
    String transcriptPath,

    @JsonProperty("cwd")
    String cwd,

    @JsonProperty("permission_mode")
    String permissionMode,

    @JsonProperty("hook_event_name")
    String hookEventName,

    @JsonProperty("tool_name")
    String toolName,

    @JsonProperty("tool_input")
    Map<String, Object> toolInput,

    @JsonProperty("plugin_root")
    String pluginRoot
) {
    /**
     * Creates a HookInput instance with all required fields.
     *
     * @param sessionId       The Claude Code session identifier
     * @param transcriptPath  Path to the session transcript file
     * @param cwd             Current working directory
     * @param permissionMode  Permission mode (bypassPermissions, etc.)
     * @param hookEventName   Name of the hook event (e.g., PreToolUse, PostToolUse)
     * @param toolName        Name of the tool being invoked
     * @param toolInput       Input parameters for the tool
     * @param pluginRoot      Root directory of the plugin
     */
    public HookInput {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }
        if (hookEventName == null || hookEventName.isBlank()) {
            throw new IllegalArgumentException("hookEventName cannot be null or blank");
        }
    }

    /**
     * Creates a minimal HookInput for testing purposes.
     */
    public static HookInput createForTest(String hookEventName, String toolName) {
        return new HookInput(
            "test-session",
            "/tmp/transcript.json",
            "/tmp",
            "test",
            hookEventName,
            toolName,
            Map.of(),
            "/plugin"
        );
    }
}
