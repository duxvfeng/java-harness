package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Cleanup after commit
 */
@Command(name = "commit-cleanup",
         description = "Cleanup after commit")
public class CommitCleanupCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("CommitCleanupCommand executed");
    }
}
