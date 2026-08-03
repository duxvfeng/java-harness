package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Check TDD compliance
 */
@Command(name = "tdd-check",
         description = "Check TDD compliance")
public class TddCheckCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("TddCheckCommand executed");
    }
}
