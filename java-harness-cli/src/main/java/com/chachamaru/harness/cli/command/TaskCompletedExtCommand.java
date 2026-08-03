package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Extended task completed handler
 */
@Command(name = "task-completed-ext",
         description = "Extended task completed handler")
public class TaskCompletedExtCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("TaskCompletedExtCommand executed");
    }
}
