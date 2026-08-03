package com.chachamaru.harness.cli.command;

import com.chachamaru.harness.cli.command.hook.HookCommand;
import com.chachamaru.harness.cli.command.plan.PlanGroupCommand;
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
         version = "4.0.0-java-SNAPSHOT",
         description = "Java Harness - Claude Code Harness CLI for Java",
         subcommands = {
             PlanCommand.class,
             GenCommand.class,
             PlansCommand.class,
             WorkCommand.class,
             ReviewCommand.class,
             SyncCommand.class,
             ValidateCommand.class,
             InitCommand.class,
             DoctorCommand.class,
             StatusCommand.class,
             EvidenceCommand.class,
             CiCheckCommand.class,
             CiStatusCommand.class,
             SprintContractCommand.class,
             CodexLoopCommand.class,
             BreezingSignalCommand.class,
             SubagentStartCommand.class,
             SubagentStopCommand.class,
             MemCommand.class,
             ChannelsWakeCommand.class,
             InboxCheckCommand.class,
             FailureCodifierCommand.class,
             HookCommand.class,
             PlanGroupCommand.class,
             CommandLine.HelpCommand.class
         })
public class HarnessCLI implements Runnable {

    @Override
    public void run() {
        // Default behavior - show help
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