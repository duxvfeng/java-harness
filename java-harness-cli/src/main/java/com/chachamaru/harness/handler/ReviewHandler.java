package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Review command handler.
 * Reviews completed work tasks.
 */
public class ReviewHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReviewHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            // Get working directory from system property or default to current directory
            String workDir = System.getProperty("java.harness.work.dir", ".");

            // Read Plans.md if it exists
            Path plansPath = Paths.get(workDir, "Plans.md");
            String plansContent = "";
            if (Files.exists(plansPath)) {
                plansContent = Files.readString(plansPath);
            }

            // Generate review prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("# Review Completed Work\n\n");

            if (args.length > 0) {
                String taskId = args[0];
                prompt.append("## Task ID: ").append(taskId).append("\n\n");

                // Try to extract the specific task from Plans.md
                if (!plansContent.isEmpty() && plansContent.contains(taskId)) {
                    String taskSection = extractTaskSection(plansContent, taskId);
                    if (!taskSection.isEmpty()) {
                        prompt.append("## Completed Work\n\n");
                        prompt.append(taskSection);
                        prompt.append("\n\n");
                    }
                }
            }

            if (!plansContent.isEmpty()) {
                prompt.append("## All Completed Tasks\n\n");
                prompt.append(plansContent);
                prompt.append("\n\n");
            }

            prompt.append("Please review the completed work according to the task specifications.\n");
            prompt.append("\nChecklist:\n");
            prompt.append("- [ ] Implementation matches specification\n");
            prompt.append("- [ ] Code quality standards met\n");
            prompt.append("- [ ] Tests pass and coverage adequate\n");
            prompt.append("- [ ] Documentation updated\n");
            prompt.append("- [ ] No regressions introduced\n");

            System.out.println(prompt.toString());

        } catch (IOException e) {
            logger.error("Error reading work files", e);
            System.err.println("Error reading work files: " + e.getMessage());
            System.exit(1);
        }
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
