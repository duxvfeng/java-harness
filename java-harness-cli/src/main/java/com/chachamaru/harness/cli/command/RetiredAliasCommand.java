package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Scan repo for retired alias residue
 */
@Command(name = "retired-alias",
         description = "Scan repo for retired alias residue")
public class RetiredAliasCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("RetiredAliasCommand executed");
    }
}
