package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Quality pack operations
 */
@Command(name = "quality-pack",
         description = "Quality pack operations")
public class QualityPackCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("QualityPackCommand executed");
    }
}
