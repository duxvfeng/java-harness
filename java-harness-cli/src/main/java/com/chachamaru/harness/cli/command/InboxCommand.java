package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Inbox management
 */
@Command(name = "inbox",
         description = "Inbox management")
public class InboxCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("InboxCommand executed");
    }
}
