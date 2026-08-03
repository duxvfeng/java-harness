package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Save state before compact
 */
@Command(name = "pre-compact-save",
         description = "Save state before compact")
public class PreCompactSaveCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PreCompactSaveCommand executed");
    }
}
