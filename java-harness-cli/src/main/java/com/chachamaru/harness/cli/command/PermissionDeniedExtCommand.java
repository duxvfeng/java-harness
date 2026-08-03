package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Extended permission denied handler
 */
@Command(name = "permission-denied-ext",
         description = "Extended permission denied handler")
public class PermissionDeniedExtCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PermissionDeniedExtCommand executed");
    }
}
