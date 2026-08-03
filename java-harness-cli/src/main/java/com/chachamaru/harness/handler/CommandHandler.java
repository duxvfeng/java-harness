package com.chachamaru.harness.handler;

/**
 * Command handler interface.
 * All command handlers must implement this interface.
 */
public interface CommandHandler {
    /**
     * Execute the command with given arguments.
     *
     * @param args Command arguments (excluding the command name itself)
     */
    void execute(String[] args);
}
