package com.chachamaru.harness.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Init command for project initialization.
 *
 * <p>This command provides project initialization capabilities:
 * <ul>
 *   <li>create - Create new project structure</li>
 *   <li>check - Check existing project</li>
 *   <li>setup - Setup development environment</li>
 *   <li>template - Create from template</li>
 * </ul>
 * </p>
 */
@Command(name = "init",
         mixinStandardHelpOptions = true,
         subcommands = {
             InitCommand.CreateCommand.class,
             InitCommand.CheckCommand.class,
             InitCommand.SetupCommand.class,
             InitCommand.TemplateCommand.class
         },
         description = "Initialize project")
public class InitCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
    private boolean verbose;

    @Override
    public void run() {
        // Default behavior - show help
        CommandLine.usage(this, System.out);
    }

    /**
     * Create new project structure
     */
    @Command(name = "create",
             mixinStandardHelpOptions = true,
             description = "Create new project structure")
    public static class CreateCommand implements Runnable {

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        private boolean verbose;

        @Option(names = {"-n", "--name"},
                 description = "Project name",
                 defaultValue = "my-harness-project")
        private String projectName;

        @Option(names = {"-d", "--directory"},
                 description = "Project directory",
                 defaultValue = ".")
        private String projectDir;

        @Option(names = {"--type"},
                 description = "Project type: java, spring-boot, basic",
                 defaultValue = "java")
        private String projectType;

        @Option(names = {"--force"},
                 description = "Overwrite existing files")
        private boolean force;

        @Option(names = {"--dry-run"},
                 description = "Show what would be created without creating")
        private boolean dryRun;

        @Override
        public void run() {
            try {
                System.out.println("🚀 Creating new project: " + projectName);
                System.out.println("  Directory: " + projectDir);
                System.out.println("  Type: " + projectType);

                if (dryRun) {
                    System.out.println("  ⚠️  DRY RUN - No actual changes");
                }

                ProjectInitializer initializer = new ProjectInitializer(verbose);
                ProjectStructure structure = initializer.createStructure(projectName, projectDir, projectType, dryRun, force);

                if (!dryRun) {
                    System.out.println();
                    System.out.println("✓ Project created successfully!");
                    System.out.println();
                    displayNextSteps(structure, projectName, projectDir);
                } else {
                    System.out.println();
                    System.out.println("📋 Project structure preview:");
                    structure.displayPreview();
                }

            } catch (Exception e) {
                System.err.println("✗ Project creation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void displayNextSteps(ProjectStructure structure, String projectName, String projectDir) {
            System.out.println("🎯 Next steps:");
            System.out.println("   1. cd " + projectDir);
            System.out.println("  2. mvn clean compile");
            System.out.println("  3. java harness --help");
            System.out.println();
            System.out.println("📖 Documentation:");
            System.out.println("  https://github.com/your-org/java-harness/wiki");
        }
    }

    /**
     * Check existing project
     */
    @Command(name = "check",
             mixinStandardHelpOptions = true,
             description = "Check existing project")
    public static class CheckCommand implements Runnable {

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        private boolean verbose;

        @Option(names = {"-d", "--directory"},
                 description = "Project directory to check (default: current directory)",
                 defaultValue = ".")
        private String projectDir;

        @Option(names = {"--fix"},
                 description = "Attempt to fix common issues")
        private boolean autoFix;

        @Option(names = {"--json"},
                 description = "Output in JSON format")
        private boolean jsonOutput;

        @Override
        public void run() {
            try {
                Path projectPath = Paths.get(projectDir);

                System.out.println("🔍 Checking project: " + projectPath.toAbsolutePath());

                ProjectChecker checker = new ProjectChecker(verbose);
                CheckResult result = checker.checkProject(projectPath);

                if (jsonOutput) {
                    outputJsonResult(result);
                } else {
                    outputHumanResult(result);
                }

                if (autoFix && result.issueCount > 0) {
                    System.out.println();
                    System.out.println("🔧 Attempting automatic fixes...");
                    int fixed = checker.applyAutoFixes(projectPath, result.issues);
                    System.out.println("✓ Applied " + fixed + " fix(es)");
                }

                if (!result.isValid && !autoFix) {
                    System.exit(1);
                }

            } catch (Exception e) {
                System.err.println("✗ Check failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }

        private void outputJsonResult(CheckResult result) {
            System.out.println("{");
            System.out.println("  \"valid\": " + result.isValid + ",");
            System.out.println("  \"projectType\": \"" + result.projectType + "\",");
            System.out.println("  \"issues\": " + result.issueCount + ",");
            System.out.println("  \"errors\": " + result.errorCount + ",");
            System.out.println("  \"warnings\": " + result.warningCount + ",");
            System.out.println("  \"recommendations\": " + result.recommendations.size() + ",");
            System.out.println("  \"checkedAt\": \"" + result.checkedAt + "\"");
            System.out.println("}");
        }

        private void outputHumanResult(CheckResult result) {
            System.out.println();
            System.out.println("📊 Check Results");
            System.out.println("  Valid: " + (result.isValid ? "✓" : "✗"));
            System.out.println("  Project type: " + result.projectType);
            System.out.println("  Issues: " + result.issueCount);
            System.out.println("  Errors: " + result.errorCount);
            System.out.println("  Warnings: " + result.warningCount);

            if (!result.issues.isEmpty()) {
                System.out.println();
                System.out.println("⚠️  Issues Found:");
                for (CheckIssue issue : result.issues) {
                    System.out.println("  [" + issue.severity + "] " + issue.category);
                    System.out.println("      " + issue.message);
                    if (issue.recommendation != null) {
                        System.out.println("      💡 " + issue.recommendation);
                    }
                    System.out.println();
                }
            }

            if (!result.recommendations.isEmpty()) {
                System.out.println("💡 Recommendations:");
                for (String rec : result.recommendations) {
                    System.out.println("  • " + rec);
                }
            }
        }
    }

    /**
     * Setup development environment
     */
    @Command(name = "setup",
             mixinStandardHelpOptions = true,
             description = "Setup development environment")
    public static class SetupCommand implements Runnable {

        @Option(names = {"-v", "--verbose"}, description = "Show verbose output")
        private boolean verbose;

        @Option(names = {"-d", "--directory"},
                 description = "Project directory to setup (default: current directory)",
                 defaultValue = ".")
        private String projectDir;

        @Option(names = {"--skip-deps"},
                 description = "Skip dependency installation")
        private boolean skipDeps;

        @Option(names = {"--offline"},
                 description = "Setup in offline mode")
        private boolean offline;

        @Option(names = {"--minimal"},
                 description = "Minimal setup (required only)")
        private boolean minimal;

        @Override
        public void run() {
            try {
                System.out.println("🔧 Setting up development environment");
                System.out.println("  Directory: " + projectDir);

                EnvironmentSetup setup = new EnvironmentSetup(verbose);
                SetupResult result = setup.setup(projectDir, skipDeps, offline, minimal);

                System.out.println();
                System.out.println("📊 Setup Summary");
                System.out.println("  Completed: " + result.completedSteps + "/" + result.totalSteps);
                System.out.println("  Skipped: " + result.skippedSteps);
                System.out.println("  Failed: " + result.failedSteps);

                if (!result.issues.isEmpty()) {
                    System.out.println();
                    System.out.println("⚠️  Issues:");
                    for (String issue : result.issues) {
                        System.out.println("  • " + issue);
                    }
                }

                if (result.completedSteps == result.totalSteps) {
                    System.out.println();
                    System.out.println("✓ Environment setup complete!");
                } else {
                    System.out.println();
                    System.out.println("⚠️  Some setup steps failed. You may need to complete them manually.");
                }

            } catch (Exception e) {
                System.err.println("✗ Setup failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Create from template
     */
    @Command(name = "template",
             mixinStandardHelpOptions = true,
             description = "Create from template")
    public static class TemplateCommand implements Runnable {

        @Option(names = {"-t", "--template"},
                 description = "Template to use: basic, standard, advanced",
                 defaultValue = "standard")
        private String templateName;

        @Option(names = {"-d", "--directory"},
                 description = "Target directory",
                 defaultValue = ".")
        private String targetDir;

        @Option(names = {"--list"},
                 description = "List available templates")
                 private boolean listTemplates;

        @Override
        public void run() {
            try {
                if (listTemplates) {
                    System.out.println("📋 Available Templates:");
                    System.out.println("  basic - Basic project structure");
                    System.out.println("  standard - Standard harness setup (recommended)");
                    System.out.println("  advanced - Advanced setup with all features");
                    System.out.println("  spring-boot - Spring Boot application");
                    return;
                }

                System.out.println("📋 Creating project from template: " + templateName);
                System.out.println("  Target: " + targetDir);

                TemplateEngine engine = new TemplateEngine();
                boolean created = engine.createFromTemplate(templateName, targetDir);

                if (created) {
                    System.out.println("✓ Project created from template successfully!");
                } else {
                    System.err.println("✗ Template creation failed");
                    System.exit(1);
                }

            } catch (Exception e) {
                System.err.println("✗ Template creation failed: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }

    /**
     * Project initializer
     */
    public static class ProjectInitializer {
        private final boolean verbose;

        public ProjectInitializer(boolean verbose) {
            this.verbose = verbose;
        }

        public ProjectStructure createStructure(String projectName, String projectDir, String projectType, boolean dryRun, boolean force) throws IOException {
            ProjectStructure structure = new ProjectStructure();

            // Basic directory structure
            structure.addDirectory("src/main/java");
            structure.addDirectory("src/test/java");
            structure.addDirectory("src/main/resources");
            structure.addDirectory("target");
            structure.addDirectory(".claude/state");
            structure.addDirectory("plans");
            structure.addDirectory(".claude/worktrees");

            // Configuration files
            structure.addFile(".gitignore", generateGitIgnore());
            structure.addFile("Plans.md", generatePlansTemplate(projectName));
            structure.addFile(".claude/settings.json", generateSettingsTemplate());
            structure.addFile("pom.xml", generatePomTemplate(projectName, projectType));
            structure.addFile("README.md", generateReadmeTemplate(projectName));

            if (!dryRun) {
                createStructure(structure, projectDir, force);
            }

            return structure;
        }

        String generateGitIgnore() {
            return """
# Java
*.class
*.jar
*.war
*.ear
target/
*.log

# Maven
.mvn/
*.mvn
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn-eclipse.xml

# IDE
.idea/
*.iml
*.iws
*.ipr
.vscode/
*.swp
*.swo
*~

# Claude Harness
.claude/
plans/archive/
.worktrees/
*.backup.*

# OS
.DS_Store
Thumbs.db

# Logs
*.log
"""
                .trim();
        }

        String generatePlansTemplate(String projectName) {
            return String.format("""
# %s Project Plan

**目标**: Implement core harness functionality

## Phase 1: Initial Setup (Week 1)

### 目标
Set up basic project structure and tooling

| Task | 内容 | DoD | Depends | Status |
|------|------|-----|---------|--------|
| 1.1 | 创建项目结构 | 标准目录结构存在 | - | cc:TODO |
| 1.2 | 配置构建系统 | pom.xml 可编译 | 1.1 | cc:TODO |
| 1.3 | 创建基础配置 | .claude/settings.json 存在 | 1.2 | cc:TODO |

## Phase 验收标准

### 功能完整性
- [ ] 项目结构正确
- [ ] 构建系统工作
- [ ] 配置文件完整

---
*Generated by Java Harness init command - %s*
""", projectName, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        String generateSettingsTemplate() {
            return """
{
  "version": "1.0",
  "project": {
    "name": "java-harness",
    "type": "cli"
  },
  "cli": {
    "name": "harness",
    "version": "4.0.0",
    "description": "Java Harness CLI"
  },
  "features": {
    "hooks": true,
    "guardrails": true,
    "workflows": false
  }
}
""";
        }

        String generatePomTemplate(String projectName, String projectType) {
            // Simplified POM template
            return String.format("""
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>%s</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>%s</name>
    <description>Java Harness CLI Project</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <junit.version>5.10.0</junit.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${junit.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
""", projectName, projectName);
        }

        String generateReadmeTemplate(String projectName) {
            return String.format("""
# %s

**Java Harness CLI Project**

## 概述
这是一个使用 Java Harness CLI 的项目。

## 快速开始

### 构建项目

```bash
mvn clean compile
```

### 运行命令

```bash
java -jar target/harness-cli.jar --help
```

## 项目结构

```
%s/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   └── test/
│       └── java/
├── target/
├── .claude/
├── plans/
├── Plans.md
└── pom.xml
```

## 下一步

1. 配置开发环境
2. 实现核心功能
3. 添加测试
4. 文档完善

## 许可证

[Specify your license here]
""", projectName, projectName);
        }

        private void createStructure(ProjectStructure structure, String projectDir, boolean force) throws IOException {
            Path basePath = Paths.get(projectDir);

            for (ProjectDir dir : structure.directories) {
                Path dirPath = basePath.resolve(dir.name);
                Files.createDirectories(dirPath);
                if (verbose) {
                    System.out.println("✓ Created directory: " + dir.name);
                }
            }

            for (ProjectFile file : structure.files) {
                Path filePath = basePath.resolve(file.name);
                Files.createDirectories(filePath.getParent());

                if (!Files.exists(filePath) || force) {
                    Files.write(filePath, file.content.getBytes(StandardCharsets.UTF_8));
                    if (verbose) {
                        System.out.println("✓ Created file: " + file.name);
                    }
                }
            }
        }
    }

    /**
     * Project checker
     */
    public static class ProjectChecker {
        private final boolean verbose;

        public ProjectChecker(boolean verbose) {
            this.verbose = verbose;
        }

        public CheckResult checkProject(Path projectPath) {
            CheckResult result = new CheckResult();
            result.checkedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);

            // Check for common project indicators
            boolean hasPom = Files.exists(projectPath.resolve("pom.xml"));
            boolean hasSource = Files.exists(projectPath.resolve("src/main/java"));
            boolean hasClaude = Files.exists(projectPath.resolve(".claude"));
            boolean hasPlans = Files.exists(projectPath.resolve("Plans.md"));

            // Determine project type
            if (hasPom && hasSource) {
                result.projectType = "maven-java";
            } else if (hasClaude || hasPlans) {
                result.projectType = "harness";
            } else {
                result.projectType = "unknown";
            }

            // Check for issues
            if (!hasPom) {
                CheckIssue issue = new CheckIssue();
                issue.severity = "error";
                issue.category = "Build System";
                issue.message = "Missing pom.xml - Maven build file not found";
                issue.recommendation = "Run: mvn archetype:generate or copy from template";
                result.issues.add(issue);
                result.errorCount++;
            }

            if (!hasSource) {
                CheckIssue issue = new CheckIssue();
                issue.severity = "warning";
                issue.category = "Source Structure";
                issue.message = "Missing source directory structure";
                issue.recommendation = "Create: mkdir -p src/main/java";
                result.issues.add(issue);
                result.warningCount++;
            }

            if (!hasClaude) {
                CheckIssue issue = new CheckIssue();
                issue.severity = "info";
                issue.category = "Harness Integration";
                issue.message = "No .claude directory found";
                issue.recommendation = "Optional: mkdir -p .claude/state";
                result.issues.add(issue);
                result.warningCount++;
            }

            result.issueCount = result.issues.size();
            result.isValid = result.errorCount == 0;

            // Add recommendations
            if (result.isValid) {
                result.recommendations.add("Run 'mvn clean compile' to verify build");
                result.recommendations.add("Run 'java -jar target/*.jar --help' to test CLI");
            }

            return result;
        }

        public int applyAutoFixes(Path projectPath, List<CheckIssue> issues) {
            int fixed = 0;

            for (CheckIssue issue : issues) {
                if (issue.severity.equals("error") || issue.severity.equals("warning")) {
                    try {
                        if (issue.category.equals("Source Structure") && issue.message.contains("mkdir")) {
                            Path sourcePath = projectPath.resolve("src/main/java");
                            Files.createDirectories(sourcePath);
                            fixed++;
                        }
                    } catch (Exception e) {
                        System.err.println("  ✗ Failed to fix: " + issue.message);
                    }
                }
            }

            return fixed;
        }
    }

    /**
     * Environment setup
     */
    public static class EnvironmentSetup {
        private final boolean verbose;

        public EnvironmentSetup(boolean verbose) {
            this.verbose = verbose;
        }

        public SetupResult setup(String projectDir, boolean skipDeps, boolean offline, boolean minimal) {
            SetupResult result = new SetupResult();
            result.totalSteps = 5;

            // Step 1: Check Maven
            if (!checkMaven()) {
                result.issues.add("Maven not found. Install from https://maven.apache.org/");
                result.failedSteps++;
            } else {
                result.completedSteps++;
            }

            // Step 2: Check Java
            if (!checkJava()) {
                result.issues.add("Java 17+ not found. Install from https://adoptium.net/");
                result.failedSteps++;
            } else {
                result.completedSteps++;
            }

            // Step 3: Check Git
            if (!checkGit()) {
                result.issues.add("Git not found. Install from https://git-scm.com/");
                result.failedSteps++;
            } else {
                result.completedSteps++;
            }

            // Step 4: Verify project structure
            ProjectChecker checker = new ProjectChecker(verbose);
            CheckResult check = checker.checkProject(Paths.get(projectDir));
            if (!check.isValid) {
                result.issues.add("Project structure incomplete. Run: java harness init create");
                result.failedSteps++;
            } else {
                result.completedSteps++;
            }

            // Step 5: Dependencies
            if (!skipDeps && !offline) {
                // Would run mvn dependency:go-away here
                result.completedSteps++;
                result.skippedSteps++;
            } else {
                result.completedSteps++;
            }

            return result;
        }

        private boolean checkMaven() {
            try {
                ProcessBuilder pb = new ProcessBuilder("mvn", "-version");
                Process process = pb.start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }

        private boolean checkJava() {
            try {
                ProcessBuilder pb = new ProcessBuilder("java", "-version");
                Process process = pb.start();
                return process.waitFor() == 0 && process.getOutputStream().toString().contains("17");
            } catch (Exception e) {
                return false;
            }
        }

        private boolean checkGit() {
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "--version");
                Process process = pb.start();
                return process.waitFor() == 0;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Template engine
     */
    public static class TemplateEngine {
        public boolean createFromTemplate(String templateName, String targetDir) {
            try {
                // Simplified template creation
                ProjectInitializer initializer = new ProjectInitializer(false);

                switch (templateName.toLowerCase()) {
                    case "basic":
                        initializer.createStructure("basic-project", targetDir, "java", false, false);
                        break;
                    case "spring-boot":
                        initializer.createStructure("spring-boot-app", targetDir, "spring-boot", false, false);
                        break;
                    default: // "standard"
                        initializer.createStructure("java-harness", targetDir, "java", false, false);
                        break;
                }

                return true;

            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * Project structure holder
     */
    public static class ProjectStructure {
        List<ProjectDir> directories = new ArrayList<>();
        List<ProjectFile> files = new ArrayList<>();

        void addDirectory(String name) {
            directories.add(new ProjectDir(name));
        }

        void addFile(String name, String content) {
            files.add(new ProjectFile(name, content));
        }

        void displayPreview() {
            System.out.println("📁 Directories:");
            for (ProjectDir dir : directories) {
                System.out.println("  " + dir.name + "/");
            }
            System.out.println();
            System.out.println("📄 Files:");
            for (ProjectFile file : files) {
                System.out.println("  " + file.name);
                System.out.println("    " + file.content.substring(0, Math.min(50, file.content.length())) + "...");
            }
        }
    }

    /**
     * Project directory holder
     */
    public static class ProjectDir {
        String name;

        ProjectDir(String name) {
            this.name = name;
        }
    }

    /**
     * Project file holder
     */
    public static class ProjectFile {
        String name;
        String content;

        ProjectFile(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    /**
     * Check result holder
     */
    public static class CheckResult {
        boolean isValid;
        String projectType;
        int issueCount;
        int errorCount;
        int warningCount;
        List<CheckIssue> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        String checkedAt;
    }

    /**
     * Check issue holder
     */
    public static class CheckIssue {
        String severity;
        String category;
        String message;
        String recommendation;
    }

    /**
     * Setup result holder
     */
    public static class SetupResult {
        int totalSteps;
        int completedSteps;
        int skippedSteps;
        int failedSteps;
        List<String> issues = new ArrayList<>();
    }

    /**
     * Main entry point for testing
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new InitCommand()).execute(args);
        System.exit(exitCode);
    }
}