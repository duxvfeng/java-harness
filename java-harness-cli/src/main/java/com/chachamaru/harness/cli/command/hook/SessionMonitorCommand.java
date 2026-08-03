package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * SessionMonitor hook command for Harness CLI.
 *
 * <p>SessionStart: project state collection and session.json.</p>
 */
@Command(name = "session-monitor",
         description = "SessionStart: project state collection and session.json")
public class SessionMonitorCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SessionStart: project state collection and session.json...");
    }
}
