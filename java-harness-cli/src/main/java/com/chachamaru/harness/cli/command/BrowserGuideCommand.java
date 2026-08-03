package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Open browser guide
 */
@Command(name = "browser-guide",
         description = "Open browser guide")
public class BrowserGuideCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("BrowserGuideCommand executed");
    }
}
