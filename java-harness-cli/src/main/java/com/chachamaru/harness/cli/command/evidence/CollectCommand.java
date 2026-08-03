package com.chachamaru.harness.cli.command.evidence;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Collect evidence command for Harness CLI.
 *
 * <p>Collects evidence (test results, build logs) from stdin or file.</p>
 */
@Command(name = "collect",
         description = "Collect evidence (test results, build logs)")
public class CollectCommand implements Runnable {

    @Option(names = {"--label"}, description = "Evidence label", defaultValue = "general")
    private String label;

    @Option(names = {"--file"}, description = "Read content from file instead of stdin")
    private String file;

    @Override
    public void run() {
        System.out.println("Collecting evidence with label: " + label);
        if (file != null) {
            System.out.println("Reading from file: " + file);
        }
    }
}
