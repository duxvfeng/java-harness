package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * Notification hook command for Harness CLI.
 *
 * <p>Notification event logging.</p>
 */
@Command(name = "notification",
         description = "Notification event logging")
public class NotificationCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Notification event logging...");
    }
}
