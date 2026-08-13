package com.chachamaru.harness.isolation.integration;

import com.chachamaru.harness.isolation.IsolationStateManager;
import com.chachamaru.harness.isolation.IsolationStateReset;
import com.chachamaru.harness.isolation.integration.HarnessWorkIsolationIntegration;
import com.chachamaru.harness.isolation.model.IsolationStateFile;
import com.chachamaru.harness.isolation.model.CodeStatus;
import com.chachamaru.harness.isolation.ui.IsolationDecision;
import com.chachamaru.harness.isolation.ui.IsolationDecisionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for complete branch isolation state lifecycle.
 */
class BranchIsolationIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testCompleteLifecycle_TaskSeries() throws Exception {
        Path repository = createGitRepository();
        RecordingUI ui = new RecordingUI();
        IsolationStateManager manager = new IsolationStateManager(tempDir.resolve("state"));
        HarnessWorkIsolationIntegration integration = new HarnessWorkIsolationIntegration(
            manager, new com.chachamaru.harness.isolation.CodeStatusDetector(),
            new IsolationStateReset(), ui);

        ui.decisions.add(new IsolationDecision(IsolationDecisionType.ISOLATE, "isolate", "first task"));
        IsolationDecision first = integration.handlePhaseABranchIsolation("17.1", "first", repository.toString());
        assertEquals(IsolationDecisionType.ISOLATE, first.getDecisionType());
        assertTrue(manager.loadState().hasActiveSeries());

        IsolationDecision second = integration.handlePhaseABranchIsolation("17.2", "second", repository.toString());
        assertEquals(IsolationDecisionType.CONTINUE, second.getDecisionType());
        assertEquals(1, ui.interactionCount);
        assertEquals(List.of(17.1, 17.2), manager.loadState().getCurrentSeries().getTaskSequence());

        IsolationStateFile completed = manager.loadState();
        completed.getCurrentSeries().getSeriesContext().setCompletionPercentage(100);
        manager.saveState(completed);
        ui.decisions.add(new IsolationDecision(IsolationDecisionType.ISOLATE, "isolate", "new series"));

        IsolationDecision third = integration.handlePhaseABranchIsolation("18.1", "new phase", repository.toString());
        IsolationStateFile finalState = manager.loadState();
        assertEquals(IsolationDecisionType.ISOLATE, third.getDecisionType());
        assertEquals(2, ui.interactionCount);
        assertEquals("phase-18", finalState.getCurrentSeries().getSeriesContext().getPhaseName());
        assertTrue(finalState.getDecisionHistory().stream()
            .anyMatch(record -> "reset".equals(record.getDecision())));
    }

    @Test
    void testIntegration_Components() {
        // Test that integration components work together
        HarnessWorkIsolationIntegration integration = new HarnessWorkIsolationIntegration(
            new IsolationStateManager(tempDir.resolve("components")),
            new com.chachamaru.harness.isolation.CodeStatusDetector(),
            new IsolationStateReset(), new RecordingUI());

        // Test that components are properly initialized
        assertNotNull(integration);
    }

    @Test
    void testStateResetLifecycle() {
        // Test state reset lifecycle: Initial → Isolated → Active Use → Ready for Reset → Reset → Initial

        IsolationStateManager manager = new IsolationStateManager(tempDir.resolve("reset"));
        IsolationStateReset resetLogic = new IsolationStateReset();

        try {
            // 1. Initial state
            IsolationStateFile state = manager.createNewStateFile();
            assertFalse(state.hasActiveSeries());

            // 2. Setup isolation (simulate)
            // In real scenario, this would be done through integration point
            state.setCurrentSeries(createTestSeries());
            assertTrue(state.hasActiveSeries());

            // 3. Ready for reset conditions
            CodeStatus cleanStatus = CodeStatus.builder()
                .branchClean(true)
                .hasUncommittedChanges(false)
                .build();
            state.setCodeStatus(cleanStatus);

            // 4. Evaluate reset conditions
            var evaluation = resetLogic.evaluateResetConditions(state, cleanStatus);

            // 5. Execute reset if appropriate
            if (evaluation.shouldReset()) {
                state = resetLogic.executeReset(state, "Integration test reset");
                assertFalse(state.hasActiveSeries());
            }

        } catch (Exception e) {
            fail("Integration test failed: " + e.getMessage());
        }
    }

    // Helper method to create test series
    private com.chachamaru.harness.isolation.model.SeriesInfo createTestSeries() {
        com.chachamaru.harness.isolation.model.SeriesInfo series =
            new com.chachamaru.harness.isolation.model.SeriesInfo("test-series");

        series.addTaskToSequence(17.1);
        series.addTaskToSequence(17.2);
        series.setIsolationActive(true);

        return series;
    }

    private Path createGitRepository() throws Exception {
        Path repository = tempDir.resolve("git-repository");
        Files.createDirectories(repository);
        runGit(repository, "init", "-b", "main");
        runGit(repository, "config", "user.email", "test@example.com");
        runGit(repository, "config", "user.name", "Test User");
        Files.writeString(repository.resolve("README.md"), "test\n");
        runGit(repository, "add", ".");
        runGit(repository, "commit", "-m", "initial");
        return repository;
    }

    private static void runGit(Path directory, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command)
            .directory(directory.toFile())
            .redirectErrorStream(true)
            .start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
    }

    private static class RecordingUI extends com.chachamaru.harness.isolation.ui.EnhancedIsolationUI {
        private final List<IsolationDecision> decisions = new ArrayList<>();
        private int interactionCount;

        @Override
        public IsolationDecision displayIsolationOptions(
                com.chachamaru.harness.isolation.ui.BranchType branchType, IsolationStateFile state) {
            interactionCount++;
            return decisions.isEmpty()
                ? new IsolationDecision(IsolationDecisionType.ISOLATE, "isolate", "default")
                : decisions.remove(0);
        }

        @Override
        public void displayResetRecommendation(String explanation) {
            // The lifecycle assertion verifies the persisted reset record.
        }
    }
}
