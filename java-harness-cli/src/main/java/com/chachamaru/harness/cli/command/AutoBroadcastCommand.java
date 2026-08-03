package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Auto broadcast messages
 */
@Command(name = "auto-broadcast",
         description = "Auto broadcast messages")
public class AutoBroadcastCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("AutoBroadcastCommand executed");
    }
}
