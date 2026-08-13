package com.chachamaru.harness.isolation;

import com.chachamaru.harness.isolation.model.CodeStatus;
import com.chachamaru.harness.isolation.model.IsolationStateFile;
import com.chachamaru.harness.isolation.model.SeriesContext;
import com.chachamaru.harness.isolation.model.SeriesInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Intelligent state reset logic for branch isolation.
 * Determines when to automatically reset isolation state based on code status and activity patterns.
 */
public class IsolationStateReset {

    private static final Logger logger = LoggerFactory.getLogger(IsolationStateReset.class);

    // Default reset conditions
    private static final int DEFAULT_INACTIVITY_HOURS = 4;
    private static final int MAX_INACTIVITY_HOURS = 24;
    private static final int MIN_INACTIVITY_HOURS = 1;

    /**
     * Evaluate whether isolation state should be reset
     */
    public ResetEvaluation evaluateResetConditions(IsolationStateFile state, CodeStatus codeStatus) {
        if (state == null || !state.hasActiveSeries()) {
            return new ResetEvaluation(false, List.of("No active isolation series"),
                "State is already in initial condition");
        }

        List<String> satisfiedConditions = new ArrayList<>();
        String resetExplanation = "";

        // Condition 1: Branch is clean and no uncommitted changes
        if (isBranchCleanReady(codeStatus)) {
            satisfiedConditions.add("branch_clean_and_no_uncommitted_changes");
        }

        // Condition 2: Task series is complete
        if (isTaskSeriesComplete(state, codeStatus)) {
            satisfiedConditions.add("task_series_complete");
        }

        // Condition 3: Inactivity timeout with clean branch
        if (isInactiveWithCleanBranch(state, codeStatus)) {
            satisfiedConditions.add("inactivity_with_clean_branch");
        }

        // Condition 4: User explicitly requested reset (manual)
        if (state.getResetTriggers() != null &&
            state.getCurrentSeries() != null &&
            state.getCurrentSeries().isAutoResetPending()) {
            satisfiedConditions.add("manual_reset_requested");
        }

        // Determine if reset should occur
        boolean shouldReset = !satisfiedConditions.isEmpty() &&
                             meetsResetThreshold(satisfiedConditions);

        if (shouldReset) {
            resetExplanation = generateResetExplanation(satisfiedConditions, state, codeStatus);
        } else {
            resetExplanation = generateContinueExplanation(state, codeStatus);
        }

        return new ResetEvaluation(shouldReset, satisfiedConditions, resetExplanation);
    }

    /**
     * Execute state reset
     */
    public IsolationStateFile executeReset(IsolationStateFile state, String reason) {
        if (state == null) {
            logger.warn("Cannot reset null state, creating new state");
            return new IsolationStateFile();
        }

        logger.info("Executing isolation state reset. Reason: {}", reason);

        // Record the reset in decision history before clearing
        if (state.getCurrentSeries() != null) {
            recordResetDecision(state, reason);
        }

        // Clear current series information
        state.setCurrentSeries(null);

        // Reset code status to defaults
        state.setCodeStatus(new CodeStatus());

        // Keep reset triggers but update them
        if (state.getResetTriggers() != null) {
            state.getResetTriggers().setTaskSeriesComplete(false);
            state.getResetTriggers().setAutoResetCondition("branch_clean_and_no_uncommitted_changes");
        }

        // Update metadata
        if (state.getMetadata() != null) {
            state.getMetadata().markAsUpdated();
        }

        logger.info("Isolation state reset completed successfully");
        return state;
    }

    /**
     * Check if branch is clean and ready for reset
     */
    private boolean isBranchCleanReady(CodeStatus codeStatus) {
        if (codeStatus == null) {
            return false;
        }

        return codeStatus.isBranchClean() &&
               !codeStatus.hasUncommittedChanges() &&
               !codeStatus.hasError();
    }

    /**
     * Check if task series is complete
     */
    private boolean isTaskSeriesComplete(IsolationStateFile state, CodeStatus codeStatus) {
        if (state.getCurrentSeries() == null || state.getCurrentSeries().getSeriesContext() == null) {
            return false;
        }

        SeriesContext context = state.getCurrentSeries().getSeriesContext();

        // Series is considered complete if:
        // 1. Completion percentage is 100% OR
        // 2. All estimated tasks are done OR
        // 3. Branch is clean and no active work
        boolean percentageComplete = context.getCompletionPercentage() != null &&
                                   context.getCompletionPercentage() >= 100;

        boolean tasksComplete = context.getEstimatedTasks() != null &&
                               state.getCurrentSeries().getTaskCount() != null &&
                               state.getCurrentSeries().getTaskCount() >= context.getEstimatedTasks();

        boolean cleanAndInactive = isBranchCleanReady(codeStatus) &&
                                  isInactiveForHours(state.getCurrentSeries().getLastActivityDate(), 1);

        return percentageComplete || tasksComplete || cleanAndInactive;
    }

    /**
     * Check if there's inactivity with clean branch
     */
    private boolean isInactiveWithCleanBranch(IsolationStateFile state, CodeStatus codeStatus) {
        if (state.getCurrentSeries() == null || state.getCurrentSeries().getLastActivityDate() == null) {
            return false;
        }

        int inactivityThreshold = getInactivityThreshold(state);
        return isBranchCleanReady(codeStatus) &&
               isInactiveForHours(state.getCurrentSeries().getLastActivityDate(), inactivityThreshold);
    }

    /**
     * Check if enough time has passed since last activity
     */
    private boolean isInactiveForHours(LocalDateTime lastActivity, int hours) {
        if (lastActivity == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long hoursSinceActivity = ChronoUnit.HOURS.between(lastActivity, now);
        return hoursSinceActivity >= hours;
    }

    /**
     * Get inactivity threshold from state or use default
     */
    private int getInactivityThreshold(IsolationStateFile state) {
        if (state.getResetTriggers() != null &&
            state.getResetTriggers().getAutoResetAfterHours() != null) {

            int configuredHours = state.getResetTriggers().getAutoResetAfterHours();

            // Clamp to reasonable range
            return Math.max(MIN_INACTIVITY_HOURS,
                           Math.min(MAX_INACTIVITY_HOURS, configuredHours));
        }

        return DEFAULT_INACTIVITY_HOURS;
    }

    /**
     * Check if reset conditions meet threshold for automatic reset
     */
    private boolean meetsResetThreshold(List<String> satisfiedConditions) {
        // Reset if any condition is met for now
        // Could implement more complex logic later (e.g., require 2+ conditions)
        return !satisfiedConditions.isEmpty();
    }

    /**
     * Generate explanation for why reset should occur
     */
    private String generateResetExplanation(List<String> conditions, IsolationStateFile state, CodeStatus codeStatus) {
        StringBuilder explanation = new StringBuilder("Branch isolation state should be reset because:\n");

        for (String condition : conditions) {
            switch (condition) {
                case "branch_clean_and_no_uncommitted_changes":
                    explanation.append("- Branch is clean with no uncommitted changes\n");
                    break;
                case "task_series_complete":
                    explanation.append("- Task series is complete (")
                              .append(state.getCurrentSeries().getTaskCount())
                              .append(" tasks completed)\n");
                    break;
                case "inactivity_with_clean_branch":
                    int hours = getInactivityThreshold(state);
                    explanation.append("- No activity for ")
                              .append(hours)
                              .append(" hours and branch is clean\n");
                    break;
                case "manual_reset_requested":
                    explanation.append("- User requested manual state reset\n");
                    break;
                default:
                    explanation.append("- ").append(condition).append("\n");
                    break;
            }
        }

        return explanation.toString().trim();
    }

    /**
     * Generate explanation for why reset should NOT occur
     */
    private String generateContinueExplanation(IsolationStateFile state, CodeStatus codeStatus) {
        StringBuilder explanation = new StringBuilder("Branch isolation state should continue because:\n");

        if (codeStatus != null && codeStatus.hasUncommittedChanges()) {
            explanation.append("- There are uncommitted changes that need to be committed\n");
        }

        if (codeStatus != null && !codeStatus.isBranchClean()) {
            explanation.append("- Branch has uncommitted changes\n");
        }

        if (state.getCurrentSeries() != null && !isTaskSeriesComplete(state, codeStatus)) {
            explanation.append("- Task series is still in progress (")
                      .append(state.getCurrentSeries().getCurrentTask())
                      .append(" of ")
                      .append(state.getCurrentSeries().getEstimatedTasks())
                      .append(")\n");
        }

        if (state.getCurrentSeries() != null && state.getCurrentSeries().getLastActivityDate() != null) {
            long minutesSinceActivity = ChronoUnit.MINUTES.between(
                state.getCurrentSeries().getLastActivityDate(), LocalDateTime.now());
            explanation.append("- Recent activity (")
                      .append(minutesSinceActivity)
                      .append(" minutes ago) suggests ongoing work\n");
        }

        return explanation.toString().trim();
    }

    /**
     * Record reset decision in history
     */
    private void recordResetDecision(IsolationStateFile state, String reason) {
        if (state.getCurrentSeries() == null) {
            return;
        }

        logger.debug("Recording reset decision in history");

        // Create a decision record for the reset
        com.chachamaru.harness.isolation.model.DecisionRecord resetRecord =
            new com.chachamaru.harness.isolation.model.DecisionRecord(
                state.getCurrentSeries().getSeriesId(),
                state.getCurrentSeries().getCurrentTask(),
                "reset",
                reason
            );
        resetRecord.setInteractionType("automatic");
        resetRecord.setUserChoice("n/a");
        resetRecord.setWorktreePath(state.getCurrentSeries().getBranchInfo() != null ?
            state.getCurrentSeries().getBranchInfo().getWorktreePath() : null);

        state.addDecisionRecord(resetRecord);
    }

    /**
     * Check if auto-reset is enabled
     */
    public boolean isAutoResetEnabled(IsolationStateFile state) {
        return state != null &&
               state.getResetTriggers() != null &&
               state.getResetTriggers().isAutoResetEnabled();
    }

    /**
     * Set manual reset pending flag
     */
    public void setManualResetPending(IsolationStateFile state, boolean pending) {
        if (state != null && state.getCurrentSeries() != null) {
            state.getCurrentSeries().setAutoResetPending(pending);

            if (pending && state.getResetTriggers() != null) {
                state.getResetTriggers().setTaskSeriesComplete(true);
            }
        }
    }

    /**
     * Get current inactivity threshold in hours
     */
    public int getInactivityThresholdHours(IsolationStateFile state) {
        return getInactivityThreshold(state);
    }

    /**
     * Set inactivity threshold
     */
    public void setInactivityThreshold(IsolationStateFile state, int hours) {
        if (state != null && state.getResetTriggers() != null) {
            int clampedHours = Math.max(MIN_INACTIVITY_HOURS,
                                     Math.min(MAX_INACTIVITY_HOURS, hours));
            state.getResetTriggers().setAutoResetAfterHours(clampedHours);
        }
    }
}