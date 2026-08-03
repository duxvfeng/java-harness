package com.chachamaru.harness.hook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * Hook input event model.
 * Represents a hook event from Claude Code.
 */
public class HookInput {
    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("transcript_path")
    private String transcriptPath;

    private String cwd;

    @JsonProperty("permission_mode")
    private String permissionMode;

    @JsonProperty("hook_event_name")
    private String hookEventName;

    @JsonProperty("tool_name")
    private String toolName;

    @JsonProperty("tool_input")
    private Map<String, Object> toolInput;

    @JsonProperty("plugin_root")
    private String pluginRoot;

    // Getters and setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTranscriptPath() { return transcriptPath; }
    public void setTranscriptPath(String transcriptPath) { this.transcriptPath = transcriptPath; }

    public String getCwd() { return cwd; }
    public void setCwd(String cwd) { this.cwd = cwd; }

    public String getPermissionMode() { return permissionMode; }
    public void setPermissionMode(String permissionMode) { this.permissionMode = permissionMode; }

    public String getHookEventName() { return hookEventName; }
    public void setHookEventName(String hookEventName) { this.hookEventName = hookEventName; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public Map<String, Object> getToolInput() { return toolInput; }
    public void setToolInput(Map<String, Object> toolInput) { this.toolInput = toolInput; }

    public String getPluginRoot() { return pluginRoot; }
    public void setPluginRoot(String pluginRoot) { this.pluginRoot = pluginRoot; }
}
