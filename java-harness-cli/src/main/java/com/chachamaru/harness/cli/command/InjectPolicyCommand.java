package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Inject policy rules
 */
@Command(name = "inject-policy",
         description = "Inject policy rules")
public class InjectPolicyCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("InjectPolicyCommand executed");
    }
}
