package com.chachamaru.harness.protocol;

/**
 * Hook event types in the Claude Code Harness lifecycle.
 *
 * <p>This enum defines the various hook points where custom logic can be injected
 * during the execution flow of AI-assisted development tasks.</p>
 *
 * @since 4.1.0
 */
public enum HookEventType {
    /**
     * Pre-execution hook - fired before a task begins execution.
     */
    PRE_HOOK,

    /**
     * Post-execution hook - fired after a task completes successfully.
     */
    POST_HOOK,

    /**
     * Guardrail hook - fired for safety/policy validation checks.
     */
    GUARDRAIL_HOOK,

    /**
     * Pre-commit hook - fired before code is committed to version control.
     */
    PRE_COMMIT_HOOK,

    /**
     * Post-commit hook - fired after code is committed to version control.
     */
    POST_COMMIT_HOOK,

    /**
     * Pre-review hook - fired before a code review begins.
     */
    PRE_REVIEW_HOOK,

    /**
     * Post-review hook - fired after a code review completes.
     */
    POST_REVIEW_HOOK,

    /**
     * Error hook - fired when an error occurs during task execution.
     */
    ERROR_HOOK
}
