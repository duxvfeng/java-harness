package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Emit the release prompt for the host to execute
 */
@Command(name = "release",
         description = "Emit the release prompt for the host to execute")
public class ReleaseCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("ReleaseCommand executed");
    }
}
