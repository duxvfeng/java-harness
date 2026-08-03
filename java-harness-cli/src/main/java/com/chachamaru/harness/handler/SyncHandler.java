package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Sync command handler.
 * Synchronizes configuration and state with Plans.md.
 */
public class SyncHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(SyncHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            // Get root path from args or system property or default to current directory
            String rootDir = ".";
            if (args.length > 0) {
                rootDir = args[0];
            } else {
                rootDir = System.getProperty("java.harness.work.dir", ".");
            }

            // Read Plans.md if it exists
            Path plansPath = Paths.get(rootDir, "Plans.md");
            String plansContent = "";
            if (Files.exists(plansPath)) {
                plansContent = Files.readString(plansPath);
            }

            // Generate sync prompt
            StringBuilder prompt = new StringBuilder();
            prompt.append("# Configuration Sync\n\n");

            prompt.append("## Sync Status\n\n");
            prompt.append("Root directory: ").append(rootDir).append("\n\n");

            if (!plansContent.isEmpty()) {
                prompt.append("## Plans.md Content\n\n");
                prompt.append(plansContent);
                prompt.append("\n\n");
            }

            prompt.append("## Sync Operations\n\n");
            prompt.append("Please synchronize the following:\n\n");
            prompt.append("### Configuration Files\n");
            prompt.append("- [ ] harness.toml configuration\n");
            prompt.append("- [ ] hooks.json configuration\n");
            prompt.append("- [ ] Plugin metadata\n");
            prompt.append("- [ ] Skill definitions\n");
            prompt.append("\n");

            prompt.append("### State Files\n");
            prompt.append("- [ ] session.json state\n");
            prompt.append("- [ ] breezing-timeline.jsonl timeline\n");
            prompt.append("- [ ] Other state files\n");
            prompt.append("\n");

            prompt.append("### Plans.md\n");
            prompt.append("- [ ] Task status updated\n");
            prompt.append("- [ ] Completed tasks marked\n");
            prompt.append("- [ ] Dependencies checked\n");
            prompt.append("\n");

            prompt.append("Please proceed with the sync operations.\n");

            System.out.println(prompt.toString());

        } catch (IOException e) {
            logger.error("Error reading sync files", e);
            System.err.println("Error reading sync files: " + e.getMessage());
            System.exit(1);
        }
    }
}
