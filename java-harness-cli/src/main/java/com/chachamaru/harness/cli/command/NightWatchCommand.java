package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Emit night-watch patrol report
 */
@Command(name = "night-watch",
         description = "Emit night-watch patrol report")
public class NightWatchCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("NightWatchCommand executed");
    }
}
