package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * Permission hook command for Harness CLI.
 *
 * <p>Evaluates PermissionRequest auto-approval.</p>
 */
@Command(name = "permission",
         description = "Evaluate PermissionRequest auto-approval")
public class PermissionCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("Evaluating PermissionRequest auto-approval...");
    }
}
