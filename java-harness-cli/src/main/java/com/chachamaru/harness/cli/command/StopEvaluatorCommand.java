package com.chachamaru.harness.cli.command;

import picocli.CommandLine.Command;

/**
 * Stop evaluator
 */
@Command(name = "stop-evaluator",
         description = "Stop evaluator")
public class StopEvaluatorCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("StopEvaluatorCommand executed");
    }
}
