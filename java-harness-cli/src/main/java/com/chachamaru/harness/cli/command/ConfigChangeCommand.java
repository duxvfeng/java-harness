package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Handle configuration changes
 */
@Command(name = "config-change",
         description = "Handle configuration changes")
public class ConfigChangeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("ConfigChangeCommand executed");
    }
}
