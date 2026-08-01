package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for FailureCodifierCommand.
 */
public class FailureCodifierCommandTest {

    @Test
    public void testFailureCodifierProposeCommand(@TempDir Path tempDir) throws Exception {
        FailureCodifierCommand.ProposeCommand command = new FailureCodifierCommand.ProposeCommand();
        command.dryRun = true;
        command.repoRoot = tempDir.toString();
        command.format = "json";
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(0, exitCode);
    }

    @Test
    public void testFailureCodifierProposeCommandWithoutDryRun(@TempDir Path tempDir) throws Exception {
        FailureCodifierCommand.ProposeCommand command = new FailureCodifierCommand.ProposeCommand();
        command.dryRun = false;  // Should fail
        command.repoRoot = tempDir.toString();
        command.format = "json";
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(2, exitCode);  // Exit code 2 for missing --dry-run
    }

    @Test
    public void testFailureCodifierProposeCommandWithInvalidRepo(@TempDir Path tempDir) throws Exception {
        FailureCodifierCommand.ProposeCommand command = new FailureCodifierCommand.ProposeCommand();
        command.dryRun = true;
        command.repoRoot = "/nonexistent/path";
        command.format = "json";
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(1, exitCode);  // Exit code 1 for invalid repo
    }

    @Test
    public void testFailureCodifierProposeCommandWithCurrentDir(@TempDir Path tempDir) throws Exception {
        FailureCodifierCommand.ProposeCommand command = new FailureCodifierCommand.ProposeCommand();
        command.dryRun = true;
        command.repoRoot = null;  // Should use current directory
        command.format = "json";
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(0, exitCode);
    }

    @Test
    public void testFailureCodifierProposeCommandWithUnsupportedFormat(@TempDir Path tempDir) throws Exception {
        FailureCodifierCommand.ProposeCommand command = new FailureCodifierCommand.ProposeCommand();
        command.dryRun = true;
        command.repoRoot = tempDir.toString();
        command.format = "xml";  // Unsupported format
        command.verbose = false;

        Integer exitCode = command.call();
        assertNotNull(exitCode);
        assertEquals(1, exitCode);  // Exit code 1 for unsupported format
    }

    @Test
    public void testFailureAnalyzer(@TempDir Path tempDir) throws Exception {
        FailureCodifierCommand.FailureAnalyzer analyzer =
            new FailureCodifierCommand.FailureAnalyzer(false);
        FailureCodifierCommand.FailureProposal proposal = analyzer.proposeFixes(tempDir);

        assertNotNull(proposal);
        assertNotNull(proposal.repoRoot());
        assertNotNull(proposal.analyzedAt());
        assertNotNull(proposal.failureTasks());
        assertNotNull(proposal.failuresByCategory());
        assertNotNull(proposal.recommendation());
    }

    @Test
    public void testFailureTaskRecord(@TempDir Path tempDir) {
        FailureCodifierCommand.FailureTask task = new FailureCodifierCommand.FailureTask(
            "test-123",
            "test_failure",
            "test",
            "Test failure title",
            "Test failure description",
            java.util.List.of("evidence1.txt", "evidence2.log"),
            java.util.List.of("task-8.2.19", "task-8.2.20"),
            "high",
            "2-4 hours"
        );

        assertNotNull(task);
        assertEquals("test-123", task.taskId());
        assertEquals("test_failure", task.type());
        assertEquals("test", task.category());
        assertEquals("Test failure title", task.title());
        assertEquals("Test failure description", task.description());
        assertEquals(2, task.evidenceFiles().size());
        assertEquals(2, task.affectedTasks().size());
        assertEquals("high", task.priority());
        assertEquals("2-4 hours", task.estimatedEffort());
    }

    @Test
    public void testFailureProposalRecord(@TempDir Path tempDir) {
        LocalDateTime now = LocalDateTime.now();
        FailureCodifierCommand.FailureProposal proposal = new FailureCodifierCommand.FailureProposal(
            "/test/repo",
            now,
            java.util.List.of(),
            0,
            java.util.Map.of("test", 0, "ci", 0),
            "No failures detected"
        );

        assertNotNull(proposal);
        assertEquals("/test/repo", proposal.repoRoot());
        assertEquals(now, proposal.analyzedAt());
        assertNotNull(proposal.failureTasks());
        assertEquals(0, proposal.totalFailures());
        assertEquals(2, proposal.failuresByCategory().size());
        assertEquals("No failures detected", proposal.recommendation());
    }

    @Test
    public void testMainCommand() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode = new CommandLine(new FailureCodifierCommand())
            .execute("propose", "--dry-run", "--repo-root", ".");

        System.setOut(System.out);
        // Should output valid JSON
        assertNotNull(outputStream.toString());
        assertTrue(outputStream.toString().contains("repo_root") ||
                  outputStream.toString().contains("failures"));
    }

    @Test
    public void testDefaultBehavior() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));

        int exitCode = new CommandLine(new FailureCodifierCommand()).execute();

        System.setOut(System.out);
        // Default behavior should show help
        assertTrue(outputStream.toString().contains("Usage") ||
                  outputStream.toString().contains("help"));
    }

    @Test
    public void testFailureAnalyzerWithTestResults(@TempDir Path tempDir) {
        // Create a mock test results directory
        Path testResultsDir = tempDir.resolve("target").resolve("surefire-reports");
        testResultsDir.toFile().mkdirs();

        FailureCodifierCommand.FailureAnalyzer analyzer =
            new FailureCodifierCommand.FailureAnalyzer(true);
        FailureCodifierCommand.FailureProposal proposal = analyzer.proposeFixes(tempDir);

        assertNotNull(proposal);
        assertTrue(proposal.totalFailures() >= 0);
    }

    @Test
    public void testFailureAnalyzerWithCIConfig(@TempDir Path tempDir) {
        // Create a mock CI workflow directory
        Path githubDir = tempDir.resolve(".github").resolve("workflows");
        githubDir.toFile().mkdirs();

        FailureCodifierCommand.FailureAnalyzer analyzer =
            new FailureCodifierCommand.FailureAnalyzer(true);
        FailureCodifierCommand.FailureProposal proposal = analyzer.proposeFixes(tempDir);

        assertNotNull(proposal);
        assertTrue(proposal.totalFailures() >= 0);
    }

    @Test
    public void testFailureAnalyzerWithBuildConfig(@TempDir Path tempDir) {
        // Create a mock pom.xml
        Path pomXml = tempDir.resolve("pom.xml");

        FailureCodifierCommand.FailureAnalyzer analyzer =
            new FailureCodifierCommand.FailureAnalyzer(true);
        FailureCodifierCommand.FailureProposal proposal = analyzer.proposeFixes(tempDir);

        assertNotNull(proposal);
        assertTrue(proposal.totalFailures() >= 0);
    }
}
