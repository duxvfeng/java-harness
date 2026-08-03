package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Elicitation handler
 */
@Command(name = "elicitation",
         description = "Elicitation handler")
public class ElicitationCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("ElicitationCommand executed");
    }
}
