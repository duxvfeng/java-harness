package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Log tool name usage
 */
@Command(name = "log-toolname",
         description = "Log tool name usage")
public class LogToolnameCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("LogToolnameCommand executed");
    }
}
