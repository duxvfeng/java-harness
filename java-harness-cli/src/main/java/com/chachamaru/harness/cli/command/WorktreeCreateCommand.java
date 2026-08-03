package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Create git worktree
 */
@Command(name = "worktree-create",
         description = "Create git worktree")
public class WorktreeCreateCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("WorktreeCreateCommand executed");
    }
}
