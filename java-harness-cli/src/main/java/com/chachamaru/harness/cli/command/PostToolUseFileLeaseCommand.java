package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Post tool use file lease handler
 */
@Command(name = "post-tool-use-file-lease",
         description = "Post tool use file lease handler")
public class PostToolUseFileLeaseCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PostToolUseFileLeaseCommand executed");
    }
}
