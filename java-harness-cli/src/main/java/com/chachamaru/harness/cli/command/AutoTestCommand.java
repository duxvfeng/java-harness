package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Auto run tests
 */
@Command(name = "auto-test",
         description = "Auto run tests")
public class AutoTestCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("AutoTestCommand executed");
    }
}
