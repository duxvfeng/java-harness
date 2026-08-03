package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * PostToolFailure hook command for Harness CLI.
 *
 * <p>PostToolUseFailure counter and escalation.</p>
 */
@Command(name = "post-tool-failure",
         description = "PostToolUseFailure counter and escalation")
public class PostToolFailureCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PostToolUseFailure counter and escalation...");
    }
}
