package com.chachamaru.harness.cli.hook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Hook input from Claude Code (stdin)
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
     * Validate required fields
     */
    public boolean isValid() {
        return sessionId != null && !sessionId.isBlank()
            && hookEventName != null && !hookEventName.isBlank()
            && toolName != null && !toolName.isBlank();
    }

    /**
     * Convert to Map for IPC communication
     */
    public java.util.Map<String, Object> toMap() {
        return java.util.Map.of(
            "session_id", sessionId,
            "transcript_path", transcriptPath,
            "cwd", cwd,
            "permission_mode", permissionMode,
            "hook_event_name", hookEventName,
            "tool_name", toolName,
            "tool_input", toolInput,
            "plugin_root", pluginRoot
        );
    }
}
