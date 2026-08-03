package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Memory bridge operations
 */
@Command(name = "memory-bridge",
         description = "Memory bridge operations")
public class MemoryBridgeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("MemoryBridgeCommand executed");
    }
}
