package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * PostCompact hook command for Harness CLI.
 *
 * <p>PostCompact WIP context re-injection.</p>
 */
@Command(name = "post-compact",
         description = "PostCompact WIP context re-injection")
public class PostCompactCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PostCompact WIP context re-injection...");
    }
}
