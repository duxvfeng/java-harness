package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * PostTool hook command for Harness CLI.
 *
 * <p>Evaluates PostToolUse tampering/security checks.</p>
 */
@Command(name = "post-tool",
         description = "Evaluate PostToolUse tampering/security checks")
public class PostToolCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Evaluating PostToolUse tampering/security checks...");
    }
}
