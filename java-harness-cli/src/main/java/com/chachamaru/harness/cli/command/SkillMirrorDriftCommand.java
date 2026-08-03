package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Detect skill mirror drift
 */
@Command(name = "skill-mirror-drift",
         description = "Detect skill mirror drift")
public class SkillMirrorDriftCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("SkillMirrorDriftCommand executed");
    }
}
