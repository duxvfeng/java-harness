package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Handle instructions loaded event
 */
@Command(name = "instructions-loaded",
         description = "Handle instructions loaded event")
public class InstructionsLoadedCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("InstructionsLoadedCommand executed");
    }
}
