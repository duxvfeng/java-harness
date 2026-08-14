package com.chachamaru.harness.isolation.integration;

import com.chachamaru.harness.isolation.*;
import com.chachamaru.harness.isolation.model.IsolationStateFile;
import com.chachamaru.harness.isolation.model.SeriesInfo;
import com.chachamaru.harness.isolation.model.BranchInfo;
import com.chachamaru.harness.isolation.model.DecisionRecord;
import com.chachamaru.harness.isolation.ui.*;
import com.chachamaru.harness.isolation.model.CodeStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Integration point for enhanced branch isolation state management in harness-work flow.
 * This class integrates the new state persistence system into the Phase A preparation stage.
 */
public class HarnessWorkIsolationIntegration {

    private static final Logger logger = LoggerFactory.getLogger(HarnessWorkIsolationIntegration.class);

    private final IsolationStateManager stateManager;
    private final CodeStatusDetector codeDetector;
    private final IsolationStateReset stateReset;
    private final EnhancedIsolationUI ui;

    public HarnessWorkIsolationIntegration() {
        this(new IsolationStateManager(), new CodeStatusDetector(),
            new IsolationStateReset(), new EnhancedIsolationUI());
    }

    public HarnessWorkIsolationIntegration(IsolationStateManager stateManager,
                                           CodeStatusDetector codeDetector,
                                           IsolationStateReset stateReset,
                                           EnhancedIsolationUI ui) {
        this.stateManager = stateManager;
        this.codeDetector = codeDetector;
        this.stateReset = stateReset;
        this.ui = ui;
    }

    /**
     * Phase A integration: Enhanced branch isolation detection and state management
     * Called during harness-work Phase A preparation stage
     */
    public IsolationDecision handlePhaseABranchIsolation(String taskId, String taskTitle, String worktreePath) {
        logger.info("Phase A: Enhanced branch isolation detection for task {}", taskId);

        try {
            // 1. Load current isolation state
            IsolationStateFile state = stateManager.loadStateSafely();
            logger.debug("Loaded isolation state: {}", state.getCurrentSeries());

            // 2. Detect current code status
            CodeStatus codeStatus = (state.getCurrentSeries() != null && state.getCurrentSeries().getBranchInfo() != null) ?
                codeDetector.detectCodeStatus(state.getCurrentSeries().getBranchInfo().getWorktreePath()) :
                new CodeStatus();

            state.setCodeStatus(codeStatus);

            // 3. Check if reset is needed
            var resetEvaluation = stateReset.evaluateResetConditions(state, codeStatus);

            // 4. Execute reset if conditions met
            if (resetEvaluation.shouldReset()) {
                logger.info("Automatic reset triggered: {}", resetEvaluation.getExplanation());
                ui.displayResetRecommendation(resetEvaluation.getExplanation());

                // Execute reset automatically (user can override)
                state = stateReset.executeReset(state, "Automatic reset: " + resetEvaluation.getExplanation());
                stateManager.saveState(state);

                // After reset, we need to set up new isolation
                logger.info("State reset completed, setting up new isolation for task {}", taskId);
            }

            // 5. Handle user interaction based on current state
            IsolationDecision decision = isSameTaskSeries(state, taskId)
                ? new IsolationDecision(IsolationDecisionType.CONTINUE, "continue",
                    "Continuing the active task series without prompting")
                : handleUserInteraction(state, taskId, taskTitle, worktreePath);

            // 6. Update state based on decision
            updateStateBasedOnDecision(state, decision, taskId, taskTitle, worktreePath);

            // 7. Save updated state
            stateManager.saveState(state);

            logger.info("Phase A isolation handling completed: {}", decision.getDecisionType());
            return decision;

        } catch (Exception e) {
            logger.error("Failed to handle Phase A branch isolation", e);

            // Fallback to conservative behavior
            return new IsolationDecision(IsolationDecisionType.SKIP, "error-fallback",
                "State management failed, continuing without isolation: " + e.getMessage());
        }
    }

    /**
     * Handle user interaction based on current state
     */
    private IsolationDecision handleUserInteraction(IsolationStateFile state, String taskId,
                                                   String taskTitle, String worktreePath) {
        // Determine branch type
        BranchType branchType = determineBranchType(state, worktreePath);

        // Display current state and get user decision
        IsolationDecision decision = ui.displayIsolationOptions(branchType, state);

        // If user chose to show details, we need to continue with the actual decision
        if (decision.getDecisionType() == null) {
            // This shouldn't happen, but handle it defensively
            logger.warn("UI returned null decision type, defaulting to SKIP");
            return new IsolationDecision(IsolationDecisionType.SKIP, "fallback",
                "UI interaction failed, defaulting to skip isolation");
        }

        return decision;
    }

    /**
     * Update state based on user decision
     */
    private void updateStateBasedOnDecision(IsolationStateFile state, IsolationDecision decision,
                                           String taskId, String taskTitle, String worktreePath) {
        switch (decision.getDecisionType()) {
            case ISOLATE:
                setupNewIsolation(state, taskId, taskTitle, worktreePath);
                break;

            case CONTINUE:
                continueCurrentIsolation(state, taskId);
                break;

            case RESET:
                executeStateReset(state, decision.getReason());
                break;

            case SKIP:
            case CANCEL:
                // No state changes needed for skip/cancel
                break;
        }
    }

    private boolean isSameTaskSeries(IsolationStateFile state, String taskId) {
        if (!state.hasActiveSeries() || state.getCurrentSeries().getSeriesContext() == null) {
            return false;
        }
        String phaseName = state.getCurrentSeries().getSeriesContext().getPhaseName();
        return phaseName != null && phaseName.equals(extractPhaseName(taskId));
    }

    /**
     * Setup new isolation for task series
     */
    private void setupNewIsolation(IsolationStateFile state, String taskId, String taskTitle, String worktreePath) {
        logger.info("Setting up new isolation for task {}", taskId);

        // Create new series info
        String seriesId = generateSeriesId(taskId, taskTitle);
        SeriesInfo series = new SeriesInfo(seriesId);
        series.setIsolationActive(true);
        series.setAutoResetPending(false);

        // Create branch info
        BranchInfo branchInfo = createBranchInfo(worktreePath);
        series.setBranchInfo(branchInfo);

        // Create series context
        var context = com.chachamaru.harness.isolation.model.SeriesContext.builder()
            .purpose(taskTitle)
            .phaseName(extractPhaseName(taskId))
            .taskType("implementation")
            .build();
        series.setSeriesContext(context);

        // Add first task to sequence
        series.addTaskToSequence(parseTaskId(taskId));

        // Set as current series
        state.setCurrentSeries(series);

        // Record decision
        DecisionRecord record = new DecisionRecord(seriesId, parseTaskId(taskId),
            "isolate", "User chose to isolate task series");
        record.setInteractionType("ask");
        record.setUserChoice("isolate");
        record.setWorktreePath(worktreePath);
        state.addDecisionRecord(record);

        logger.info("New isolation series created: {}", seriesId);
    }

    /**
     * Continue with current isolation
     */
    private void continueCurrentIsolation(IsolationStateFile state, String taskId) {
        if (state.getCurrentSeries() == null) {
            logger.warn("Cannot continue isolation - no current series exists");
            return;
        }

        logger.info("Continuing current isolation for task {}", taskId);

        // Add task to sequence
        state.getCurrentSeries().addTaskToSequence(parseTaskId(taskId));

        // Update activity timestamp
        state.getCurrentSeries().setLastActivityDate(LocalDateTime.now());

        // Record decision
        DecisionRecord record = new DecisionRecord(state.getCurrentSeries().getSeriesId(),
            parseTaskId(taskId), "continue", "User chose to continue current isolation");
        record.setInteractionType("continue");
        record.setUserChoice("continue");
        state.addDecisionRecord(record);

        logger.debug("Task {} added to series {}", taskId, state.getCurrentSeries().getSeriesId());
    }

    /**
     * Execute state reset
     */
    private void executeStateReset(IsolationStateFile state, String reason) {
        logger.info("Executing state reset: {}", reason);

        state = stateReset.executeReset(state, reason);

        try {
            stateManager.saveState(state);
        } catch (Exception e) {
            logger.error("Failed to save state after reset", e);
        }
    }

    /**
     * Determine branch type based on current state
     */
    private BranchType determineBranchType(IsolationStateFile state, String worktreePath) {
        if (state.getCurrentSeries() != null && state.getCurrentSeries().getBranchInfo() != null) {
            String originalBranch = state.getCurrentSeries().getBranchInfo().getOriginalBranch();

            if (originalBranch != null) {
                return classifyBranch(originalBranch);
            }
        }

        if (worktreePath != null && !worktreePath.isBlank()) {
            return classifyBranch(codeDetector.getCurrentBranch(worktreePath));
        }

        return BranchType.FEATURE;
    }

    private BranchType classifyBranch(String branchName) {
        if (branchName == null) {
            return BranchType.FEATURE;
        }

        switch (branchName.toLowerCase()) {
            case "master":
            case "main":
            case "develop":
            case "production":
                return BranchType.MAIN;
            default:
                return BranchType.FEATURE;
        }
    }

    /**
     * Create branch information for isolation
     */
    private BranchInfo createBranchInfo(String worktreePath) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String featureBranch = "feature/task-series-" + timestamp;

        // Try to get current branch as base ref
        String baseRef = null;
        String originalBranch = "master"; // Default assumption

        try {
            // Get current branch
            originalBranch = codeDetector.getCurrentBranch(worktreePath);
            baseRef = codeDetector.getBaseReference(worktreePath);
        } catch (Exception e) {
            logger.warn("Could not detect git information, using defaults", e);
        }

        return new BranchInfo(featureBranch, worktreePath, baseRef, originalBranch);
    }

    /**
     * Generate unique series ID
     */
    private String generateSeriesId(String taskId, String taskTitle) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String sanitizedTitle = taskTitle.replaceAll("[^a-zA-Z0-9-]", "-")
                                      .replaceAll("-+", "-")
                                      .toLowerCase();

        if (sanitizedTitle.length() > 30) {
            sanitizedTitle = sanitizedTitle.substring(0, 30);
        }

        return String.format("task-%s-%s-%s", taskId, sanitizedTitle, timestamp);
    }

    /**
     * Extract phase name from task ID
     */
    private String extractPhaseName(String taskId) {
        try {
            double taskNum = parseTaskId(taskId);
            int phaseNum = (int) taskNum;
        return "phase-" + phaseNum; // Task numbers such as 17.1 belong to phase 17
        } catch (Exception e) {
            return "unknown-phase";
        }
    }

    /**
     * Parse task ID string to double
     */
    private double parseTaskId(String taskId) {
        try {
            // Handle various task ID formats: "17.1", "task-17.1", "#17.1"
            String cleanId = taskId.replaceAll("[^0-9.]", "");
            if (cleanId.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(cleanId);
        } catch (Exception e) {
            logger.warn("Could not parse task ID: {}", taskId);
            return 0.0;
        }
    }

    /**
     * Get current isolation state (for testing/monitoring)
     */
    public IsolationStateFile getCurrentState() {
        try {
            return stateManager.loadState();
        } catch (Exception e) {
            logger.error("Failed to load current state", e);
            return null;
        }
    }

    /**
     * Manually reset isolation state (for testing/user request)
     */
    public void manualReset(String reason) throws Exception {
        logger.info("Manual reset requested: {}", reason);

        IsolationStateFile state = stateManager.loadState();
        state = stateReset.executeReset(state, "Manual reset: " + reason);
        stateManager.saveState(state);
    }
}
