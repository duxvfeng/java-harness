package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Track usage statistics
 */
@Command(name = "usage-tracker",
         description = "Track usage statistics")
public class UsageTrackerCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("UsageTrackerCommand executed");
    }
}
