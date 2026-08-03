package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Policy management
 */
@Command(name = "policy",
         description = "Policy management")
public class PolicyCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PolicyCommand executed");
    }
}
