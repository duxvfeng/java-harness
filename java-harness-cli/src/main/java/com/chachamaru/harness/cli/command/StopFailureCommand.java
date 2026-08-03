package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Handle stop failure
 */
@Command(name = "stop-failure",
         description = "Handle stop failure")
public class StopFailureCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("StopFailureCommand executed");
    }
}
