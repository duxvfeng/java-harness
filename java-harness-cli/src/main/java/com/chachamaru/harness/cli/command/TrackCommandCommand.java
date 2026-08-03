package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Track command usage
 */
@Command(name = "track-command",
         description = "Track command usage")
public class TrackCommandCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("TrackCommandCommand executed");
    }
}
