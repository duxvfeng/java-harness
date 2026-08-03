package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * SessionInit hook command for Harness CLI.
 *
 * <p>SessionStart: session initialization and Plans.md summary.</p>
 */
@Command(name = "session-init",
         description = "SessionStart: session initialization and Plans.md summary")
public class SessionInitCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SessionStart: session initialization and Plans.md summary...");
    }
}
