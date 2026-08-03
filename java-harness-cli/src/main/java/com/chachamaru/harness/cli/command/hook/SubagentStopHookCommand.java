package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * SubagentStopHook hook command for Harness CLI.
 *
 * <p>SubagentStop: track agent lifecycle stop.</p>
 */
@Command(name = "subagent-stop",
         description = "SubagentStop: track agent lifecycle stop")
public class SubagentStopHookCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SubagentStop: track agent lifecycle stop...");
    }
}
