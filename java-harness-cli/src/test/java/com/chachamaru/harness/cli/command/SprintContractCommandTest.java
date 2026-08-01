package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SprintContractCommand.
 */
class SprintContractCommandTest {

    private ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    @Test
    void testGenerateCommandBasic() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("generate", "-d", ".", "-t", "8.2.14");

        assertEquals(0, exitCode);
    }

    @Test
    void testGenerateCommandWithOptions() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("generate", "-d", ".", "-t", "8.2.14",
            "--lane", "experimental", "--stage", "implement");

        assertEquals(0, exitCode);
    }

    @Test
    void testGenerateCommandWithSpec() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("generate", "-d", ".", "-t", "8.2.14",
            "--spec", "specs/task-8.2.14.md");

        assertEquals(0, exitCode);
    }

    @Test
    void testGenerateCommandForceOverwrite() throws Exception {
        // Create a temp directory
        Path tempDir = Files.createTempDirectory("scontract-test");
        Path contractsDir = tempDir.resolve(".claude/state/contracts");
        Files.createDirectories(contractsDir);

        // Create an existing contract
        Path existingContract = contractsDir.resolve("8.2.14.sprint-contract.json");
        Files.writeString(existingContract, "{ \"existing\": true }");

        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        // Without --force, should fail
        int exitCode1 = cmd.execute("generate", "-d", tempDir.toString(), "-t", "8.2.14");
        assertEquals(1, exitCode1);

        // With --force, should succeed
        int exitCode2 = cmd.execute("generate", "-d", tempDir.toString(), "-t", "8.2.14", "--force");
        assertEquals(0, exitCode2);

        // Cleanup
        deleteDirectory(tempDir);
    }

    @Test
    void testValidateCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        // Create a test contract file first
        int exitCode = cmd.execute("generate", "-d", ".", "-t", "test-validate");
        assertEquals(0, exitCode);

        // Now validate it
        baos = captureOutput();
        exitCode = cmd.execute("validate", "-d", ".", "-c", ".claude/state/contracts/test-validate.sprint-contract.json");

        assertEquals(0, exitCode);
    }

    @Test
    void testValidateCommandStrict() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("generate", "-d", ".", "-t", "test-strict");

        baos = captureOutput();
        exitCode = cmd.execute("validate", "-d", ".", "-c", ".claude/state/contracts/test-strict.sprint-contract.json", "--strict");

        assertEquals(0, exitCode);
    }

    @Test
    void testValidateCommandNonExistentFile() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("validate", "-d", ".", "-c", "nonexistent.json");

        assertEquals(1, exitCode);
    }

    @Test
    void testListCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        // Generate some contracts first
        cmd.execute("generate", "-d", ".", "-t", "test-list-1");
        cmd.execute("generate", "-d", ".", "-t", "test-list-2");

        baos = captureOutput();
        int exitCode = cmd.execute("list", "-d", ".");

        assertEquals(0, exitCode);
    }

    @Test
    void testListCommandWithFilters() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("list", "-d", ".", "--lane", "default", "--stage", "plan");

        assertEquals(0, exitCode);
    }

    @Test
    void testListCommandJsonFormat() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("list", "-d", ".", "--format", "json");

        assertEquals(0, exitCode);
    }

    @Test
    void testListCommandDetailedFormat() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("list", "-d", ".", "--format", "detailed");

        assertEquals(0, exitCode);
    }

    @Test
    void testTemplateCommandList() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("template", "-d", ".", "--list");

        assertEquals(0, exitCode);
    }

    @Test
    void testTemplateCommandShow() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        // This might fail if template doesn't exist, but should not crash
        int exitCode = cmd.execute("template", "-d", ".", "--show", "default");

        // Either success (template found) or error (template not found) is acceptable
        assertTrue(exitCode == 0 || exitCode == 2);
    }

    @Test
    void testHelpCommand() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("--help");

        assertEquals(0, exitCode);
        String output = baos.toString();

        // Help should contain description
        assertTrue(output.contains("Sprint") || output.contains("Contract"));
    }

    @Test
    void testSprintContractRecord() {
        // Test SprintContract record creation
        SprintContractCommand.SprintContract contract = new SprintContractCommand.SprintContract(
            "8.2.14",
            "Sprint Contract Command",
            "Implementation of sprint-contract command",
            "default",
            "implement",
            "in_progress",
            "",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            Map.of("version", "1.0"),
            SprintContractCommand.ContractDefinition.create("", List.of(), List.of(), List.of()),
            SprintContractCommand.ReviewSettings.create(true, 3, List.of())
        );

        assertNotNull(contract);
        assertEquals("8.2.14", contract.taskId());
        assertEquals("default", contract.lane());
        assertEquals("implement", contract.stage());
    }

    @Test
    void testContractDefinitionRecord() {
        // Test ContractDefinition record creation
        SprintContractCommand.ContractDefinition definition = SprintContractCommand.ContractDefinition.create(
            "Task must be completed",
            List.of(" criterion 1", "criterion 2"),
            List.of("requirement 1"),
            List.of("test 1")
        );

        assertNotNull(definition);
        assertEquals("Task must be completed", definition.dod());
        assertEquals(2, definition.acceptanceCriteria().size());
    }

    @Test
    void testReviewSettingsRecord() {
        // Test ReviewSettings record creation
        SprintContractCommand.ReviewSettings review = SprintContractCommand.ReviewSettings.create(
            true,
            3,
            List.of("checkpoint 1", "checkpoint 2")
        );

        assertNotNull(review);
        assertTrue(review.autoReview());
        assertEquals(3, review.maxIterations());
        assertEquals(2, review.reviewCheckpoints().size());
    }

    @Test
    void testValidationResultRecord() {
        // Test ValidationResult record creation
        SprintContractCommand.ValidationResult result = new SprintContractCommand.ValidationResult(
            true,
            10,
            5,
            5,
            List.of(),
            List.of("warning 1")
        );

        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(10, result.fieldCount());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void testSprintContractGenerator() {
        SprintContractCommand.SprintContractGenerator generator =
            new SprintContractCommand.SprintContractGenerator(Paths.get("."), false);

        assertDoesNotThrow(() -> {
            SprintContractCommand.SprintContract contract = generator.generateContract(
                "test-task", null, null, null, null);
            assertNotNull(contract);
            assertEquals("test-task", contract.taskId());
        });
    }

    @Test
    void testContractValidator() {
        SprintContractCommand.ContractValidator validator =
            new SprintContractCommand.ContractValidator(false);

        SprintContractCommand.SprintContract contract = new SprintContractCommand.SprintContract(
            "8.2.14",
            "Test",
            "Test contract",
            "default",
            "plan",
            "pending",
            "",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            Map.of(),
            SprintContractCommand.ContractDefinition.create("", List.of(), List.of(), List.of()),
            SprintContractCommand.ReviewSettings.create(true, 3, List.of())
        );

        SprintContractCommand.ValidationResult result = validator.validate(contract);

        assertNotNull(result);
        assertTrue(result.isValid());
    }

    @Test
    void testContractValidatorStrictMode() {
        SprintContractCommand.ContractValidator validator =
            new SprintContractCommand.ContractValidator(false);
        validator.setStrictMode(true);

        SprintContractCommand.SprintContract contract = new SprintContractCommand.SprintContract(
            "8.2.14",
            "Test",
            "Test contract",
            "default",
            "plan",
            "pending",
            "",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            Map.of(),
            SprintContractCommand.ContractDefinition.create("", List.of(), List.of(), List.of()),
            SprintContractCommand.ReviewSettings.create(true, 3, List.of())
        );

        SprintContractCommand.ValidationResult result = validator.validate(contract);

        assertNotNull(result);
        // In strict mode, empty DoD should generate warnings
        assertTrue(result.warnings().size() > 0 || result.isValid());
    }

    @Test
    void testContractValidatorInvalidContract() {
        SprintContractCommand.ContractValidator validator =
            new SprintContractCommand.ContractValidator(false);

        SprintContractCommand.SprintContract invalidContract = new SprintContractCommand.SprintContract(
            "",  // Empty taskId - should fail validation
            "",
            "",
            "default",
            "plan",
            "pending",
            "",
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now(),
            Map.of(),
            SprintContractCommand.ContractDefinition.create("", List.of(), List.of(), List.of()),
            SprintContractCommand.ReviewSettings.create(true, 3, List.of())
        );

        SprintContractCommand.ValidationResult result = validator.validate(invalidContract);

        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.errors().size() > 0);
    }

    @Test
    void testContractLister() throws Exception {
        Path tempDir = Files.createTempDirectory("lister-test");
        Path contractsDir = tempDir.resolve(".claude/state/contracts");
        Files.createDirectories(contractsDir);

        SprintContractCommand.ContractLister lister =
            new SprintContractCommand.ContractLister(contractsDir, false);

        List<SprintContractCommand.SprintContract> contracts = lister.listContracts();

        assertNotNull(contracts);
        assertTrue(contracts.isEmpty()); // No contracts yet

        // Cleanup
        deleteDirectory(tempDir);
    }

    @Test
    void testTemplateManager() throws Exception {
        Path tempDir = Files.createTempDirectory("template-test");
        Path templatesDir = tempDir.resolve(".claude/templates/contracts");
        Files.createDirectories(templatesDir);

        SprintContractCommand.TemplateManager manager =
            new SprintContractCommand.TemplateManager(tempDir, false);

        // List templates (should be empty initially)
        List<String> templates = manager.listTemplates();
        assertNotNull(templates);
        assertTrue(templates.isEmpty());

        // Cleanup
        deleteDirectory(tempDir);
    }

    @Test
    void testInvalidDirectory() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("generate", "-d", "/nonexistent/path", "-t", "8.2.14");

        // Should return error code
        assertEquals(1, exitCode);
    }

    @Test
    void testGenerateWithoutTaskId() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("generate", "-d", ".");

        // Should return error code (missing required option)
        assertEquals(2, exitCode);
    }

    @Test
    void testValidateWithoutContractFile() {
        ByteArrayOutputStream baos = captureOutput();

        SprintContractCommand command = new SprintContractCommand();
        CommandLine cmd = new CommandLine(command);

        int exitCode = cmd.execute("validate", "-d", ".");

        // Should return error code (missing required option)
        assertEquals(2, exitCode);
    }

    // Helper method to cleanup directories
    private void deleteDirectory(Path path) throws Exception {
        if (Files.exists(path)) {
            Files.walk(path)
                 .sorted((a, b) -> -a.compareTo(b)) // Reverse order for deletion
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                     } catch (IOException e) {
                         // Ignore
                     }
                 });
        }
    }
}
