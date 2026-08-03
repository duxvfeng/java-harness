package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Track file changes
 */
@Command(name = "track-changes",
         description = "Track file changes")
public class TrackChangesCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("TrackChangesCommand executed");
    }
}
