package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * SessionSummary hook command for Harness CLI.
 *
 * <p>Stop: session summary to session-log.md.</p>
 */
@Command(name = "session-summary",
         description = "Stop: session summary to session-log.md")
public class SessionSummaryCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Stop: session summary to session-log.md...");
    }
}
