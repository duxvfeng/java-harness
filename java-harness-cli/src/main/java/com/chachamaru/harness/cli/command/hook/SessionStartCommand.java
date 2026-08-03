package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * SessionStart hook command for Harness CLI.
 *
 * <p>SessionStart env setup.</p>
 */
@Command(name = "session-start",
         description = "SessionStart env setup")
public class SessionStartCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SessionStart env setup...");
    }
}
