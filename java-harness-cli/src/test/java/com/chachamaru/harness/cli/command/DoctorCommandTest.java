package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DoctorCommand
 */
class DoctorCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testCheckCommandInstantiation() {
        DoctorCommand.CheckCommand checkCommand = new DoctorCommand.CheckCommand();
        assertNotNull(checkCommand, "CheckCommand should be instantiated");
    }

    @Test
    void testFixCommandInstantiation() {
        DoctorCommand.FixCommand fixCommand = new DoctorCommand.FixCommand();
        assertNotNull(fixCommand, "FixCommand should be instantiated");
    }

    @Test
    void testReportCommandInstantiation() {
        DoctorCommand.ReportCommand reportCommand = new DoctorCommand.ReportCommand();
        assertNotNull(reportCommand, "ReportCommand should be instantiated");
    }

    @Test
    void testAnalyzeCommandInstantiation() {
        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        assertNotNull(analyzeCommand, "AnalyzeCommand should be instantiated");
    }

    @Test
    void testCheckCommandBasicCall() throws Exception {
        // Create a basic project structure
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        Path settingsFile = claudeDir.resolve("settings.json");
        Files.write(settingsFile, "{\"version\": \"1.0\"}".getBytes(StandardCharsets.UTF_8));

        Path pomFile = tempDir.resolve("pom.xml");
        Files.write(pomFile, "<project></project>".getBytes(StandardCharsets.UTF_8));

        DoctorCommand.CheckCommand checkCommand = new DoctorCommand.CheckCommand();
        checkCommand.projectDir = tempDir.toString();
        checkCommand.quiet = true;

        Integer result = checkCommand.call();

        assertNotNull(result, "Should return exit code");
        assertTrue(result >= 0 && result <= 2, "Exit code should be 0, 1, or 2");
    }

    @Test
    void testCheckCommandWithInvalidDirectory() throws Exception {
        DoctorCommand.CheckCommand checkCommand = new DoctorCommand.CheckCommand();
        checkCommand.projectDir = "/nonexistent/directory";
        checkCommand.quiet = true;

        Integer result = checkCommand.call();

        assertEquals(1, result, "Should return exit code 1 for invalid directory");
    }

    @Test
    void testCheckCommandJsonOutput() throws Exception {
        // Create minimal project structure
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        DoctorCommand.CheckCommand checkCommand = new DoctorCommand.CheckCommand();
        checkCommand.projectDir = tempDir.toString();
        checkCommand.jsonOutput = true;

        Integer result = checkCommand.call();

        // Should complete without exception
        assertNotNull(result, "Should return exit code");
    }

    @Test
    void testFixCommandDryRun() throws Exception {
        // Create a project with some missing directories
        Path pomFile = tempDir.resolve("pom.xml");
        Files.write(pomFile, "<project></project>".getBytes(StandardCharsets.UTF_8));

        DoctorCommand.FixCommand fixCommand = new DoctorCommand.FixCommand();
        fixCommand.projectDir = tempDir.toString();
        fixCommand.dryRun = true;

        Integer result = fixCommand.call();

        assertNotNull(result, "Should return exit code");
    }

    @Test
    void testFixCommandWithAutoFix() throws Exception {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.write(pomFile, "<project></project>".getBytes(StandardCharsets.UTF_8));

        DoctorCommand.FixCommand fixCommand = new DoctorCommand.FixCommand();
        fixCommand.projectDir = tempDir.toString();
        fixCommand.autoFix = true;

        Integer result = fixCommand.call();

        assertNotNull(result, "Should return exit code");
    }

    @Test
    void testReportCommandMarkdownFormat() throws Exception {
        DoctorCommand.ReportCommand reportCommand = new DoctorCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.outputFile = tempDir.resolve("report.md").toString();
        reportCommand.format = "md";

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should generate markdown report successfully");

        Path reportPath = tempDir.resolve("report.md");
        assertTrue(Files.exists(reportPath), "Report file should be created");
    }

    @Test
    void testReportCommandJsonFormat() throws Exception {
        DoctorCommand.ReportCommand reportCommand = new DoctorCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.outputFile = tempDir.resolve("report.json").toString();
        reportCommand.format = "json";

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should generate JSON report successfully");

        Path reportPath = tempDir.resolve("report.json");
        assertTrue(Files.exists(reportPath), "Report file should be created");
    }

    @Test
    void testReportCommandHtmlFormat() throws Exception {
        DoctorCommand.ReportCommand reportCommand = new DoctorCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.outputFile = tempDir.resolve("report.html").toString();
        reportCommand.format = "html";

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should generate HTML report successfully");

        Path reportPath = tempDir.resolve("report.html");
        assertTrue(Files.exists(reportPath), "Report file should be created");

        String content = Files.readString(reportPath);
        assertTrue(content.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(content.contains("Java Harness Health Report"), "Should contain title");
    }

    @Test
    void testAnalyzeCommandConfiguration() throws Exception {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        analyzeCommand.component = "configuration";
        analyzeCommand.projectDir = tempDir.toString();

        Integer result = analyzeCommand.call();

        assertEquals(0, result, "Should analyze configuration successfully");
    }

    @Test
    void testAnalyzeCommandDependencies() throws Exception {
        Path pomFile = tempDir.resolve("pom.xml");
        Files.write(pomFile, "<project></project>".getBytes(StandardCharsets.UTF_8));

        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        analyzeCommand.component = "dependencies";
        analyzeCommand.projectDir = tempDir.toString();

        Integer result = analyzeCommand.call();

        assertEquals(0, result, "Should analyze dependencies successfully");
    }

    @Test
    void testAnalyzeCommandStructure() throws Exception {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        analyzeCommand.component = "structure";
        analyzeCommand.projectDir = tempDir.toString();

        Integer result = analyzeCommand.call();

        assertTrue(result >= 0, "Should analyze structure successfully");
    }

    @Test
    void testAnalyzeCommandSkills() throws Exception {
        Path skillsDir = tempDir.resolve(".claude/skills");
        Files.createDirectories(skillsDir);

        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        analyzeCommand.component = "skills";
        analyzeCommand.projectDir = tempDir.toString();

        Integer result = analyzeCommand.call();

        assertEquals(0, result, "Should analyze skills successfully");
    }

    @Test
    void testAnalyzeCommandWorkflows() throws Exception {
        Path workflowsDir = tempDir.resolve(".claude/workflows");
        Files.createDirectories(workflowsDir);

        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        analyzeCommand.component = "workflows";
        analyzeCommand.projectDir = tempDir.toString();

        Integer result = analyzeCommand.call();

        assertEquals(0, result, "Should analyze workflows successfully");
    }

    @Test
    void testAnalyzeCommandInvalidComponent() throws Exception {
        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        analyzeCommand.component = "invalid-component";
        analyzeCommand.projectDir = tempDir.toString();

        Integer result = analyzeCommand.call();

        assertEquals(2, result, "Should return error for invalid component");
    }

    @Test
    void testAnalyzeCommandWithDeepAnalysis() throws Exception {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        DoctorCommand.AnalyzeCommand analyzeCommand = new DoctorCommand.AnalyzeCommand();
        analyzeCommand.component = "configuration";
        analyzeCommand.projectDir = tempDir.toString();
        analyzeCommand.deepAnalysis = true;

        Integer result = analyzeCommand.call();

        assertEquals(0, result, "Should perform deep analysis successfully");
    }

    @Test
    void testCommandAnnotationPresence() {
        picocli.CommandLine.Command commandAnnotation =
            DoctorCommand.CheckCommand.class.getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "CheckCommand should have @Command annotation");
        assertEquals("check", commandAnnotation.name());

        commandAnnotation = DoctorCommand.FixCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "FixCommand should have @Command annotation");
        assertEquals("fix", commandAnnotation.name());

        commandAnnotation = DoctorCommand.ReportCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "ReportCommand should have @Command annotation");
        assertEquals("report", commandAnnotation.name());

        commandAnnotation = DoctorCommand.AnalyzeCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "AnalyzeCommand should have @Command annotation");
        assertEquals("analyze", commandAnnotation.name());
    }

    /**
     * Integration test for command structure
     */
    @Test
    void testDoctorCommandIntegration() {
        DoctorCommand doctorCommand = new DoctorCommand();
        assertNotNull(doctorCommand, "DoctorCommand should be properly instantiated");

        picocli.CommandLine.Command commandAnnotation =
            doctorCommand.getClass().getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "DoctorCommand should have @Command annotation");
        assertEquals("doctor", commandAnnotation.name());
    }

    /**
     * Test report generation with detailed output
     */
    @Test
    void testReportCommandWithDetailedOutput() throws Exception {
        DoctorCommand.ReportCommand reportCommand = new DoctorCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.outputFile = tempDir.resolve("detailed-report.html").toString();
        reportCommand.format = "html";
        reportCommand.detailed = true;

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should generate detailed report successfully");

        Path reportPath = tempDir.resolve("detailed-report.html");
        assertTrue(Files.exists(reportPath), "Detailed report file should be created");
    }

    /**
     * Test check command with verbose output
     */
    @Test
    void testCheckCommandWithVerboseOutput() throws Exception {
        Path claudeDir = tempDir.resolve(".claude");
        Files.createDirectories(claudeDir);

        DoctorCommand.CheckCommand checkCommand = new DoctorCommand.CheckCommand();
        checkCommand.projectDir = tempDir.toString();
        checkCommand.verbose = true;

        Integer result = checkCommand.call();

        assertNotNull(result, "Should complete with verbose output");
    }
}
