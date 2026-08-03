package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Elicitation result handler
 */
@Command(name = "elicitation-result",
         description = "Elicitation result handler")
public class ElicitationResultCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("ElicitationResultCommand executed");
    }
}
