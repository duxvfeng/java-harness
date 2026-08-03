package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Session management
 */
@Command(name = "session",
         description = "Session management")
public class SessionCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SessionCommand executed");
    }
}
