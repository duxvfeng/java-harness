package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Remove git worktree
 */
@Command(name = "worktree-remove",
         description = "Remove git worktree")
public class WorktreeRemoveCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("WorktreeRemoveCommand executed");
    }
}
