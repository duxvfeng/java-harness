package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Generate fix proposals
 */
@Command(name = "fix-proposal",
         description = "Generate fix proposals")
public class FixProposalCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("FixProposalCommand executed");
    }
}
