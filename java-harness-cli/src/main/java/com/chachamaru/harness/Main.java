package com.chachamaru.harness;

import com.chachamaru.harness.handler.CommandHandler;
import com.chachamaru.harness.handler.CommandRegistry;
import java.util.Arrays;

/**
 * Main entry point for Java Harness CLI.
 * This is the primary entry point that replaces the picocli-based HarnessCLI.
 *
 * Usage: java-harness <command> [args...]
 */
public class Main {
    private static final String VERSION = "5.0.0-java";

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        if (command.equals("--version") || command.equals("-v")) {
            System.out.println("java-harness " + VERSION);
            System.exit(0);
        }

        if (command.equals("help") || command.equals("--help") || command.equals("-h")) {
            printUsage();
            System.exit(0);
        }

        CommandHandler handler = CommandRegistry.getHandler(command);
        if (handler == null) {
            System.err.println("Unknown command: " + command);
            printUsage();
            System.exit(1);
        }

        try {
            handler.execute(commandArgs);
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java-harness <command> [args...]");
        System.err.println("");
        System.err.println("Commands:");
        System.err.println("  plan                    Generate plan prompt");
        System.err.println("  work <taskID>           Execute work task");
        System.err.println("  review <taskID>         Review completed work");
        System.err.println("  release [--check]       Prepare release");
        System.err.println("  sync [root]            Sync configuration");
        System.err.println("  init [root]            Initialize project");
        System.err.println("  validate [skills|agents|all] [root]  Validate skills/agents");
        System.err.println("  doctor [--migration] [root]  Health check");
        System.err.println("  hook <type>             Execute hook handler");
        System.err.println("");
        System.err.println("  --version, -v           Print version");
        System.err.println("  help, --help, -h        Show this help");
    }
}
