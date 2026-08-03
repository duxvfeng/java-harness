package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Unregister session
 */
@Command(name = "session-unregister",
         description = "Unregister session")
public class SessionUnregisterCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SessionUnregisterCommand executed");
    }
}
