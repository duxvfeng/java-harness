package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EvidenceCommand
 */
class EvidenceCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testCollectCommandInstantiation() {
        EvidenceCommand.CollectCommand collectCommand = new EvidenceCommand.CollectCommand();
        assertNotNull(collectCommand, "CollectCommand should be instantiated");
    }

    @Test
    void testListCommandInstantiation() {
        EvidenceCommand.ListCommand listCommand = new EvidenceCommand.ListCommand();
        assertNotNull(listCommand, "ListCommand should be instantiated");
    }

    @Test
    void testArchiveCommandInstantiation() {
        EvidenceCommand.ArchiveCommand archiveCommand = new EvidenceCommand.ArchiveCommand();
        assertNotNull(archiveCommand, "ArchiveCommand should be instantiated");
    }

    @Test
    void testReportCommandInstantiation() {
        EvidenceCommand.ReportCommand reportCommand = new EvidenceCommand.ReportCommand();
        assertNotNull(reportCommand, "ReportCommand should be instantiated");
    }

    @Test
    void testCollectCommandBasicCall() throws Exception {
        EvidenceCommand.CollectCommand collectCommand = new EvidenceCommand.CollectCommand();
        collectCommand.projectDir = tempDir.toString();
        collectCommand.verbose = true;

        Integer result = collectCommand.call();

        assertEquals(0, result, "Should return 0 for successful collection");
    }

    @Test
    void testCollectCommandWithInvalidDirectory() throws Exception {
        EvidenceCommand.CollectCommand collectCommand = new EvidenceCommand.CollectCommand();
        collectCommand.projectDir = "/nonexistent/directory";

        Integer result = collectCommand.call();

        assertEquals(1, result, "Should return 1 for invalid directory");
    }

    @Test
    void testCollectCommandTestsOnly() throws Exception {
        // Create mock test results directory
        Path testDir = tempDir.resolve("target/surefire-reports");
        Files.createDirectories(testDir);
        Files.write(testDir.resolve("TEST-test.xml"), "<testsuites></testsuites>".getBytes(StandardCharsets.UTF_8));

        EvidenceCommand.CollectCommand collectCommand = new EvidenceCommand.CollectCommand();
        collectCommand.projectDir = tempDir.toString();
        collectCommand.type = "tests";

        Integer result = collectCommand.call();

        assertEquals(0, result, "Should return 0 for tests collection");
    }

    @Test
    void testCollectCommandLogsOnly() throws Exception {
        // Create mock logs directory
        Path logDir = tempDir.resolve("target/logs");
        Files.createDirectories(logDir);
        Files.write(logDir.resolve("build.log"), "Build log content".getBytes(StandardCharsets.UTF_8));

        EvidenceCommand.CollectCommand collectCommand = new EvidenceCommand.CollectCommand();
        collectCommand.projectDir = tempDir.toString();
        collectCommand.type = "logs";

        Integer result = collectCommand.call();

        assertEquals(0, result, "Should return 0 for logs collection");
    }

    @Test
    void testCollectCommandCoverageOnly() throws Exception {
        // Create mock coverage directory
        Path coverageDir = tempDir.resolve("target/site/jacoco");
        Files.createDirectories(coverageDir);
        Files.write(coverageDir.resolve("index.html"), "<html>Coverage</html>".getBytes(StandardCharsets.UTF_8));

        EvidenceCommand.CollectCommand collectCommand = new EvidenceCommand.CollectCommand();
        collectCommand.projectDir = tempDir.toString();
        collectCommand.type = "coverage";

        Integer result = collectCommand.call();

        assertEquals(0, result, "Should return 0 for coverage collection");
    }

    @Test
    void testCollectCommandWithFailuresOnly() throws Exception {
        // Create test directory with both pass and fail files
        Path testDir = tempDir.resolve("target/surefire-reports");
        Files.createDirectories(testDir);
        Files.write(testDir.resolve("TEST-pass.xml"), "<testsuites><testsuite/></testsuites>".getBytes(StandardCharsets.UTF_8));
        Files.write(testDir.resolve("TEST-fail.xml"), "<testsuites><testsuite><failure/></testsuite></testsuites>".getBytes(StandardCharsets.UTF_8));

        EvidenceCommand.CollectCommand collectCommand = new EvidenceCommand.CollectCommand();
        collectCommand.projectDir = tempDir.toString();
        collectCommand.type = "tests";
        collectCommand.includeFailuresOnly = true;

        Integer result = collectCommand.call();

        assertEquals(0, result, "Should return 0 for failures-only collection");
    }

    @Test
    void testListCommandBasicCall() throws Exception {
        // Create mock evidence directory
        Path evidenceDir = tempDir.resolve(".claude/evidence");
        Files.createDirectories(evidenceDir);
        Files.write(evidenceDir.resolve("test-result.xml"), "<testsuites></testsuites>".getBytes(StandardCharsets.UTF_8));

        EvidenceCommand.ListCommand listCommand = new EvidenceCommand.ListCommand();
        listCommand.projectDir = tempDir.toString();

        Integer result = listCommand.call();

        assertEquals(0, result, "Should return 0 for successful list");
    }

    @Test
    void testListCommandWithJsonFormat() throws Exception {
        Path evidenceDir = tempDir.resolve(".claude/evidence");
        Files.createDirectories(evidenceDir);

        EvidenceCommand.ListCommand listCommand = new EvidenceCommand.ListCommand();
        listCommand.projectDir = tempDir.toString();
        listCommand.format = "json";

        Integer result = listCommand.call();

        assertEquals(0, result, "Should return 0 for JSON format");
    }

    @Test
    void testListCommandWithDetailedFormat() throws Exception {
        Path evidenceDir = tempDir.resolve(".claude/evidence");
        Files.createDirectories(evidenceDir);

        EvidenceCommand.ListCommand listCommand = new EvidenceCommand.ListCommand();
        listCommand.projectDir = tempDir.toString();
        listCommand.format = "detailed";

        Integer result = listCommand.call();

        assertEquals(0, result, "Should return 0 for detailed format");
    }

    @Test
    void testListCommandWithInvalidDirectory() throws Exception {
        EvidenceCommand.ListCommand listCommand = new EvidenceCommand.ListCommand();
        listCommand.projectDir = "/nonexistent/directory";

        Integer result = listCommand.call();

        assertEquals(1, result, "Should return 1 for invalid directory");
    }

    @Test
    void testArchiveCommandBasicCall() throws Exception {
        EvidenceCommand.ArchiveCommand archiveCommand = new EvidenceCommand.ArchiveCommand();
        archiveCommand.projectDir = tempDir.toString();
        archiveCommand.outputFile = "test-archive.zip";

        Integer result = archiveCommand.call();

        assertEquals(0, result, "Should return 0 for archive command");
    }

    @Test
    void testArchiveCommandWithInvalidDirectory() throws Exception {
        EvidenceCommand.ArchiveCommand archiveCommand = new EvidenceCommand.ArchiveCommand();
        archiveCommand.projectDir = "/nonexistent/directory";

        Integer result = archiveCommand.call();

        assertEquals(1, result, "Should return 1 for invalid directory");
    }

    @Test
    void testReportCommandBasicCall() throws Exception {
        EvidenceCommand.ReportCommand reportCommand = new EvidenceCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.outputFile = "evidence-report.md";

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should return 0 for report generation");
    }

    @Test
    void testReportCommandJsonFormat() throws Exception {
        EvidenceCommand.ReportCommand reportCommand = new EvidenceCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.outputFile = "evidence-report.json";
        reportCommand.format = "json";

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should return 0 for JSON report");
    }

    @Test
    void testReportCommandHtmlFormat() throws Exception {
        EvidenceCommand.ReportCommand reportCommand = new EvidenceCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.outputFile = "evidence-report.html";
        reportCommand.format = "html";

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should return 0 for HTML report");
    }

    @Test
    void testReportCommandWithDetailedOutput() throws Exception {
        EvidenceCommand.ReportCommand reportCommand = new EvidenceCommand.ReportCommand();
        reportCommand.projectDir = tempDir.toString();
        reportCommand.detailed = true;

        Integer result = reportCommand.call();

        assertEquals(0, result, "Should return 0 for detailed report");
    }

    @Test
    void testEvidenceCollector() {
        Path evidenceDir = tempDir.resolve(".claude/evidence");
        EvidenceCommand.EvidenceCollector collector = new EvidenceCommand.EvidenceCollector(tempDir, evidenceDir, false);

        assertNotNull(collector, "Collector should be instantiated");
    }

    @Test
    void testEvidenceCollectorWithMockData() throws Exception {
        // Create mock test results
        Path testDir = tempDir.resolve("target/surefire-reports");
        Files.createDirectories(testDir);
        Files.write(testDir.resolve("TEST-test.xml"), "<testsuites></testsuites>".getBytes(StandardCharsets.UTF_8));

        Path evidenceDir = tempDir.resolve(".claude/evidence");
        EvidenceCommand.EvidenceCollector collector = new EvidenceCommand.EvidenceCollector(tempDir, evidenceDir, false);

        var result = collector.collectTestResults(false);

        assertNotNull(result, "Should return collection result");
        assertEquals(evidenceDir.toString(), result.outputPath);
    }

    @Test
    void testEvidenceLister() throws Exception {
        Path evidenceDir = tempDir.resolve(".claude/evidence");
        Files.createDirectories(evidenceDir);
        Files.write(evidenceDir.resolve("test-result.xml"), "<testsuites></testsuites>".getBytes(StandardCharsets.UTF_8));

        EvidenceCommand.EvidenceLister lister = new EvidenceCommand.EvidenceLister(evidenceDir);
        var items = lister.listEvidence();

        assertNotNull(items, "Should return evidence list");
        assertFalse(items.isEmpty(), "Should have at least one evidence item");
    }

    @Test
    void testEvidenceArchiver() {
        EvidenceCommand.EvidenceArchiver archiver = new EvidenceCommand.EvidenceArchiver(tempDir, false);

        assertNotNull(archiver, "Archiver should be instantiated");
    }

    @Test
    void testEvidenceReporter() {
        EvidenceCommand.EvidenceReporter reporter = new EvidenceCommand.EvidenceReporter(tempDir, false);

        assertNotNull(reporter, "Reporter should be instantiated");
    }

    @Test
    void testCollectionResult() {
        EvidenceCommand.CollectionResult result = new EvidenceCommand.CollectionResult();

        assertEquals(0, result.testResultCount);
        assertEquals(0, result.buildLogCount);
        assertEquals(0, result.coverageReportCount);
        assertEquals(0, result.totalFiles);
        assertEquals("", result.outputPath);
    }

    @Test
    void testCollectionResultWithPath() {
        String outputPath = "/test/output";
        EvidenceCommand.CollectionResult result = new EvidenceCommand.CollectionResult(outputPath);

        assertEquals(outputPath, result.outputPath);
    }

    @Test
    void testArchiveResult() {
        EvidenceCommand.ArchiveResult result = new EvidenceCommand.ArchiveResult("/test/archive.zip", 10, 1024);

        assertEquals("/test/archive.zip", result.archivePath);
        assertEquals(10, result.fileCount);
        assertEquals(1024, result.totalSize);
    }

    @Test
    void testEvidenceItemRecord() {
        var now = java.time.LocalDateTime.now();
        EvidenceCommand.EvidenceItem item = new EvidenceCommand.EvidenceItem(
            "test-result.xml",
            "test-result",
            "/path/to/test-result.xml",
            1024,
            now
        );

        assertEquals("test-result.xml", item.name());
        assertEquals("test-result", item.type());
        assertEquals("/path/to/test-result.xml", item.path());
        assertEquals(1024, item.size());
        assertEquals(now, item.timestamp());
    }

    @Test
    void testCommandAnnotationPresence() {
        picocli.CommandLine.Command commandAnnotation =
            EvidenceCommand.CollectCommand.class.getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "CollectCommand should have @Command annotation");
        assertEquals("collect", commandAnnotation.name());

        commandAnnotation = EvidenceCommand.ListCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "ListCommand should have @Command annotation");
        assertEquals("list", commandAnnotation.name());

        commandAnnotation = EvidenceCommand.ArchiveCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "ArchiveCommand should have @Command annotation");
        assertEquals("archive", commandAnnotation.name());

        commandAnnotation = EvidenceCommand.ReportCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "ReportCommand should have @Command annotation");
        assertEquals("report", commandAnnotation.name());
    }

    /**
     * Integration test for command structure
     */
    @Test
    void testEvidenceCommandIntegration() {
        EvidenceCommand evidenceCommand = new EvidenceCommand();
        assertNotNull(evidenceCommand, "EvidenceCommand should be properly instantiated");

        picocli.CommandLine.Command commandAnnotation =
            evidenceCommand.getClass().getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "EvidenceCommand should have @Command annotation");
        assertEquals("evidence", commandAnnotation.name());
    }
}
