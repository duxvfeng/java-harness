package com.chachamaru.harness.cli.command.hook;

import picocli.CommandLine.Command;

/**
 * AskUserQuestionNormalize hook command for Harness CLI.
 *
 * <p>PreToolUse AskUserQuestion answer bridge.</p>
 */
@Command(name = "ask-user-question-normalize",
         description = "PreToolUse AskUserQuestion answer bridge")
public class AskUserQuestionNormalizeCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("PreToolUse AskUserQuestion answer bridge...");
    }
}
