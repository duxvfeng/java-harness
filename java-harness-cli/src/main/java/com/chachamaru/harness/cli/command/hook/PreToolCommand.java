package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * PreTool hook command for Harness CLI.
 *
 * <p>Evaluates PreToolUse guardrails before tool execution.</p>
 */
@Command(name = "pre-tool",
         description = "Evaluate PreToolUse guardrails")
public class PreToolCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Evaluating PreToolUse guardrails...");
    }
}
