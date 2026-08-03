package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Runtime reactive handler
 */
@Command(name = "runtime-reactive",
         description = "Runtime reactive handler")
public class RuntimeReactiveCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("RuntimeReactiveCommand executed");
    }
}
