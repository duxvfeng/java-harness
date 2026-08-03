package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Clear pending tasks
 */
@Command(name = "clear-pending",
         description = "Clear pending tasks")
public class ClearPendingCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("ClearPendingCommand executed");
    }
}
