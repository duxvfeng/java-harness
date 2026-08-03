package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Hook command group for processing Claude Code hook events.
 *
 * <p>This command serves as a parent for hook-related subcommands:
 * <ul>
 *   <li>pre-tool - Pre-tool execution hook</li>
 *   <li>post-tool - Post-tool execution hook</li>
 *   <li>permission - Permission check hook</li>
 *   <li>session-start - Session start hook</li>
 *   <li>post-tool-failure - Post-tool failure hook</li>
 *   <li>post-compact - Post-compact hook</li>
 *   <li>notification - Notification hook</li>
 *   <li>permission-denied - Permission denied hook</li>
 *   <li>ask-user-question-normalize - Ask user question normalize hook</li>
 *   <li>session-init - Session init hook</li>
 *   <li>session-cleanup - Session cleanup hook</li>
 *   <li>session-monitor - Session monitor hook</li>
 *   <li>session-summary - Session summary hook</li>
 *   <li>ci-status-hook - CI status hook</li>
 *   <li>subagent-start-hook - Subagent start hook</li>
 *   <li>subagent-stop-hook - Subagent stop hook</li>
 * </ul>
 * </p>
 */
@Command(name = "hook",
         mixinStandardHelpOptions = true,
         subcommands = {
             PreToolCommand.class,
             PostToolCommand.class,
             PermissionCommand.class,
             SessionStartCommand.class,
             PostToolFailureCommand.class,
             PostCompactCommand.class,
             NotificationCommand.class,
             PermissionDeniedCommand.class,
             AskUserQuestionNormalizeCommand.class,
             SessionInitCommand.class,
             SessionCleanupCommand.class,
             SessionMonitorCommand.class,
             SessionSummaryCommand.class,
             CiStatusHookCommand.class,
             SubagentStartHookCommand.class,
             SubagentStopHookCommand.class
         },
         description = "Hook subcommands for processing Claude Code hook events")
public class HookCommand implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
