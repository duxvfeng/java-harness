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

    // 可测试的退出接口（用于依赖注入）
    interface ExitHandler {
        void exit(int code);
    }

    private static ExitHandler exitHandler = code -> System.exit(code);

    public static void main(String[] args) {
        int exitCode = execute(args);
        exitHandler.exit(exitCode);
    }

    // 可测试的执行方法
    static int execute(String[] args) {
        if (args.length == 0) {
            printUsage();
            return 1;
        }

        String command = args[0];
        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);

        if (command.equals("--version") || command.equals("-v")) {
            System.out.println("java-harness " + VERSION);
            return 0;
        }

        if (command.equals("help") || command.equals("--help") || command.equals("-h")) {
            printUsage();
            return 0;
        }

        CommandHandler handler = CommandRegistry.getHandler(command);
        if (handler == null) {
            System.err.println("Unknown command: " + command);
            printUsage();
            return 1;
        }

        try {
            handler.execute(commandArgs);
            return 0;
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private static void printUsage() {
        System.err.println("Usage: java-harness <command> [args...]");
        System.err.println("");
        System.err.println("Commands:");
        System.err.println("  Core:");
        System.err.println("    plan                    Generate plan prompt");
        System.err.println("    work <taskID>           Execute work task");
        System.err.println("    review <taskID>         Review completed work");
        System.err.println("    release [--check]       Prepare release");
        System.err.println("    sync [root]            Sync configuration");
        System.err.println("");
        System.err.println("  Utilities:");
        System.err.println("    init [root]            Initialize project");
        System.err.println("    validate [skills|agents|all] [root]  Validate skills/agents");
        System.err.println("    doctor [--migration] [root]  Health check");
        System.err.println("    status [--json]        Show project status");
        System.err.println("");
        System.err.println("  Generation:");
        System.err.println("    gen [--prompt] [--output]  Generate content");
        System.err.println("    sprint-contract <command>  Manage sprint contracts");
        System.err.println("    evidence <command>     Collect and report evidence");
        System.err.println("");
        System.err.println("  CLI Tools:");
        System.err.println("    version                 Show version information");
        System.err.println("    help [command]          Show help for command");
        System.err.println("    completion <shell>     Generate shell completions");
        System.err.println("");
        System.err.println("  Hooks:");
        System.err.println("    hook <type>             Execute hook handler");
        System.err.println("");
        System.err.println("  --version, -v           Print version");
        System.err.println("  help, --help, -h        Show this help");
    }

    // 设置测试用的退出处理器（仅供测试使用）
    static void setExitHandlerForTesting(ExitHandler handler) {
        exitHandler = handler;
    }
}
