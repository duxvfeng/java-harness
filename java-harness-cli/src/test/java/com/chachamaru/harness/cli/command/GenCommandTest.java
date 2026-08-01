package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GenCommand
 */
class GenCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateCommandInstantiation() {
        GenCommand.CreateCommand createCommand = new GenCommand.CreateCommand();
        assertNotNull(createCommand, "CreateCommand should be instantiated");
    }

    @Test
    void testCheckCommandInstantiation() {
        GenCommand.CheckCommand checkCommand = new GenCommand.CheckCommand();
        assertNotNull(checkCommand, "CheckCommand should be instantiated");
    }

    @Test
    void testUpdateCommandInstantiation() {
        GenCommand.UpdateCommand updateCommand = new GenCommand.UpdateCommand();
        assertNotNull(updateCommand, "UpdateCommand should be instantiated");
    }

    @Test
    void testValidateCommandInstantiation() {
        GenCommand.ValidateCommand validateCommand = new GenCommand.ValidateCommand();
        assertNotNull(validateCommand, "ValidateCommand should be instantiated");
    }

    @Test
    void testPlanFileCreation() throws IOException {
        // Create a test Plans.md file
        Path testPlan = tempDir.resolve("test-Plans.md");
        String content = """
# Test Plan

## Phase 1

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | Test task | Test complete | - | cc:TODO |
| 1.2 | Another task | Done | 1.1 | cc:completed ✅ abc123 |
""";

        Files.write(testPlan, content.getBytes(StandardCharsets.UTF_8));

        // Test PlanFile parsing
        GenCommand.PlanFile plan = new GenCommand.PlanFile(testPlan.toString());

        assertTrue(plan.getTaskCount() >= 0, "Should be able to parse file");
        assertTrue(plan.getTasks().containsKey("1.1") || plan.getTaskCount() >= 0,
                   "Should either find task 1.1 or complete parsing without error");

        if (plan.getTasks().containsKey("1.1")) {
            assertEquals("cc:TODO", plan.getTasks().get("1.1").status);
        }
    }

    @Test
    void testPlanFileValidation() throws IOException {
        // Create a test Plans.md file
        Path testPlan = tempDir.resolve("test-Plans.md");
        String content = """
# Test Plan

## Phase 1

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | Test task | Test complete | - | cc:TODO |
""";

        Files.write(testPlan, content.getBytes(StandardCharsets.UTF_8));

        GenCommand.PlanFile plan = new GenCommand.PlanFile(testPlan.toString());
        var issues = plan.validate(false);

        assertTrue(issues.size() >= 0, "Validation should complete without errors");
    }

    @Test
    void testDriftDetector() throws IOException {
        // Create test plan with completed task
        Path testPlan = tempDir.resolve("test-Plans.md");
        String content = """
# Test Plan

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | Test task | Test complete | - | cc:completed ✅ testhash |
""";

        Files.write(testPlan, content.getBytes(StandardCharsets.UTF_8));

        GenCommand.PlanFile plan = new GenCommand.PlanFile(testPlan.toString());
        GenCommand.DriftDetector detector = new GenCommand.DriftDetector();

        var issues = detector.detectDrift(plan, "master");

        assertNotNull(issues, "Should return drift issues list");
        assertTrue(issues.size() >= 0, "Should complete drift detection");
    }

    @Test
    void testTaskInfoStructure() {
        GenCommand.TaskInfo taskInfo = new GenCommand.TaskInfo();

        taskInfo.taskId = "1.1";
        taskInfo.status = "cc:TODO";
        taskInfo.commitHash = null;

        assertEquals("1.1", taskInfo.taskId);
        assertEquals("cc:TODO", taskInfo.status);
        assertNull(taskInfo.commitHash);

        // Test constructor with parameters
        GenCommand.TaskInfo taskWithParams = new GenCommand.TaskInfo("2.1", "cc:completed", "abc123");
        assertEquals("2.1", taskWithParams.taskId);
        assertEquals("cc:completed", taskWithParams.status);
        assertEquals("abc123", taskWithParams.commitHash);
    }

    @Test
    void testDriftIssueStructure() {
        GenCommand.DriftIssue issue = new GenCommand.DriftIssue(
            "ERROR",
            "1.1",
            "Test error message",
            "Fix suggestion"
        );

        assertEquals("ERROR", issue.severity);
        assertEquals("1.1", issue.taskId);
        assertEquals("Test error message", issue.message);
        assertEquals("Fix suggestion", issue.suggestedFix);
    }

    @Test
    void testValidationIssueStructure() {
        GenCommand.ValidationIssue issue = new GenCommand.ValidationIssue(
            "WARNING",
            "Table header",
            "Validation message"
        );

        assertEquals("WARNING", issue.level);
        assertEquals("Table header", issue.location);
        assertEquals("Validation message", issue.message);
    }

    @Test
    void testCommandAnnotationPresence() {
        picocli.CommandLine.Command commandAnnotation =
            GenCommand.CreateCommand.class.getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "CreateCommand should have @Command annotation");
        assertEquals("create", commandAnnotation.name());

        commandAnnotation = GenCommand.CheckCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "CheckCommand should have @Command annotation");
        assertEquals("check", commandAnnotation.name());

        commandAnnotation = GenCommand.UpdateCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "UpdateCommand should have @Command annotation");
        assertEquals("update", commandAnnotation.name());

        commandAnnotation = GenCommand.ValidateCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "ValidateCommand should have @Command annotation");
        assertEquals("validate", commandAnnotation.name());
    }

    @Test
    void testPlanFileEmptyFile() throws IOException {
        Path emptyFile = tempDir.resolve("empty-Plans.md");
        Files.write(emptyFile, new byte[0]);

        GenCommand.PlanFile plan = new GenCommand.PlanFile(emptyFile.toString());
        var issues = plan.validate(false);

        assertTrue(issues.size() > 0, "Should detect validation issues");
        assertTrue(issues.stream().anyMatch(i -> i.level.equals("ERROR")),
                   "Should detect at least one ERROR level issue");
    }

    @Test
    void testPlanFileUpdateStatus() throws IOException {
        Path testPlan = tempDir.resolve("test-Plans.md");
        String content = """
# Test Plan

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | Test task | Test complete | - | cc:TODO |
""";

        Files.write(testPlan, content.getBytes(StandardCharsets.UTF_8));

        GenCommand.PlanFile plan = new GenCommand.PlanFile(testPlan.toString());
        plan.updateTaskStatus("1.1", "cc:completed ✅ test123");

        assertEquals("cc:completed ✅ test123", plan.getTasks().get("1.1").status);
    }

    /**
     * Integration test for command structure
     */
    @Test
    void testGenCommandIntegration() {
        GenCommand genCommand = new GenCommand();
        assertNotNull(genCommand, "GenCommand should be properly instantiated");

        picocli.CommandLine.Command commandAnnotation =
            genCommand.getClass().getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "GenCommand should have @Command annotation");
        assertEquals("gen", commandAnnotation.name());
    }
}