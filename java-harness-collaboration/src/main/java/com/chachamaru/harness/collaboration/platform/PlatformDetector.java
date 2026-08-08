package com.chachamaru.harness.collaboration.platform;

/**
 * Platform detection utility for identifying the current AI development environment.
 *
 * <p>Detects whether Java Harness is running in Claude Code or Codex CLI
 * by examining environment variables and system properties.</p>
 *
 * <p>Detection logic:
 * <ul>
 *   <li>Checks CLAUDE_CODE_HARNESS environment variable for Claude Code</li>
 *   <li>Checks CODEX_CLI environment variable for Codex CLI</li>
 *   <li>Defaults to CLAUDE_CODE if no platform-specific indicators found</li>
 * </ul>
 *
 * @spec_reference Phase 7: Dual Platform Support
 */
public class PlatformDetector {

    /**
     * Environment variable that indicates Claude Code environment.
     */
    private static final String CLAUDE_CODE_ENV = "CLAUDE_CODE_HARNESS";

    /**
     * Environment variable that indicates Codex CLI environment.
     */
    private static final String CODEX_CLI_ENV = "CODEX_CLI";

    /**
     * Detects the current platform by examining environment variables.
     *
     * <p>Detection priority:
     * <ol>
     *   <li>Codex CLI (if CODEX_CLI is set)</li>
     *   <li>Claude Code (if CLAUDE_CODE_HARNESS is set)</li>
     *   <li>Default to Claude Code (primary platform)</li>
     </ol>
     *
     * @return The detected platform (never null)
     */
    public Platform detectCurrentPlatform() {
        // Check for Codex CLI first
        if (isCodexEnvironment()) {
            return Platform.CODEX;
        }

        // Check for Claude Code
        if (isClaudeCodeEnvironment()) {
            return Platform.CLAUDE_CODE;
        }

        // Default to Claude Code (primary platform)
        return Platform.CLAUDE_CODE;
    }

    /**
     * Checks if currently running in Claude Code environment.
     *
     * @return true if CLAUDE_CODE_HARNESS environment variable is set
     */
    private boolean isClaudeCodeEnvironment() {
        String env = System.getenv(CLAUDE_CODE_ENV);
        return env != null && !env.trim().isEmpty();
    }

    /**
     * Checks if currently running in Codex CLI environment.
     *
     * @return true if CODEX_CLI environment variable is set
     */
    private boolean isCodexEnvironment() {
        String env = System.getenv(CODEX_CLI_ENV);
        return env != null && !env.trim().isEmpty();
    }
}
