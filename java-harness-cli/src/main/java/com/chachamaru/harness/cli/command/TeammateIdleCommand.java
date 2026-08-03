package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Handle teammate idle event
 */
@Command(name = "teammate-idle",
         description = "Handle teammate idle event")
public class TeammateIdleCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("TeammateIdleCommand executed");
    }
}
