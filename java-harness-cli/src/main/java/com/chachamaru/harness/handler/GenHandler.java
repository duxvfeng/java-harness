package com.chachamaru.harness.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Gen command handler.
 * Generates various content like prompts, templates, and documentation.
 */
public class GenHandler implements CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(GenHandler.class);

    @Override
    public void execute(String[] args) {
        try {
            String prompt = null;
            String outputFile = null;
            String template = "default";
            String projectRoot = ".";

            // Parse arguments
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--prompt") && i + 1 < args.length) {
                    prompt = args[++i];
                } else if (arg.equals("--output") && i + 1 < args.length) {
                    outputFile = args[++i];
                } else if (arg.equals("--template") && i + 1 < args.length) {
                    template = args[++i];
                } else if (!arg.startsWith("--")) {
                    projectRoot = arg;
                }
            }

            logger.info("Generating content with template: {}", template);

            // Generate content
            String content = generateContent(prompt, template, projectRoot);

            // Output content
            if (outputFile != null) {
                Files.writeString(Paths.get(outputFile), content);
                System.out.println("✓ Generated content written to: " + outputFile);
            } else {
                System.out.println(content);
            }

        } catch (Exception e) {
            logger.error("Generation failed", e);
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private String generateContent(String prompt, String template, String projectRoot) throws IOException {
        StringBuilder content = new StringBuilder();

        switch (template) {
            case "plan":
                content.append("# Plan Generation\n\n");
                if (prompt != null) {
                    content.append("## Prompt\n\n").append(prompt).append("\n\n");
                }
                content.append("## Task Analysis\n\n");
                content.append("Analyze the requirements and generate a structured plan.\n\n");
                content.append("## Implementation Steps\n\n");
                content.append("1. Research and analysis\n");
                content.append("2. Design approach\n");
                content.append("3. Implementation\n");
                content.append("4. Testing and validation\n");
                break;

            case "review":
                content.append("# Code Review Report\n\n");
                if (prompt != null) {
                    content.append("## Context\n\n").append(prompt).append("\n\n");
                }
                content.append("## Review Checklist\n\n");
                content.append("- [ ] Code quality and style\n");
                content.append("- [ ] Error handling\n");
                content.append("- [ ] Performance considerations\n");
                content.append("- [ ] Security implications\n");
                content.append("- [ ] Test coverage\n");
                content.append("- [ ] Documentation completeness\n");
                break;

            case "release":
                content.append("# Release Notes\n\n");
                content.append("## Version\n\n");
                content.append("## What's New\n\n");
                content.append("## Bug Fixes\n\n");
                content.append("## Breaking Changes\n\n");
                content.append("## Migration Guide\n\n");
                break;

            default:
                content.append("# Generated Content\n\n");
                if (prompt != null) {
                    content.append("## Prompt\n\n").append(prompt).append("\n\n");
                }
                content.append("## Content\n\n");
                content.append("Generated content based on the '").append(template).append("' template.\n\n");
                content.append("## Next Steps\n\n");
                content.append("1. Review and customize the generated content\n");
                content.append("2. Add specific details and requirements\n");
                content.append("3. Validate against project standards\n");
        }

        return content.toString();
    }
}
