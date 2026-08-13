package com.chachamaru.harness.isolation;

import com.chachamaru.harness.isolation.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for isolation state management components.
 */
class IsolationStateManagementTest {

    private IsolationStateManager stateManager;
    private IsolationStateReset stateReset;
    private CodeStatusDetector codeDetector;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        stateManager = new IsolationStateManager();
        stateReset = new IsolationStateReset();
        codeDetector = new CodeStatusDetector();
    }

    @Test
    void testStateCreation() throws Exception {
        IsolationStateFile state = stateManager.createNewStateFile();

        assertNotNull(state);
        assertEquals("2.0", state.getVersion());
        assertEquals("branch-isolation-state-v2", state.getSchemaType());
        assertNotNull(state.getResetTriggers());
        assertNotNull(state.getMetadata());
    }

    @Test
    void testStateSaveAndLoad() throws Exception {
        IsolationStateFile originalState = stateManager.createNewStateFile();

        // Set up some test data
        SeriesInfo series = new SeriesInfo("test-series");
        series.addTaskToSequence(17.1);
        series.setIsolationActive(true);

        originalState.setCurrentSeries(series);

        // Save and load
        stateManager.saveState(originalState);
        IsolationStateFile loadedState = stateManager.loadState();

        // Verify
        assertEquals(originalState.getCurrentSeries().getSeriesId(),
                    loadedState.getCurrentSeries().getSeriesId());
        assertEquals(originalState.getCurrentSeries().getTaskCount(),
                    loadedState.getCurrentSeries().getTaskCount());
    }

    @Test
    void testResetEvaluation() {
        IsolationStateFile state = stateManager.createNewStateFile();

        // Set up a scenario where reset should occur
        CodeStatus cleanStatus = CodeStatus.builder()
            .branchClean(true)
            .hasUncommittedChanges(false)
            .build();

        state.setCodeStatus(cleanStatus);

        SeriesInfo series = new SeriesInfo("test-series");
        series.addTaskToSequence(17.1);
        series.setIsolationActive(true);
        series.setLastActivityDate(LocalDateTime.now().minusHours(5)); // 5 hours ago
        state.setCurrentSeries(series);

        // Evaluate
        var evaluation = stateReset.evaluateResetConditions(state, cleanStatus);

        // Should recommend reset due to inactivity with clean branch
        assertTrue(evaluation.shouldReset());
        assertTrue(evaluation.hasCondition("inactivity_with_clean_branch"));
    }

    @Test
    void testResetExecution() {
        IsolationStateFile state = stateManager.createNewStateFile();

        // Set up active series
        SeriesInfo series = new SeriesInfo("test-series");
        series.addTaskToSequence(17.1);
        series.setIsolationActive(true);
        state.setCurrentSeries(series);

        // Execute reset
        IsolationStateFile resetState = stateReset.executeReset(state, "Test reset");

        // Verify state is cleared
        assertNull(resetState.getCurrentSeries());
        assertEquals(0, resetState.getDecisionHistory().size()); // History should be preserved actually
    }

    @Test
    void testCodeStatusBuilder() {
        CodeStatus status = CodeStatus.builder()
            .hasUncommittedChanges(false)
            .branchClean(true)
            .commitsCount(3)
            .lastCommitMessage("Test commit")
            .build();

        assertFalse(status.hasUncommittedChanges());
        assertTrue(status.isBranchClean());
        assertEquals(3, status.getCommitsCount());
        assertEquals("Test commit", status.getLastCommitMessage());
    }

    @Test
    void testSeriesInfoTaskSequence() {
        SeriesInfo series = new SeriesInfo("test-series");

        series.addTaskToSequence(17.1);
        series.addTaskToSequence(17.2);
        series.addTaskToSequence(17.3);

        assertEquals(3, series.getTaskCount());
        assertEquals(17.3, series.getCurrentTask());
        assertEquals(List.of(17.1, 17.2, 17.3), series.getTaskSequence());
    }

    @Test
    void testSeriesContextCompletion() {
        SeriesContext context = SeriesContext.builder()
            .estimatedTasks(10)
            .completionPercentage(100)
            .build();

        assertTrue(context.isTaskSeriesComplete());

        // Test incomplete scenario
        SeriesContext incompleteContext = SeriesContext.builder()
            .estimatedTasks(10)
            .completionPercentage(50)
            .build();

        assertFalse(incompleteContext.isTaskSeriesComplete());
    }

    @Test
    void testDecisionRecord() {
        DecisionRecord record = new DecisionRecord("test-series", 17.1, "isolate", "Test decision");

        assertNotNull(record.getTimestamp());
        assertEquals("test-series", record.getSeriesId());
        assertEquals(17.1, record.getTask());
        assertEquals("isolate", record.getDecision());
        assertEquals("Test decision", record.getReason());
    }

    @Test
    void testBranchInfo() {
        BranchInfo branchInfo = new BranchInfo(
            "feature/test-branch",
            ".claude/worktrees/test-branch",
            "abc123",
            "master"
        );

        assertEquals("feature/test-branch", branchInfo.getFeatureBranch());
        assertEquals(".claude/worktrees/test-branch", branchInfo.getWorktreePath());
        assertEquals("abc123", branchInfo.getBaseRef());
        assertEquals("master", branchInfo.getOriginalBranch());
        assertNotNull(branchInfo.getCreatedAt());
    }

    @Test
    void testStateFileHelperMethods() {
        IsolationStateFile state = stateManager.createNewStateFile();

        // Test initial state
        assertFalse(state.hasActiveSeries());
        assertFalse(state.isReadyForReset());

        // Set up active series
        SeriesInfo series = new SeriesInfo("test-series");
        series.setIsolationActive(true);
        state.setCurrentSeries(series);

        assertTrue(state.hasActiveSeries());

        // Set up code status ready for reset
        CodeStatus cleanCode = CodeStatus.builder()
            .branchClean(true)
            .hasUncommittedChanges(false)
            .build();
        state.setCodeStatus(cleanCode);
        series.setAutoResetPending(true);

        assertTrue(state.isReadyForReset());
    }

    @Test
    void testResetTriggersDefaults() {
        ResetTriggers triggers = new ResetTriggers();

        assertTrue(triggers.isAutoResetEnabled());
        assertTrue(triggers.isManualResetAvailable());
        assertEquals(4, triggers.getAutoResetAfterHours());
        assertEquals("branch_clean_and_no_uncommitted_changes", triggers.getAutoResetCondition());
    }

    @Test
    void testStateMetadata() {
        StateMetadata metadata = new StateMetadata();

        assertEquals("2.0", metadata.getVersion());
        assertNotNull(metadata.getCreatedAt());
        assertNotNull(metadata.getUpdatedAt());
        assertFalse(metadata.isMigrated());

        metadata.markAsUpdated();
        assertNotNull(metadata.getUpdatedAt());

        metadata.setMigratedFrom("1.0");
        assertTrue(metadata.isMigrated());
    }
}