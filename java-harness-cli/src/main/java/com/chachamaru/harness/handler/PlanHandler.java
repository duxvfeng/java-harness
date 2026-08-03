package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Plan command handler.
 * Generates a plan prompt for the host to execute.
 */
public class PlanHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(PlanHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            // Get working directory from system property or default to current directory
            String workDir = System.getProperty("java.harness.work.dir", ".");

            // Read spec.md if it exists
            Path specPath = Paths.get(workDir, "spec.md");
            String specContent = "";
            if (Files.exists(specPath)) {
                specContent = Files.readString(specPath);
            }

            // Read Plans.md if it exists
            Path plansPath = Paths.get(workDir, "Plans.md");
            String plansContent = "";
            if (Files.exists(plansPath)) {
                plansContent = Files.readString(plansPath);
            }

            // Generate plan prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("# Plan Generation\n\n");

            if (!specContent.isEmpty()) {
                prompt.append("## Specification\n\n");
                prompt.append(specContent);
                prompt.append("\n\n");
            }

            if (!plansContent.isEmpty()) {
                prompt.append("## Existing Plans\n\n");
                prompt.append(plansContent);
                prompt.append("\n\n");
            }

            prompt.append("Please generate or update the plan based on the above specifications.\n");

            System.out.println(prompt.toString());

        } catch (IOException e) {
            logger.error("Error reading plan files", e);
            System.err.println("Error reading plan files: " + e.getMessage());
            System.exit(1);
        }
    }
}
