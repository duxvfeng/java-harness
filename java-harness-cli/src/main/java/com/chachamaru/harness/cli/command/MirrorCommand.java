package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Report skills/ mirror drift
 */
@Command(name = "mirror",
         description = "Report skills/ mirror drift")
public class MirrorCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("MirrorCommand executed");
    }
}
