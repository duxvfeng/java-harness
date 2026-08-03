package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Initialize setup
 */
@Command(name = "setup-init",
         description = "Initialize setup")
public class SetupInitCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SetupInitCommand executed");
    }
}
