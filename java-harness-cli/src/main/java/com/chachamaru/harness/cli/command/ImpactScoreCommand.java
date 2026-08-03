package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Compute judgment-card impact_score
 */
@Command(name = "impact-score",
         description = "Compute judgment-card impact_score")
public class ImpactScoreCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("ImpactScoreCommand executed");
    }
}
