package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Extended notification handler
 */
@Command(name = "notification-ext",
         description = "Extended notification handler")
public class NotificationExtCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("NotificationExtCommand executed");
    }
}
