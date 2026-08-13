package com.chachamaru.harness.isolation;

import com.chachamaru.harness.isolation.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
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
        stateManager = new IsolationStateManager(tempDir);
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

        originalState.setCodeStatus(CodeStatus.builder()
            .branchClean(true)
            .hasUncommittedChanges(false)
            .filesChanged(List.of("src/Main.java"))
            .commitsCount(2)
            .lastCommitMessage("test commit")
            .untrackedFilesCount(0)
            .build());
        originalState.addDecisionRecord(new DecisionRecord(
            "test-series", 17.1, "isolate", "test decision"));

        // Save and load
        stateManager.saveState(originalState);
        String json = Files.readString(Path.of(stateManager.getStateFilePath()));
        assertFalse(json.contains("readyForReset"));
        assertFalse(json.contains("taskSeriesComplete"));
        assertFalse(json.contains("migrated"));
        IsolationStateFile loadedState = stateManager.loadState();

        // Verify
        assertEquals(originalState.getCurrentSeries().getSeriesId(),
                    loadedState.getCurrentSeries().getSeriesId());
        assertEquals(originalState.getCurrentSeries().getTaskCount(),
                    loadedState.getCurrentSeries().getTaskCount());
        assertEquals(originalState.getCodeStatus(), loadedState.getCodeStatus());
        assertEquals(1, loadedState.getDecisionHistory().size());
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
        assertEquals(1, resetState.getDecisionHistory().size());
        assertEquals("reset", resetState.getDecisionHistory().get(0).getDecision());
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

    @Test
    void testCodeStatusDetectorSupportsGitWorktreeAndAllChangeTypes() throws Exception {
        Path repository = tempDir.resolve("repository");
        Path worktree = tempDir.resolve("worktree");
        runGit(repository.getParent(), "init", "-b", "main", repository.getFileName().toString());
        runGit(repository, "config", "user.email", "test@example.com");
        runGit(repository, "config", "user.name", "Test User");
        Files.writeString(repository.resolve("tracked.txt"), "one\n");
        Files.writeString(repository.resolve("deleted.txt"), "delete me\n");
        runGit(repository, "add", ".");
        runGit(repository, "commit", "-m", "initial");
        Files.writeString(repository.resolve("tracked.txt"), "two\n");
        runGit(repository, "add", "tracked.txt");
        runGit(repository, "commit", "-m", "second");
        runGit(repository, "worktree", "add", "-b", "feature/test", worktree.toString(), "HEAD");

        Files.writeString(worktree.resolve("tracked.txt"), "changed\n");
        Files.delete(worktree.resolve("deleted.txt"));
        Files.writeString(worktree.resolve("new.txt"), "new\n");

        CodeStatus status = codeDetector.detectCodeStatus(worktree.toString());

        assertFalse(status.hasError());
        assertTrue(status.hasUncommittedChanges());
        assertFalse(status.isBranchClean());
        assertEquals(1, status.getUntrackedFilesCount());
        assertTrue(status.getFilesChanged().contains("tracked.txt"));
        assertTrue(status.getFilesChanged().contains("deleted.txt"));
        assertTrue(status.getFilesChanged().contains("new.txt"));
        assertEquals("feature/test", codeDetector.getCurrentBranch(worktree.toString()));
        assertEquals(runGit(repository, "rev-parse", "HEAD^"),
            codeDetector.getBaseReference(worktree.toString()));
        assertTrue(codeDetector.isValidGitWorktree(worktree.toString()));
    }

    @Test
    void testLegacyDecisionStateIsMigratedToV2() throws Exception {
        Path legacyFile = tempDir.resolve(".claude/state/branch-isolation-decision.json");
        Files.createDirectories(legacyFile.getParent());
        Files.writeString(legacyFile, """
            {
              "decisions": [{
                "timestamp": "2026-08-13T10:00:00",
                "strategy": "force",
                "userResponse": "auto-isolate",
                "reason": "legacy decision"
              }],
              "currentStrategy": "force",
              "lastUpdated": "2026-08-13T10:00:00"
            }
            """);

        IsolationStateFile migrated = stateManager.loadState();

        assertEquals("2.0", migrated.getVersion());
        assertTrue(migrated.getMetadata().isMigrated());
        assertEquals(1, migrated.getDecisionHistory().size());
        assertEquals("force", migrated.getDecisionHistory().get(0).getDecision());
        assertEquals("auto-isolate", migrated.getDecisionHistory().get(0).getUserChoice());
        assertTrue(Files.readString(legacyFile).contains("\"schemaType\" : \"branch-isolation-state-v2\""));
    }

    private static String runGit(Path directory, String... args) throws IOException, InterruptedException {
        List<String> command = new java.util.ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
        return output.trim();
    }
}
