package com.chachamaru.harness.tools.config;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI interface for generating plugin configuration files.
 *
 * <p>This tool generates the complete set of configuration files needed
 * to package Java Harness as a Claude Code plugin:
 * <ul>
 *   <li>plugin.json - Plugin metadata</li>
 *   <li>hooks.json - Hook event handlers configuration</li>
 *   <li>settings.json - Plugin settings</li>
 *   <li>marketplace.json - Marketplace metadata (optional)</li>
 * </ul>
 * </p>
 */
@Command(name = "generate-plugin-config",
         mixinStandardHelpOptions = true,
         description = "Generate Claude Code plugin configuration files",
         version = "4.0.0")
public class ConfigGeneratorCLI implements Runnable {

    @Option(names = {"-o", "--output"},
             description = "Output directory for generated files",
             defaultValue = ".claude-plugin/")
    private String outputDir;

    @Option(names = {"-p", "--project-root"},
             description = "Project root directory",
             defaultValue = ".")
    private String projectRoot;

    @Option(names = {"-n", "--name"},
             description = "Plugin name",
             defaultValue = "java-harness")
    private String pluginName;

    @Option(names = {"-v", "--version"},
             description = "Plugin version",
             defaultValue = "4.0.0")
    private String pluginVersion;

    @Option(names = {"--include-marketplace"},
             description = "Include marketplace.json generation")
    private boolean includeMarketplace;

    @Option(names = {"--force"},
             description = "Overwrite existing files")
    private boolean force;

    @Option(names = {"--dry-run"},
             description = "Show what would be generated without creating files")
    private boolean dryRun;

    @Option(names = {"--verbose"},
             description = "Show detailed output")
    private boolean verbose;

    @Override
    public void run() {
        try {
            System.out.println("🔧 Generating plugin configuration files");
            System.out.println("  Output: " + outputDir);
            System.out.println("  Project: " + projectRoot);
            System.out.println("  Plugin: " + pluginName + " v" + pluginVersion);

            Path outputPath = Paths.get(outputDir);

            if (dryRun) {
                System.out.println("  ⚠️  DRY RUN - No actual changes");
                performDryRun(outputPath);
            } else {
                generateConfigs(outputPath);
            }

            System.out.println();
            System.out.println("✓ Configuration generation complete!");
            System.out.println();
            displayNextSteps();

        } catch (Exception e) {
            System.err.println("✗ Configuration generation failed: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }

    private void generateConfigs(Path outputPath) throws IOException {
        ConfigSyncTool syncTool = new ConfigSyncTool(pluginName);

        try {
            // Generate settings.json
            Path settingsPath = syncTool.generateClaudeCodeSettings(outputPath);
            if (verbose) {
                System.out.println("✓ Generated: " + settingsPath);
            }

            // Generate plugin.json
            Path pluginPath = outputPath.resolve("plugin.json");
            writePluginJson(pluginPath);
            if (verbose) {
                System.out.println("✓ Generated: " + pluginPath);
            }

            // Generate hooks.json
            Path hooksPath = outputPath.resolve("hooks.json");
            writeHooksJson(hooksPath);
            if (verbose) {
                System.out.println("✓ Generated: " + hooksPath);
            }

            // Generate marketplace.json if requested
            if (includeMarketplace) {
                Path marketplacePath = outputPath.resolve("marketplace.json");
                writeMarketplaceJson(marketplacePath);
                if (verbose) {
                    System.out.println("✓ Generated: " + marketplacePath);
                }
            }
        } catch (ConfigSyncTool.ConfigSyncException e) {
            throw new IOException("Failed to generate configurations", e);
        }
    }

    private void performDryRun(Path outputPath) {
        System.out.println();
        System.out.println("📋 Files that would be generated:");

        String[] files = {"plugin.json", "hooks.json", "settings.json"};
        if (includeMarketplace) {
            files = new String[]{"plugin.json", "hooks.json", "settings.json", "marketplace.json"};
        }

        for (String file : files) {
            System.out.println("  " + outputPath.resolve(file));
        }
    }

    private void writePluginJson(Path path) throws IOException {
        String content = """
        {
          "name": "%s",
          "version": "%s",
          "description": "Java implementation of Claude Code Harness",
          "author": "chachamaru",
          "license": "MIT",
          "executable": {
            "command": "./harness",
            "args": ["hook", "process"]
          },
          "capabilities": [
            "hook.pre_tool_use",
            "hook.post_tool_use",
            "hook.permission_request",
            "guardrail.rules",
            "workflow.plans_parsing",
            "collaboration.skills",
            "collaboration.agents"
          ]
        }
        """.formatted(pluginName, pluginVersion);

        java.nio.file.Files.writeString(
            path,
            content,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void writeHooksJson(Path path) throws IOException {
        String content = """
        {
          "handlers": {
            "PreToolUse": {
              "command": "./harness",
              "args": ["hook", "pre-tool-use"],
              "timeout": 10000,
              "enabled": true
            },
            "PostToolUse": {
              "command": "./harness",
              "args": ["hook", "post-tool-use"],
              "timeout": 5000,
              "enabled": true
            },
            "PermissionRequest": {
              "command": "./harness",
              "args": ["hook", "permission-request"],
              "timeout": 15000,
              "enabled": true
            }
          }
        }
        """;

        java.nio.file.Files.writeString(
            path,
            content,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void writeMarketplaceJson(Path path) throws IOException {
        String content = """
        {
          "name": "%s",
          "version": "%s",
          "type": "marketplace",
          "displayName": "Java Harness",
          "description": "Java implementation of Claude Code Harness with Hook protocol and Guardrail support",
          "repository": "https://github.com/your-org/java-harness",
          "tags": ["java", "harness", "hook", "guardrail", "workflow"],
          "categories": ["development-tools", "code-quality"],
          "license": "MIT",
          "author": {
            "name": "chachamaru",
            "email": "your-email@example.com"
          },
          "homepage": "https://github.com/your-org/java-harness",
          "documentation": "https://github.com/your-org/java-harness/wiki",
          "bugs": "https://github.com/your-org/java-harness/issues",
          "downloads": {
            "windows": {
              "url": "https://github.com/your-org/java-harness/releases/download/v%s/harness-windows-amd64.exe",
              "checksum": "sha256:placeholder"
            },
            "linux": {
              "url": "https://github.com/your-org/java-harness/releases/download/v%s/harness-linux-amd64",
              "checksum": "sha256:placeholder"
            },
            "macos": {
              "url": "https://github.com/your-org/java-harness/releases/download/v%s/harness-darwin-amd64",
              "checksum": "sha256:placeholder"
            }
          }
        }
        """.formatted(pluginName, pluginVersion, pluginVersion, pluginVersion, pluginVersion);

        java.nio.file.Files.writeString(
            path,
            content,
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void displayNextSteps() {
        System.out.println("🎯 Next steps:");
        System.out.println("   1. Review the generated files");
        System.out.println("   2. Copy to plugin directory:");
        System.out.println("      mkdir -p ~/.claude/plugins/java-harness");
        System.out.println("      cp -r " + outputDir + "/* ~/.claude/plugins/java-harness/");
        System.out.println("   3. Reload plugins in Claude Code:");
        System.out.println("      /reload-plugins");
        System.out.println();
        System.out.println("📖 Documentation:");
        System.out.println("  docs/Claude插件打包指南.md");
    }

    /**
     * Main entry point for the configuration generator.
     */
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ConfigGeneratorCLI()).execute(args);
        System.exit(exitCode);
    }
}