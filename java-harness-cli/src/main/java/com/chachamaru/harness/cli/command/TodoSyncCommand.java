package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Sync todo items
 */
@Command(name = "todo-sync",
         description = "Sync todo items")
public class TodoSyncCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("TodoSyncCommand executed");
    }
}
