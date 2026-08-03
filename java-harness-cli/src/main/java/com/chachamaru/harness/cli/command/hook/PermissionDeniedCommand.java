package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * PermissionDenied hook command for Harness CLI.
 *
 * <p>PermissionDenied event logging.</p>
 */
@Command(name = "permission-denied",
         description = "PermissionDenied event logging")
public class PermissionDeniedCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PermissionDenied event logging...");
    }
}
