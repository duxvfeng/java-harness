package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * SubagentStartHook hook command for Harness CLI.
 *
 * <p>SubagentStart: track agent lifecycle start.</p>
 */
@Command(name = "subagent-start",
         description = "SubagentStart: track agent lifecycle start")
public class SubagentStartHookCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SubagentStart: track agent lifecycle start...");
    }
}
