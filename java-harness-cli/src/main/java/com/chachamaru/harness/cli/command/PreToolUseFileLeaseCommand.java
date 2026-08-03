package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Pre tool use file lease handler
 */
@Command(name = "pre-tool-use-file-lease",
         description = "Pre tool use file lease handler")
public class PreToolUseFileLeaseCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PreToolUseFileLeaseCommand executed");
    }
}
