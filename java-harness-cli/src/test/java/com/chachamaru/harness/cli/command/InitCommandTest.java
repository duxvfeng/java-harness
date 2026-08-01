package com.chachamaru.harness.cli.command;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InitCommand
 */
class InitCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void testCreateCommandInstantiation() {
        InitCommand.CreateCommand createCommand = new InitCommand.CreateCommand();
        assertNotNull(createCommand, "CreateCommand should be instantiated");
    }

    @Test
    void testCheckCommandInstantiation() {
        InitCommand.CheckCommand checkCommand = new InitCommand.CheckCommand();
        assertNotNull(checkCommand, "CheckCommand should be instantiated");
    }

    @Test
    void testSetupCommandInstantiation() {
        InitCommand.SetupCommand setupCommand = new InitCommand.SetupCommand();
        assertNotNull(setupCommand, "SetupCommand should be instantiated");
    }

    @Test
    void testTemplateCommandInstantiation() {
        InitCommand.TemplateCommand templateCommand = new InitCommand.TemplateCommand();
        assertNotNull(templateCommand, "TemplateCommand should be instantiated");
    }

    @Test
    void testProjectStructureCreation() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        InitCommand.ProjectStructure structure = initializer.createStructure(
            "test-project",
            tempDir.toString(),
            "java",
            true,  // dry run
            false  // force
        );

        assertNotNull(structure, "ProjectStructure should be created");
        assertTrue(structure.directories.size() > 0, "Should have directories");
        assertTrue(structure.files.size() > 0, "Should have files");
    }

    @Test
    void testProjectStructureHasRequiredDirectories() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        InitCommand.ProjectStructure structure = initializer.createStructure(
            "test-project",
            tempDir.toString(),
            "java",
            true,   // dry run
            false  // force
        );

        boolean hasSrcDir = structure.directories.stream()
            .anyMatch(d -> d.name.equals("src/main/java"));
        assertTrue(hasSrcDir, "Should have src/main/java directory");

        boolean hasClaudeDir = structure.directories.stream()
            .anyMatch(d -> d.name.equals(".claude/state"));
        assertTrue(hasClaudeDir, "Should have .claude/state directory");
    }

    @Test
    void testProjectStructureHasRequiredFiles() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        InitCommand.ProjectStructure structure = initializer.createStructure(
            "test-project",
            tempDir.toString(),
            "java",
            true,   // dry run
            false  // force
        );

        boolean hasPom = structure.files.stream()
            .anyMatch(f -> f.name.equals("pom.xml"));
        assertTrue(hasPom, "Should have pom.xml file");

        boolean hasPlans = structure.files.stream()
            .anyMatch(f -> f.name.equals("Plans.md"));
        assertTrue(hasPlans, "Should have Plans.md file");

        boolean hasGitignore = structure.files.stream()
            .anyMatch(f -> f.name.equals(".gitignore"));
        assertTrue(hasGitignore, "Should have .gitignore file");
    }

    @Test
    void testProjectCheckerValidProject() throws IOException {
        // Create a valid project structure
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);

        Path pomFile = tempDir.resolve("pom.xml");
        Files.write(pomFile, "<project></project>".getBytes(StandardCharsets.UTF_8));

        Path plansFile = tempDir.resolve("Plans.md");
        Files.write(plansFile, "# Test Plan".getBytes(StandardCharsets.UTF_8));

        InitCommand.ProjectChecker checker = new InitCommand.ProjectChecker(false);
        InitCommand.CheckResult result = checker.checkProject(tempDir);

        assertNotNull(result, "Should return check result");
        assertTrue(result.isValid, "Project should be valid");
    }

    @Test
    void testProjectCheckerMissingPom() throws IOException {
        // Create directory without pom.xml
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);

        InitCommand.ProjectChecker checker = new InitCommand.ProjectChecker(false);
        InitCommand.CheckResult result = checker.checkProject(tempDir);

        assertNotNull(result, "Should return check result");
        assertFalse(result.isValid, "Project without pom.xml should be invalid");
        assertTrue(result.errorCount > 0, "Should have errors");
    }

    @Test
    void testProjectCheckerMissingSource() throws IOException {
        // Create pom.xml without source directory
        Path pomFile = tempDir.resolve("pom.xml");
        Files.write(pomFile, "<project></project>".getBytes(StandardCharsets.UTF_8));

        InitCommand.ProjectChecker checker = new InitCommand.ProjectChecker(false);
        InitCommand.CheckResult result = checker.checkProject(tempDir);

        assertNotNull(result, "Should return check result");
        assertTrue(result.warningCount > 0, "Should have warnings for missing source");
    }

    @Test
    void testProjectCheckerProjectType() throws IOException {
        // Maven project
        Path pomFile = tempDir.resolve("pom.xml");
        Files.write(pomFile, "<project></project>".getBytes(StandardCharsets.UTF_8));

        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);

        InitCommand.ProjectChecker checker = new InitCommand.ProjectChecker(false);
        InitCommand.CheckResult result = checker.checkProject(tempDir);

        assertEquals("maven-java", result.projectType, "Should detect Maven project");
    }

    @Test
    void testEnvironmentSetup() {
        InitCommand.EnvironmentSetup setup = new InitCommand.EnvironmentSetup(false);
        InitCommand.SetupResult result = setup.setup(
            tempDir.toString(),
            true,  // skip deps
            true,  // offline
            true   // minimal
        );

        assertNotNull(result, "Should return setup result");
        assertTrue(result.totalSteps > 0, "Should have steps to execute");
        assertTrue(result.completedSteps + result.failedSteps + result.skippedSteps > 0,
                   "Should have some progress");
    }

    @Test
    void testTemplateEngineBasic() {
        InitCommand.TemplateEngine engine = new InitCommand.TemplateEngine();
        boolean created = engine.createFromTemplate(
            "basic",
            tempDir.toString()
        );

        assertTrue(created, "Should create project from basic template");
    }

    @Test
    void testTemplateEngineStandard() {
        InitCommand.TemplateEngine engine = new InitCommand.TemplateEngine();
        boolean created = engine.createFromTemplate(
            "standard",
            tempDir.toString()
        );

        assertTrue(created, "Should create project from standard template");
    }

    @Test
    void testTemplateEngineSpringBoot() {
        InitCommand.TemplateEngine engine = new InitCommand.TemplateEngine();
        boolean created = engine.createFromTemplate(
            "spring-boot",
            tempDir.toString()
        );

        assertTrue(created, "Should create project from spring-boot template");
    }

    @Test
    void testGitIgnoreContent() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        String gitignore = initializer.generateGitIgnore();

        assertTrue(gitignore.contains("*.class"), "Should ignore Java class files");
        assertTrue(gitignore.contains("target/"), "Should ignore target directory");
        assertTrue(gitignore.contains(".claude/"), "Should ignore .claude directory");
    }

    @Test
    void testPlansTemplateContent() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        String plans = initializer.generatePlansTemplate("test-project");

        assertTrue(plans.contains("test-project"), "Should include project name");
        assertTrue(plans.contains("| Task |"), "Should have task table header");
        assertTrue(plans.contains("## Phase"), "Should have phase sections");
    }

    @Test
    void testPomTemplateContent() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        String pom = initializer.generatePomTemplate("test-project", "java");

        assertTrue(pom.contains("<project"), "Should be valid XML - POM should contain <project> tag");
        assertTrue(pom.contains("test-project"), "Should include artifact ID");
        assertTrue(pom.contains("<groupId>"), "Should have groupId");
        assertTrue(pom.contains("</project>"), "Should have closing project tag");
    }

    @Test
    void testReadmeTemplateContent() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        String readme = initializer.generateReadmeTemplate("test-project");

        assertTrue(readme.contains("# test-project"), "Should have title");
        assertTrue(readme.contains("## 概述"), "Should have overview section");
        assertTrue(readme.contains("## 快速开始"), "Should have quick start section");
    }

    @Test
    void testSettingsTemplateContent() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        String settings = initializer.generateSettingsTemplate();

        assertTrue(settings.contains("{"), "Should be valid JSON");
        assertTrue(settings.contains("version"), "Should have version field");
        assertTrue(settings.contains("cli"), "Should have CLI configuration");
    }

    @Test
    void testProjectStructureAddDirectory() {
        InitCommand.ProjectStructure structure = new InitCommand.ProjectStructure();
        structure.addDirectory("test/dir");

        assertTrue(structure.directories.size() == 1, "Should have one directory");
        assertEquals("test/dir", structure.directories.get(0).name, "Directory name should match");
    }

    @Test
    void testProjectStructureAddFile() {
        InitCommand.ProjectStructure structure = new InitCommand.ProjectStructure();
        structure.addFile("test.txt", "content");

        assertTrue(structure.files.size() == 1, "Should have one file");
        assertEquals("test.txt", structure.files.get(0).name, "File name should match");
        assertEquals("content", structure.files.get(0).content, "File content should match");
    }

    @Test
    void testCheckResultStructure() {
        InitCommand.CheckResult result = new InitCommand.CheckResult();

        result.isValid = true;
        result.projectType = "maven-java";
        result.issueCount = 0;
        result.errorCount = 0;
        result.warningCount = 0;
        result.checkedAt = "2023-01-01T12:00:00";

        assertTrue(result.isValid, "Should set valid flag");
        assertEquals("maven-java", result.projectType, "Should set project type");
        assertEquals(0, result.issueCount, "Should set issue count");
    }

    @Test
    void testCheckIssueStructure() {
        InitCommand.CheckIssue issue = new InitCommand.CheckIssue();

        issue.severity = "error";
        issue.category = "Build System";
        issue.message = "Missing pom.xml";
        issue.recommendation = "Run: mvn generate";

        assertEquals("error", issue.severity, "Should set severity");
        assertEquals("Build System", issue.category, "Should set category");
        assertEquals("Missing pom.xml", issue.message, "Should set message");
        assertEquals("Run: mvn generate", issue.recommendation, "Should set recommendation");
    }

    @Test
    void testSetupResultStructure() {
        InitCommand.SetupResult result = new InitCommand.SetupResult();

        result.totalSteps = 5;
        result.completedSteps = 3;
        result.skippedSteps = 1;
        result.failedSteps = 1;

        assertEquals(5, result.totalSteps, "Should set total steps");
        assertEquals(3, result.completedSteps, "Should set completed steps");
        assertEquals(1, result.skippedSteps, "Should set skipped steps");
        assertEquals(1, result.failedSteps, "Should set failed steps");
    }

    @Test
    void testCommandAnnotationPresence() {
        picocli.CommandLine.Command commandAnnotation =
            InitCommand.CreateCommand.class.getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "CreateCommand should have @Command annotation");
        assertEquals("create", commandAnnotation.name());

        commandAnnotation = InitCommand.CheckCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "CheckCommand should have @Command annotation");
        assertEquals("check", commandAnnotation.name());

        commandAnnotation = InitCommand.SetupCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "SetupCommand should have @Command annotation");
        assertEquals("setup", commandAnnotation.name());

        commandAnnotation = InitCommand.TemplateCommand.class.getAnnotation(picocli.CommandLine.Command.class);
        assertNotNull(commandAnnotation, "TemplateCommand should have @Command annotation");
        assertEquals("template", commandAnnotation.name());
    }

    /**
     * Integration test for command structure
     */
    @Test
    void testInitCommandIntegration() {
        InitCommand initCommand = new InitCommand();
        assertNotNull(initCommand, "InitCommand should be properly instantiated");

        picocli.CommandLine.Command commandAnnotation =
            initCommand.getClass().getAnnotation(picocli.CommandLine.Command.class);

        assertNotNull(commandAnnotation, "InitCommand should have @Command annotation");
        assertEquals("init", commandAnnotation.name());
    }

    /**
     * Integration test: Create a real project structure
     */
    @Test
    void testCreateRealProject() throws IOException {
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        InitCommand.ProjectStructure structure = initializer.createStructure(
            "integration-test-project",
            tempDir.toString(),
            "java",
            false,  // not dry run
            false   // force
        );

        // Verify files were actually created
        Path pomPath = tempDir.resolve("pom.xml");
        assertTrue(Files.exists(pomPath), "pom.xml should be created");

        Path plansPath = tempDir.resolve("Plans.md");
        assertTrue(Files.exists(plansPath), "Plans.md should be created");

        Path gitignorePath = tempDir.resolve(".gitignore");
        assertTrue(Files.exists(gitignorePath), ".gitignore should be created");

        Path srcDir = tempDir.resolve("src/main/java");
        assertTrue(Files.exists(srcDir), "src/main/java directory should be created");

        Path claudeDir = tempDir.resolve(".claude/state");
        assertTrue(Files.exists(claudeDir), ".claude/state directory should be created");
    }

    /**
     * Integration test: Check a created project
     */
    @Test
    void testCheckCreatedProject() throws IOException {
        // First create a project
        InitCommand.ProjectInitializer initializer = new InitCommand.ProjectInitializer(false);
        initializer.createStructure(
            "check-test-project",
            tempDir.toString(),
            "java",
            false,  // not dry run
            false   // force
        );

        // Then check it
        InitCommand.ProjectChecker checker = new InitCommand.ProjectChecker(false);
        InitCommand.CheckResult result = checker.checkProject(tempDir);

        assertTrue(result.isValid, "Created project should be valid");
        assertEquals(0, result.errorCount, "Should have no errors");
    }
}
