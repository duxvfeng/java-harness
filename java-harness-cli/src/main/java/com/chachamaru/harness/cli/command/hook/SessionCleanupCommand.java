package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * SessionCleanup hook command for Harness CLI.
 *
 * <p>SessionEnd: temp file cleanup.</p>
 */
@Command(name = "session-cleanup",
         description = "SessionEnd: temp file cleanup")
public class SessionCleanupCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SessionEnd: temp file cleanup...");
    }
}
