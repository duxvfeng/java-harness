package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Watch Plans.md for changes
 */
@Command(name = "plans-watcher",
         description = "Watch Plans.md for changes")
public class PlansWatcherCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PlansWatcherCommand executed");
    }
}
