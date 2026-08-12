package com.chachamaru.harness;

import com.chachamaru.harness.handler.CommandHandler;
import com.chachamaru.harness.handler.CommandRegistry;
import com.chachamaru.harness.hook.HookDispatcher;
import com.chachamaru.harness.skill.SkillExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

/**
 * Main entry point for Java Harness CLI.
 * This is the primary entry point that replaces the picocli-based HarnessCLI.
 *
 * Usage: java-harness <command> [args...]
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
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

        // Try to get handler first
        CommandHandler handler = CommandRegistry.getHandler(command);

        if (handler == null) {
            // No handler found, try routing to skill
            String skillName = SkillExecutor.mapCommandToSkill(command);
            if (skillName != null) {
                logger.info("Routing to skill: {}", skillName);
                boolean executed = SkillExecutor.executeSkill(skillName, commandArgs);
                if (executed) {
                    return 0;
                } else {
                    System.err.println("Error: Failed to execute skill: " + skillName);
                    return 1;
                }
            }

            // Not found in skills either
            System.err.println("Unknown command: " + command);
            printUsage();
            return 1;
        }

        try {
            handler.execute(commandArgs);
            if (handler instanceof HookDispatcher hookDispatcher) {
                return hookDispatcher.lastExitCode();
            }
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
        System.err.println("  Core (route to skills):");
        System.err.println("    plan                    Route to harness-plan skill");
        System.err.println("    work                    Route to harness-work skill");
        System.err.println("    review                  Route to harness-review skill");
        System.err.println("    release                 Route to harness-release skill");
        System.err.println("    sync                    Route to harness-sync skill");
        System.err.println("");
        System.err.println("  Utilities:");
        System.err.println("    init [root]            Initialize project");
        System.err.println("    validate [skills|agents|all] [root]  Validate skills/agents");
        System.err.println("    doctor [--migration] [root]  Health check");
        System.err.println("    status [--json]        Show project status");
        System.err.println("");
        System.err.println("  Tools:");
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
        System.err.println();
        System.err.println("Note: Core commands invoke corresponding skills (harness-*)");
    }

    // 设置测试用的退出处理器（仅供测试使用）
    static void setExitHandlerForTesting(ExitHandler handler) {
        exitHandler = handler;
    }
}
