package com.chachamaru.harness.cli.command.plan;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Plan command group for plan-related subcommands.
 *
 * <p>This command serves as a parent for plan-related subcommands:
 * <ul>
 *   <li>check-deps - Check plan dependencies</li>
 * </ul>
 * </p>
 */
@Command(name = "plans",
         mixinStandardHelpOptions = true,
         subcommands = {},
         description = "Plan subcommands")
public class PlanGroupCommand implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
