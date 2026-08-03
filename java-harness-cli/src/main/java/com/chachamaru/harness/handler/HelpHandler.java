package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Help command handler.
 * Displays help information for commands.
 */
public class HelpHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(HelpHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            if (args.length == 0) {
                showGeneralHelp();
            } else {
                showCommandHelp(args[0]);
            }

        } catch (Exception e) {
            logger.error("Help command failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private void showGeneralHelp() {
        System.out.println("Java Harness CLI - Version 5.0.0-java");
        System.out.println("====================================");
        System.out.println();
        System.out.println("Usage: java-harness <command> [args...]");
        System.out.println();
        System.out.println("Available Commands:");
        System.out.println();
        System.out.println("Core Commands:");
        System.out.println("  plan                    Generate plan prompt");
        System.out.println("  work <taskID>           Execute work task");
        System.out.println("  review <taskID>         Review completed work");
        System.out.println("  release [--check]       Prepare release");
        System.out.println("  sync [root]            Sync configuration");
        System.out.println();
        System.out.println("Utility Commands:");
        System.out.println("  init [root]            Initialize project");
        System.out.println("  validate <type>         Validate skills/agents");
        System.out.println("  doctor [--migration]    Health check");
        System.out.println("  status [--json]        Show project status");
        System.out.println("  gen [options]          Generate content");
        System.out.println();
        System.out.println("Management Commands:");
        System.out.println("  sprint-contract        Manage sprint contracts");
        System.out.println("  evidence                Collect and report evidence");
        System.out.println("  completion <shell>     Generate shell completions");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --version, -v          Show version information");
        System.out.println("  --help, -h             Show this help message");
        System.out.println();
        System.out.println("For more detailed help on a specific command:");
        System.out.println("  java-harness help <command>");
        System.out.println();
        System.out.println("For complete documentation:");
        System.out.println("  https://github.com/Chachamaru127/java-harness");
    }

    private void showCommandHelp(String command) {
        switch (command.toLowerCase()) {
            case "plan":
                showPlanHelp();
                break;
            case "work":
                showWorkHelp();
                break;
            case "review":
                showReviewHelp();
                break;
            case "release":
                showReleaseHelp();
                break;
            case "sync":
                showSyncHelp();
                break;
            case "init":
                showInitHelp();
                break;
            case "validate":
                showValidateHelp();
                break;
            case "doctor":
                showDoctorHelp();
                break;
            case "status":
                showStatusHelp();
                break;
            case "gen":
                showGenHelp();
                break;
            case "evidence":
                showEvidenceHelp();
                break;
            case "sprint-contract":
                showSprintContractHelp();
                break;
            case "completion":
                showCompletionHelp();
                break;
            default:
                System.err.println("Unknown command: " + command);
                System.err.println("Run 'java-harness help' to see available commands");
                System.exit(1);
        }
    }

    private void showPlanHelp() {
        System.out.println("Command: plan");
        System.out.println("Description: Generate plan prompt for task planning");
        System.out.println();
        System.out.println("Usage: java-harness plan [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --brief               Generate brief plan");
        System.out.println("  --output <file>       Save plan to file");
        System.out.println("  --format <type>       Plan format (markdown, json)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness plan");
        System.out.println("  java-harness plan --brief");
        System.out.println("  java-harness plan --output plan.md");
    }

    private void showWorkHelp() {
        System.out.println("Command: work");
        System.out.println("Description: Execute work task with specified backend");
        System.out.println();
        System.out.println("Usage: java-harness work <taskID> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --backend <type>      Backend type (codex, cursor, auto)");
        System.out.println("  --effort <level>      Effort level (low, medium, high, max)");
        System.out.println("  --auto-review          Enable automatic review");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness work 1.1");
        System.out.println("  java-harness work 1.1 --backend cursor");
        System.out.println("  java-harness work 1.1 --effort high");
    }

    private void showReviewHelp() {
        System.out.println("Command: review");
        System.out.println("Description: Review completed work");
        System.out.println();
        System.out.println("Usage: java-harness review <taskID> [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --strict              Enable strict review mode");
        System.out.println("  --format <type>       Review format (text, json)");
        System.out.println("  --max-rounds <n>      Maximum review rounds");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness review 1.1");
        System.out.println("  java-harness review 1.1 --strict");
    }

    private void showReleaseHelp() {
        System.out.println("Command: release");
        System.out.println("Description: Prepare release with version bump and changelog");
        System.out.println();
        System.out.println("Usage: java-harness release [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --check               Check release readiness");
        System.out.println("  --bump <type>         Version bump type (major, minor, patch)");
        System.out.println("  --dry-run             Show what would be done");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness release");
        System.out.println("  java-harness release --check");
        System.out.println("  java-harness release --bump minor");
    }

    private void showSyncHelp() {
        System.out.println("Command: sync");
        System.out.println("Description: Sync configuration and state");
        System.out.println();
        System.out.println("Usage: java-harness sync [root]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness sync");
        System.out.println("  java-harness sync /project/root");
    }

    private void showInitHelp() {
        System.out.println("Command: init");
        System.out.println("Description: Initialize new harness project");
        System.out.println();
        System.out.println("Usage: java-harness init [root] [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --force               Overwrite existing config");
        System.out.println("  --template <name>     Use specific template");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness init");
        System.out.println("  java-harness init /path/to/project");
        System.out.println("  java-harness init --force");
    }

    private void showValidateHelp() {
        System.out.println("Command: validate");
        System.out.println("Description: Validate skills, agents, or configuration");
        System.out.println();
        System.out.println("Usage: java-harness validate <type> [root]");
        System.out.println();
        System.out.println("Types:");
        System.out.println("  skills                Validate skill files");
        System.out.println("  agents                Validate agent configuration");
        System.out.println("  all                   Validate everything");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness validate skills");
        System.out.println("  java-harness validate all");
    }

    private void showDoctorHelp() {
        System.out.println("Command: doctor");
        System.out.println("Description: Health check and diagnostics");
        System.out.println();
        System.out.println("Usage: java-harness doctor [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --migration            Check migration status");
        System.out.println("  --fix                  Attempt to fix issues");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness doctor");
        System.out.println("  java-harness doctor --migration");
    }

    private void showStatusHelp() {
        System.out.println("Command: status");
        System.out.println("Description: Show project status and statistics");
        System.out.println();
        System.out.println("Usage: java-harness status [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --json                Output in JSON format");
        System.out.println("  --verbose             Show detailed information");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness status");
        System.out.println("  java-harness status --json");
    }

    private void showGenHelp() {
        System.out.println("Command: gen");
        System.out.println("Description: Generate content from templates");
        System.out.println();
        System.out.println("Usage: java-harness gen [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --prompt <text>       Custom prompt");
        System.out.println("  --output <file>       Save to file");
        System.out.println("  --template <name>     Template to use");
        System.out.println();
        System.out.println("Templates:");
        System.out.println("  plan                  Plan template");
        System.out.println("  review                Review template");
        System.out.println("  release               Release template");
        System.out.println("  default               General template");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness gen");
        System.out.println("  java-harness gen --template plan");
        System.out.println("  java-harness gen --output doc.md");
    }

    private void showEvidenceHelp() {
        System.out.println("Command: evidence");
        System.out.println("Description: Collect and report evidence for tasks");
        System.out.println();
        System.out.println("Usage: java-harness evidence <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  collect               Collect evidence for task");
        System.out.println("  report                Generate evidence report");
        System.out.println("  list                  List all evidence files");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --task <taskId>       Task ID");
        System.out.println("  --type <type>         Evidence type (tests, code, docs)");
        System.out.println("  --format <format>     Report format (text, json)");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness evidence collect --task 1.1");
        System.out.println("  java-harness evidence report --format json");
    }

    private void showSprintContractHelp() {
        System.out.println("Command: sprint-contract");
        System.out.println("Description: Manage sprint contracts for task tracking");
        System.out.println();
        System.out.println("Usage: java-harness sprint-contract <command> [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  generate              Generate new contract");
        System.out.println("  validate              Validate existing contract");
        System.out.println("  list                  List all contracts");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --task <taskId>       Task ID (required for generate)");
        System.out.println("  --contract <file>     Contract file (required for validate)");
        System.out.println("  --force               Overwrite existing");
        System.out.println("  --strict              Enable strict validation");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness sprint-contract generate --task 1.1");
        System.out.println("  java-harness sprint-contract validate --contract contract.json");
    }

    private void showCompletionHelp() {
        System.out.println("Command: completion");
        System.out.println("Description: Generate shell completion scripts");
        System.out.println();
        System.out.println("Usage: java-harness completion <shell> [options]");
        System.out.println();
        System.out.println("Shells:");
        System.out.println("  bash                  Bash completion");
        System.out.println("  zsh                   Zsh completion");
        System.out.println("  fish                  Fish completion");
        System.out.println("  powershell            PowerShell completion");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --install             Install completion script");
        System.out.println("  --uninstall           Uninstall completion script");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java-harness completion bash --install");
        System.out.println("  java-harness completion zsh");
    }
}
