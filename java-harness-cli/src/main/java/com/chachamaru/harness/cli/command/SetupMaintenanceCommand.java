package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Setup maintenance tasks
 */
@Command(name = "setup-maintenance",
         description = "Setup maintenance tasks")
public class SetupMaintenanceCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SetupMaintenanceCommand executed");
    }
}
