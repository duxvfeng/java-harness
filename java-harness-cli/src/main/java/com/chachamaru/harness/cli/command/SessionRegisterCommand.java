package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Register session
 */
@Command(name = "session-register",
         description = "Register session")
public class SessionRegisterCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SessionRegisterCommand executed");
    }
}
