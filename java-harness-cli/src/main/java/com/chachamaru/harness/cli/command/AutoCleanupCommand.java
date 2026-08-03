package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Auto cleanup temporary files
 */
@Command(name = "auto-cleanup",
         description = "Auto cleanup temporary files")
public class AutoCleanupCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("AutoCleanupCommand executed");
    }
}
