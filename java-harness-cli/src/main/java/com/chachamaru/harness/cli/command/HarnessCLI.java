package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.cli.command.hook.HookCommand;
import com.chachamaru.harness.cli.command.plan.PlanGroupCommand;
import com.chachamaru.harness.cli.command.evidence.CollectCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Main CLI entry point for Harness commands.
 *
 * <p>This class serves as the main entry point for command-line interactions,
 * supporting various commands like plan, gen, work, review, etc.</p>
 */
@Command(name = "harness",
         mixinStandardHelpOptions = true,
         versionProvider = HarnessVersionProvider.class,
         description = "Java Harness - Claude Code Harness CLI for Java",
         subcommands = {
             // Hook command group
             HookCommand.class,
             // Evidence commands
             CollectCommand.class,
             // Plan commands
             PlanGroupCommand.class,
             // Core commands
             SprintContractCommand.class,
             StatusCommand.class,
             InitCommand.class,
             SyncCommand.class,
             ValidateCommand.class,
             DoctorCommand.class,
             CodexLoopCommand.class,
             MemCommand.class,
             ChannelsWakeCommand.class,
             InboxCheckCommand.class,
             InboxCommand.class,
             // CI commands
             CiCheckCommand.class,
             CiStatusCommand.class,
             // Agent commands
             SubagentStartCommand.class,
             SubagentStopCommand.class,
             BreezingSignalCommand.class,
             FailureCodifierCommand.class,
             // Work commands
             WorkCommand.class,
             ReviewCommand.class,
             ReleaseCommand.class,
             GenCommand.class,
             // Session commands
             SessionRegisterCommand.class,
             SessionUnregisterCommand.class,
             // Audit commands
             SelfAuditCommand.class,
             RetiredAliasCommand.class,
             // Monitor commands
             NightWatchCommand.class,
             MirrorCommand.class,
             PlansWatcherCommand.class,
             // Worktree commands
             WtCommand.class,
             WorktreeCreateCommand.class,
             WorktreeRemoveCommand.class,
             // Score commands
             ImpactScoreCommand.class,
             QualityPackCommand.class,
             // Compact commands
             PreCompactCommand.class,
             PreCompactSaveCommand.class,
             // Policy commands
             PolicyCommand.class,
             InjectPolicyCommand.class,
             // Event commands
             AutoBroadcastCommand.class,
             AutoCleanupCommand.class,
             AutoTestCommand.class,
             ConfigChangeCommand.class,
             ElicitationCommand.class,
             ElicitationResultCommand.class,
             EmitTraceCommand.class,
             NotificationExtCommand.class,
             PermissionDeniedExtCommand.class,
             TaskCompletedExtCommand.class,
             RuntimeReactiveCommand.class,
             // File lease commands
             PostToolUseFileLeaseCommand.class,
             PreToolUseFileLeaseCommand.class,
             // Setup commands
             SetupInitCommand.class,
             SetupMaintenanceCommand.class,
             SkillMirrorDriftCommand.class,
             // Tracking commands
             TrackChangesCommand.class,
             TrackCommandCommand.class,
             UsageTrackerCommand.class,
             TodoSyncCommand.class,
             TddCheckCommand.class,
             // Other commands
             BrowserGuideCommand.class,
             ClearPendingCommand.class,
             CommitCleanupCommand.class,
             FixProposalCommand.class,
             InstructionsLoadedCommand.class,
             LogToolnameCommand.class,
             MemoryBridgeCommand.class,
             StopEvaluatorCommand.class,
             StopFailureCommand.class,
             TeammateIdleCommand.class,
             VersionCommand.class,
             CommandLine.HelpCommand.class
         })
public class HarnessCLI implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    /**
     * Main entry point for the CLI application
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new HarnessCLI()).execute(args);
        System.exit(exitCode);
    }
}