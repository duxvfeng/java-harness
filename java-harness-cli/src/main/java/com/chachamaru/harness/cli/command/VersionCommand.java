package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Print version
 */
@Command(name = "version",
         description = "Print version")
public class VersionCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("VersionCommand executed");
    }
}
