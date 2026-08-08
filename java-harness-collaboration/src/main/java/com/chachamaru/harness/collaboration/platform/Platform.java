package com.chachamaru.harness.collaboration.platform;

/**
 * Platform enumeration representing supported AI development environments.
 *
 * <p>Defines the platforms that Java Harness can detect and adapt to:
 * <ul>
 *   <li>CLAUDE_CODE: Anthropic Claude Code (primary platform)</li>
 *   <li>CODEX: GPT Codex CLI (optional extension)</li>
 * </ul>
 *
 * @spec_reference Phase 7: Dual Platform Support
 */
public enum Platform {
    /**
     * Anthropic Claude Code - Primary platform
     */
    CLAUDE_CODE,

    /**
     * GPT Codex CLI - Optional extension platform
     */
    CODEX
}
