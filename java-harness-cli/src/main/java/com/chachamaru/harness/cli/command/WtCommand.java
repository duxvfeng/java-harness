package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Worktree fingerprint operations
 */
@Command(name = "wt",
         description = "Worktree fingerprint operations")
public class WtCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("WtCommand executed");
    }
}
