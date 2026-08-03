package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Release command handler.
 * Prepares for release or performs release checks.
 */
public class ReleaseHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReleaseHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            // Get working directory from system property or default to current directory
            String workDir = System.getProperty("java.harness.work.dir", ".");

            boolean checkMode = false;
            if (args.length > 0 && args[0].equals("--check")) {
                checkMode = true;
            }

            // Generate release prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("# Release Preparation\n\n");

            if (checkMode) {
                prompt.append("## Release Check Mode\n\n");
                prompt.append("Performing pre-release checks...\n\n");
                prompt.append("### Checklist:\n");
                prompt.append("- [ ] All tests pass\n");
                prompt.append("- [ ] Code coverage meets requirements\n");
                prompt.append("- [ ] Documentation updated\n");
                prompt.append("- [ ] CHANGELOG.md updated\n");
                prompt.append("- [ ] Version number updated\n");
                prompt.append("- [ ] No breaking changes without proper migration guide\n");
                prompt.append("- [ ] Dependencies are up to date and secure\n");
                prompt.append("- [ ] Performance benchmarks met\n");
            } else {
                prompt.append("## Release Mode\n\n");
                prompt.append("Preparing for release...\n\n");

                // Read Plans.md if it exists
                Path plansPath = Paths.get(workDir, "Plans.md");
                if (Files.exists(plansPath)) {
                    String plansContent = Files.readString(plansPath);
                    prompt.append("### Plans Status\n\n");
                    prompt.append(plansContent);
                    prompt.append("\n\n");
                }

                prompt.append("### Release Checklist:\n");
                prompt.append("- [ ] All planned tasks completed\n");
                prompt.append("- [ ] Code review approved\n");
                prompt.append("- [ ] Integration tests pass\n");
                prompt.append("- [ ] Documentation complete\n");
                prompt.append("- [ ] Release notes prepared\n");
                prompt.append("- [ ] Backup and rollback plan ready\n");
            }

            prompt.append("\nPlease proceed with the release preparation.\n");

            System.out.println(prompt.toString());

        } catch (IOException e) {
            logger.error("Error reading work files", e);
            System.err.println("Error reading work files: " + e.getMessage());
            System.exit(1);
        }
    }
}
