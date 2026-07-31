package com.chachamaru.harness.shared.constants;

/**
 * Hook event constants
 */
public final class HookConstants {
    private HookConstants() {}

    // Hook event names
    public static final String PRE_TOOL_USE = "PreToolUse";
    public static final String POST_TOOL_USE = "PostToolUse";
    public static final String PERMISSION_REQUEST = "PermissionRequest";
    public static final String SESSION_START = "SessionStart";
    public static final String SESSION_END = "SessionEnd";
    public static final String STOP = "Stop";
    public static final String PRE_COMPACT = "PreCompact";
    public static final String POST_COMPACT = "PostCompact";
    public static final String TASK_COMPLETED = "TaskCompleted";
    public static final String TASK_CREATED = "TaskCreated";
    public static final String PERMISSION_DENIED = "PermissionDenied";
    public static final String SUBAGENT_START = "SubagentStart";
    public static final String SUBAGENT_STOP = "SubagentStop";
    public static final String TEAMMATE_IDLE = "TeammateIdle";
    public static final String NOTIFICATION = "Notification";
    public static final String CONFIG_CHANGE = "ConfigChange";
    public static final String USER_PROMPT_SUBMIT = "UserPromptSubmit";
    public static final String ELICITATION = "Elicitation";
    public static final String ELICITATION_RESULT = "ElicitationResult";
    public static final String STOP_FAILURE = "StopFailure";
    public static final String INSTRUCTIONS_LOADED = "InstructionsLoaded";
    public static final String WORKTREE_CREATE = "WorktreeCreate";
    public static final String WORKTREE_REMOVE = "WorktreeRemove";
    public static final String CWD_CHANGED = "CwdChanged";
    public static final String FILE_CHANGED = "FileChanged";
    public static final String POST_TOOL_FAILURE = "PostToolUseFailure";

    // Decision values
    public static final String DECISION_ALLOW = "allow";
    public static final String DECISION_DENY = "deny";
    public static final String DECISION_ASK = "ask";
    public static final String DECISION_DEFER = "defer";

    // Exit codes
    public static final int EXIT_ALLOW = 0;
    public static final int EXIT_DENY = 2;
}
