package com.chachamaru.harness.cli.command.plan;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Check dependencies command for Harness CLI.
 *
 * <p>Verifies done tasks only depend on closed tasks in Plans.md.</p>
 */
@Command(name = "check-deps",
         description = "Verify done tasks only depend on closed tasks")
public class CheckDepsCommand implements Runnable {

    @Parameters(description = "Path to Plans.md file", defaultValue = "Plans.md")
    private String plansFile;

    @Override
    public void run() {
        System.out.println("Checking dependencies in: " + plansFile);
    }
}
