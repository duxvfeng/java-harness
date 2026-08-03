package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Emit trace events
 */
@Command(name = "emit-trace",
         description = "Emit trace events")
public class EmitTraceCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("EmitTraceCommand executed");
    }
}
