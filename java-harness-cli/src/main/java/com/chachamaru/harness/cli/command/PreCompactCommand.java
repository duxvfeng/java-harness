package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Evaluate whether PreCompact should be blocked
 */
@Command(name = "pre-compact",
         description = "Evaluate whether PreCompact should be blocked")
public class PreCompactCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PreCompactCommand executed");
    }
}
