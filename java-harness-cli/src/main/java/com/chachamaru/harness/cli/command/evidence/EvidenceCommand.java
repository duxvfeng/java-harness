package com.chachamaru.harness.cli.command.evidence;

import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Evidence command group for evidence collection subcommands.
 *
 * <p>This command serves as a parent for evidence-related subcommands:
 * <ul>
 *   <li>collect - Collect evidence</li>
 * </ul>
 * </p>
 */
@Command(name = "evidence",
         mixinStandardHelpOptions = true,
         subcommands = {CollectCommand.class},
         description = "Evidence collection subcommands")
public class EvidenceCommand implements Runnable {
    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
