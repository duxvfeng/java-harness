package com.chachamaru.harness.hook;

import java.util.Map;

/**
 * Hook input event model.
 * Represents a hook event from Claude Code.
 */
public class HookInput {
    private String sessionId;
    private String transcriptPath;
    private String cwd;
    private String permissionMode;
    private String hookEventName;
    private String toolName;
    private Map<String, Object> toolInput;
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
