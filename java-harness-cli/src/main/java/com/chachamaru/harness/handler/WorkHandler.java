package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Work command handler with E2E detection fix loop integration (placeholder).
 *
 * Executes work tasks and handles E2E detection failures by returning to fix mode.
 *
 * @since 2.2.0 - Added E2E detection fix loop integration (stub implementation)
 */
public class WorkHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(WorkHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            // Get working directory from system property or default to current directory
            String workDir = System.getProperty("java.harness.work.dir", ".");
            Path projectRoot = Paths.get(workDir);

            // Check if this is a fix loop triggered by E2E detection failure
            if (isE2EFixMode(args)) {
                handleE2EFixMode(projectRoot, workDir, args);
                return;
            }

            // Read Plans.md if it exists
            Path plansPath = Paths.get(workDir, "Plans.md");
            String plansContent = "";
            if (Files.exists(plansPath)) {
                plansContent = Files.readString(plansPath);
            }

            // Generate work prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("# Work Execution\n\n");

            if (args.length > 0) {
                String taskId = args[0];
                prompt.append("## Task ID: ").append(taskId).append("\n\n");

                // Try to extract the specific task from Plans.md
                if (!plansContent.isEmpty() && plansContent.contains(taskId)) {
                    String taskSection = extractTaskSection(plansContent, taskId);
                    if (!taskSection.isEmpty()) {
                        prompt.append(taskSection);
                        prompt.append("\n\n");
                    }
                }
            }

            if (!plansContent.isEmpty()) {
                prompt.append("## All Tasks\n\n");
                prompt.append(plansContent);
                prompt.append("\n\n");
            }

            prompt.append("Please execute the work according to the task specifications.\n");

            // Add E2E detection context
            prompt.append("\n⚠️ **E2E Detection Integration**: ");
            prompt.append("After completion, work will go through review and E2E detection. ");
            prompt.append("If E2E detection fails, you'll be notified to fix specific issues.\n");
            prompt.append("\n🔧 **Current Status**: Full E2E detection Java implementation in progress...");

            System.out.println(prompt.toString());

        } catch (IOException e) {
            logger.error("Error reading work files", e);
            System.err.println("Error reading work files: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Check if this execution is in E2E fix mode
     */
    private boolean isE2EFixMode(String[] args) {
        for (String arg : args) {
            if ("--e2e-fix".equals(arg) || "--fix-e2e".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle E2E detection fix mode (stub implementation)
     */
    private void handleE2EFixMode(Path projectRoot, String workDir, String[] args) {
        logger.info("Entering E2E detection fix mode");

        try {
            // Extract detection ID from args
            String detectionId = extractDetectionId(args);
            if (detectionId == null) {
                System.err.println("Error: Detection ID not provided for fix mode");
                System.err.println("Usage: --e2e-fix <detection-id>");
                System.exit(1);
                return;
            }

            System.out.println("\n🔧 **E2E Detection Fix Mode**");
            System.out.println("Detection ID: " + detectionId);
            System.out.println("Purpose: Fix issues found during E2E detection\n");

            // Generate fix prompt based on detection results
            StringBuilder fixPrompt = new StringBuilder();
            fixPrompt.append("# E2E Detection Issues - Fix Required\n\n");
            fixPrompt.append("## Detection ID: ").append(detectionId).append("\n\n");
            fixPrompt.append("The following issues were found during E2E detection:\n\n");

            // In full implementation, this would load actual detection results
            fixPrompt.append("## Issues to Fix:\n");
            fixPrompt.append("- Review the specific E2E detection failure details\n");
            fixPrompt.append("- Address frontend/backend integration issues\n");
            fixPrompt.append("- Fix test failures or functionality gaps\n");
            fixPrompt.append("- Ensure all test scenarios pass\n\n");

            fixPrompt.append("## Fix Guidelines:\n");
            fixPrompt.append("1. Focus on the specific issues identified\n");
            fixPrompt.append("2. Test locally before committing\n");
            fixPrompt.append("3. Follow the project's coding standards\n");
            fixPrompt.append("4. Update relevant documentation\n");
            fixPrompt.append("5. Commit changes with clear fix messages\n\n");

            fixPrompt.append("## Next Steps:\n");
            fixPrompt.append("After fixes:\n");
            fixPrompt.append("1. Commit your changes\n");
            fixPrompt.append("2. E2E detection will automatically re-run\n");
            fixPrompt.append("3. If passes, work proceeds to completion\n");
            fixPrompt.append("4. If still fails, another fix cycle will begin\n");

            System.out.println(fixPrompt.toString());

            System.out.println("\n✅ Ready to proceed with fixes. Use standard git workflow to make changes.");
            System.out.println("\n🔧 **Note**: Full automatic fix loop integration will be available after Java implementation completes.");

        } catch (Exception e) {
            logger.error("Error in E2E fix mode", e);
            System.err.println("Error in E2E fix mode: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Extract detection ID from args
     */
    private String extractDetectionId(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--e2e-fix".equals(args[i]) || "--fix-e2e".equals(args[i])) {
                if (i + 1 < args.length) {
                    return args[i + 1];
                }
            }
        }
        return null;
    }

    /**
     * Extract a specific task section from Plans.md content.
     */
    private String extractTaskSection(String plansContent, String taskId) {
        String[] lines = plansContent.split("\n");
        StringBuilder section = new StringBuilder();
        boolean inTaskSection = false;

        for (String line : lines) {
            if (line.contains("## ") && line.contains(taskId)) {
                inTaskSection = true;
            }

            if (inTaskSection) {
                section.append(line).append("\n");

                // Stop at next task section
                if (line.startsWith("## ") && !line.contains(taskId) && section.length() > 0) {
                    // Remove the last line (next section header)
                    int lastNewline = section.lastIndexOf("\n");
                    if (lastNewline > 0) {
                        section.setLength(lastNewline);
                    }
                    break;
                }
            }
        }

        return section.toString();
    }
}
