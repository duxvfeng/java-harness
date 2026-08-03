package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * CiStatusHook hook command for Harness CLI.
 *
 * <p>PostToolUse: CI status check after push/PR.</p>
 */
@Command(name = "ci-status",
         description = "PostToolUse: CI status check after push/PR")
public class CiStatusHookCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PostToolUse: CI status check after push/PR...");
    }
}
