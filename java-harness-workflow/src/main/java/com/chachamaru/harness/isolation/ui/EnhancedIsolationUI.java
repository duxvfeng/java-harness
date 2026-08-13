package com.chachamaru.harness.isolation.ui;

import com.chachamaru.harness.isolation.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Enhanced user interaction for branch isolation with clear state visibility and options.
 */
public class EnhancedIsolationUI {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedIsolationUI.class);
    private static final Scanner scanner = new Scanner(System.in);

    /**
     * Display current state with clear visual indicators
     */
    public void displayCurrentState(IsolationStateFile state) {
        clearScreen();
        displayBanner("🔀 Branch Isolation State");
        System.out.println();

        if (state == null || !state.hasActiveSeries()) {
            displayInitialState();
        } else {
            displayActiveState(state);
        }

        System.out.println();
    }

    /**
     * Display initial state (no active isolation)
     */
    private void displayInitialState() {
        displayBox("┌──────────────────────────────────────────┐");
        displayBox("│          Current State: Initial          │");
        displayBox("└──────────────────────────────────────────┘");
        System.out.println();

        displayItem("🔀", "No active branch isolation");
        displayItem("📋", "First task in series - setup required");
        System.out.println();
    }

    /**
     * Display active isolation state
     */
    private void displayActiveState(IsolationStateFile state) {
        SeriesInfo series = state.getCurrentSeries();

        // Determine state icon and description
        String icon = getStateIcon(series);
        String status = getStateDisplayName(series);
        String description = getStateDescription(series);

        displayBox("┌──────────────────────────────────────────┐");
        displayBox(String.format("│    Current State: %-21s │", status));
        displayBox("└──────────────────────────────────────────┘");
        System.out.println();

        displayItem(icon, description);
        System.out.println();

        // Display series information
        if (series != null) {
            displaySeriesInfo(series);
        }

        // Display code status
        if (state.getCodeStatus() != null) {
            displayCodeStatus(state.getCodeStatus());
        }
    }

    /**
     * Display series information
     */
    private void displaySeriesInfo(SeriesInfo series) {
        displaySectionHeader("📊 Series Information");
        displayKeyValue("Series ID", series.getSeriesId());
        displayKeyValue("Current Task", String.valueOf(series.getCurrentTask()));
        displayKeyValue("Tasks Completed", String.valueOf(series.getTaskCount()));

        if (series.getSeriesContext() != null) {
            SeriesContext context = series.getSeriesContext();
            if (context.getPurpose() != null) {
                displayKeyValue("Purpose", context.getPurpose());
            }
            if (context.getCompletionPercentage() != null) {
                displayProgressBar("Completion", context.getCompletionPercentage());
            }
        }

        System.out.println();
    }

    /**
     * Display code status information
     */
    private void displayCodeStatus(CodeStatus codeStatus) {
        displaySectionHeader("📁 Code Status");

        String cleanStatus = codeStatus.isBranchClean() ? "✅ Clean" : "⚠️  Modified";
        displayKeyValue("Branch Status", cleanStatus);

        String commitStatus = codeStatus.hasUncommittedChanges() ?
            "⚠️  Uncommitted changes" : "✅ All committed";
        displayKeyValue("Commit Status", commitStatus);

        if (codeStatus.getCommitsCount() != null && codeStatus.getCommitsCount() > 0) {
            displayKeyValue("Total Commits", String.valueOf(codeStatus.getCommitsCount()));
        }

        if (codeStatus.getUntrackedFilesCount() != null && codeStatus.getUntrackedFilesCount() > 0) {
            displayKeyValue("Untracked Files", String.valueOf(codeStatus.getUntrackedFilesCount()));
        }

        System.out.println();
    }

    /**
     * Display isolation options for user to choose from
     */
    public IsolationDecision displayIsolationOptions(BranchType branchType, IsolationStateFile currentState) {
        displayCurrentState(currentState);

        if (currentState != null && currentState.hasActiveSeries()) {
            // Already have active isolation, ask if they want to continue or reset
            return displayActiveSeriesOptions(currentState);
        } else {
            // No active isolation, show setup options
            return displaySetupOptions(branchType);
        }
    }

    /**
     * Display options when there's an active series
     */
    private IsolationDecision displayActiveSeriesOptions(IsolationStateFile state) {
        SeriesInfo series = state.getCurrentSeries();
        CodeStatus codeStatus = state.getCodeStatus();

        displaySectionHeader("🤔 Continue or Reset?");

        if (codeStatus != null && codeStatus.hasUncommittedChanges()) {
            displayItem("⚠️", "You have uncommitted changes in your isolated branch");
            System.out.println();
        }

        if (codeStatus != null && codeStatus.isBranchClean()) {
            displayItem("✅", "Your branch is clean and ready for reset");
            System.out.println();
        }

        System.out.println("Choose an action:");
        System.out.println();
        System.out.println("  1) Continue isolation - Keep working in current branch");
        System.out.println("  2) Reset state - End isolation and return to initial state");
        System.out.println("  3) Show details - View more information about current state");
        System.out.println("  4) Cancel - Stop and decide manually");
        System.out.println();

        return getUserChoiceForActiveSeries(series);
    }

    /**
     * Display setup options for new isolation
     */
    private IsolationDecision displaySetupOptions(BranchType branchType) {
        if (branchType == BranchType.MAIN) {
            displayMainBranchProtection();
            return new IsolationDecision(IsolationDecisionType.ISOLATE,
                "auto-isolate", "Main branch protection required");
        } else {
            displayFeatureBranchOptions();
            return getUserChoiceForFeatureBranch();
        }
    }

    /**
     * Display main branch protection message
     */
    private void displayMainBranchProtection() {
        displaySectionHeader("🔒 Main Branch Protection");

        displayItem("📍", "You are on the main/master branch");
        displayItem("🛡️", "For safety, work will be isolated in a feature branch");
        displayItem("🔧", "The system will automatically create isolated branch");
        displayItem("✅", "No user action required - proceeding automatically");

        System.out.println();
    }

    /**
     * Display feature branch options
     */
    private void displayFeatureBranchOptions() {
        displaySectionHeader("🔀 Feature Branch Detected");

        displayItem("📍", "You are on a feature branch");
        displayItem("💡", "Branch isolation protects your work during development");
        System.out.println();

        System.out.println("Choose an option:");
        System.out.println();
        System.out.println("  1) Isolate branch (recommended) - Create isolated worktree for this task series");
        System.out.println("  2) Skip isolation - Continue directly on current branch");
        System.out.println("  3) Cancel - Stop execution and decide manually");
        System.out.println();
    }

    /**
     * Get user choice for active series
     */
    private IsolationDecision getUserChoiceForActiveSeries(SeriesInfo series) {
        while (true) {
            System.out.print("Your choice [1/2/3/4]: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    System.out.println("✅ Continuing with current isolation");
                    return new IsolationDecision(IsolationDecisionType.CONTINUE,
                        "continue", "User chose to continue current isolation");

                case "2":
                    System.out.println("🔄 Resetting isolation state");
                    return new IsolationDecision(IsolationDecisionType.RESET,
                        "reset", "User chose to reset isolation state");

                case "3":
                    displayDetailedStateInfo(series);
                    break; // Loop back to menu

                case "4":
                    System.out.println("❌ User cancelled execution");
                    return new IsolationDecision(IsolationDecisionType.CANCEL,
                        "cancel", "User cancelled execution");

                default:
                    System.out.println("⚠️  Invalid choice. Please enter 1, 2, 3, or 4.");
                    break;
            }
        }
    }

    /**
     * Get user choice for feature branch
     */
    private IsolationDecision getUserChoiceForFeatureBranch() {
        while (true) {
            System.out.print("Your choice [1/2/3]: ");
            String input = scanner.nextLine().trim();

            switch (input) {
                case "1":
                    System.out.println("✅ Branch isolation will be created");
                    return new IsolationDecision(IsolationDecisionType.ISOLATE,
                        "isolate", "User chose to isolate feature branch");

                case "2":
                    System.out.println("⚠️  Proceeding without branch isolation");
                    return new IsolationDecision(IsolationDecisionType.SKIP,
                        "skip", "User chose to skip isolation");

                case "3":
                    System.out.println("❌ Execution cancelled by user");
                    return new IsolationDecision(IsolationDecisionType.CANCEL,
                        "cancel", "User cancelled execution");

                default:
                    System.out.println("⚠️  Invalid choice. Please enter 1, 2, or 3.");
                    break;
            }
        }
    }

    /**
     * Display detailed state information
     */
    private void displayDetailedStateInfo(SeriesInfo series) {
        clearScreen();
        displayBanner("📊 Detailed State Information");
        System.out.println();

        displaySectionHeader("Series Details");
        displayKeyValue("Series ID", series.getSeriesId());
        displayKeyValue("Started", series.getStartDate().toString());
        displayKeyValue("Last Activity", series.getLastActivityDate().toString());

        if (series.getTaskSequence() != null && !series.getTaskSequence().isEmpty()) {
            displayKeyValue("Task Sequence", series.getTaskSequence().toString());
        }

        if (series.getBranchInfo() != null) {
            displaySectionHeader("Branch Information");
            displayKeyValue("Feature Branch", series.getBranchInfo().getFeatureBranch());
            displayKeyValue("Worktree Path", series.getBranchInfo().getWorktreePath());
            displayKeyValue("Original Branch", series.getBranchInfo().getOriginalBranch());
        }

        System.out.println();
        System.out.println("Press Enter to return to options...");
        scanner.nextLine();
    }

    /**
     * Display reset recommendation
     */
    public void displayResetRecommendation(String explanation) {
        displaySectionHeader("🔄 Reset Recommended");

        System.out.println("Based on current conditions, resetting isolation state is recommended:");
        System.out.println();
        System.out.println(explanation);
        System.out.println();

        displayItem("✅", "Reset will return you to initial state");
        displayItem("🔀", "Next task series will start fresh");
        displayItem("💾", "Your work is safe in the feature branch");

        System.out.println();
    }

    /**
     * Display continue recommendation
     */
    public void displayContinueRecommendation(String explanation) {
        displaySectionHeader("⚡ Continue Current Work");

        System.out.println("Based on current conditions, continuing isolation is recommended:");
        System.out.println();
        System.out.println(explanation);
        System.out.println();

        displayItem("🔒", "Your isolated work is protected");
        displayItem("⚡", "Continue working on current tasks");
        displayItem("💡", "Reset when series is complete");

        System.out.println();
    }

    // Helper methods for UI display

    private void displayBanner(String text) {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  " + text);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void displaySectionHeader(String text) {
        System.out.println("┌─ " + text);
        System.out.println("│");
    }

    private void displayBox(String text) {
        System.out.println(text);
    }

    private void displayItem(String icon, String text) {
        System.out.println("  " + icon + " " + text);
    }

    private void displayKeyValue(String key, String value) {
        System.out.printf("  %-20s: %s%n", key, value);
    }

    private void displayProgressBar(String label, Integer percentage) {
        System.out.printf("  %-20s: [", label);

        int filled = (int) (percentage / 10.0);
        for (int i = 0; i < 10; i++) {
            if (i < filled) {
                System.out.print("█");
            } else {
                System.out.print("░");
            }
        }

        System.out.printf("] %d%%%n", percentage);
    }

    private String getStateIcon(SeriesInfo series) {
        if (series.isAutoResetPending()) {
            return "🔄";
        } else if (series.isIsolationActive()) {
            return "🔒";
        } else {
            return "🔀";
        }
    }

    private String getStateDisplayName(SeriesInfo series) {
        if (series.isAutoResetPending()) {
            return "Ready for Reset";
        } else if (series.isIsolationActive()) {
            return "Active Isolation";
        } else {
            return "Isolated";
        }
    }

    private String getStateDescription(SeriesInfo series) {
        if (series.isAutoResetPending()) {
            return "Branch is clean, ready to reset to initial state";
        } else if (series.isIsolationActive()) {
            return "Working in isolated branch for task series";
        } else {
            return "Branch isolation established for task series";
        }
    }

    private void clearScreen() {
        // Simple screen clear that works on most terminals
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}