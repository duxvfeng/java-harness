package com.chachamaru.harness.skill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Skill executor for running harness skills.
 * Reads and executes skill files based on command routing.
 */
public class SkillExecutor {
    private static final Logger logger = LoggerFactory.getLogger(SkillExecutor.class);
    private static final String SKILLS_DIR = "skills";

    /**
     * Execute a skill by name.
     *
     * @param skillName Name of the skill to execute (e.g., "harness-plan")
     * @param args Command arguments
     * @return true if skill was found and executed, false otherwise
     */
    public static boolean executeSkill(String skillName, String[] args) {
        try {
            Path skillPath = Paths.get(SKILLS_DIR, skillName, "SKILL.md");

            if (!Files.exists(skillPath)) {
                logger.debug("Skill not found: {}", skillPath);
                return false;
            }

            logger.info("Executing skill: {}", skillName);

            String skillContent = Files.readString(skillPath);

            // For now, just display the skill information
            // In a full implementation, this would invoke Claude with the skill content
            System.out.println("# " + skillName + " Skill");
            System.out.println();
            System.out.println("This command invokes the " + skillName + " skill.");
            System.out.println();
            System.out.println("Skill location: " + skillPath);
            System.out.println();

            // Show trigger information from skill
            if (skillContent.contains("trigger:")) {
                int triggerStart = skillContent.indexOf("trigger:");
                int triggerEnd = skillContent.indexOf("\n", triggerStart);
                if (triggerEnd > triggerStart) {
                    String trigger = skillContent.substring(triggerStart + 8, triggerEnd).trim();
                    if (trigger.startsWith("\"") && trigger.endsWith("\"")) {
                        trigger = trigger.substring(1, trigger.length() - 1);
                    }
                    System.out.println("Trigger: " + trigger);
                }
            }

            System.out.println();
            System.out.println("Note: In the full implementation, this would:");
            System.out.println("1. Load the skill file content");
            System.out.println("2. Pass it to Claude for execution");
            System.out.println("3. Handle the response and update state");

            return true;

        } catch (IOException e) {
            logger.error("Failed to execute skill: {}", skillName, e);
            System.err.println("Error executing skill: " + e.getMessage());
            return false;
        }
    }

    /**
     * Map command name to skill name.
     *
     * @param command Command name (e.g., "plan")
     * @return Corresponding skill name (e.g., "harness-plan"), or null if not mapped
     */
    public static String mapCommandToSkill(String command) {
        // Core workflow commands
        Map<String, String> commandToSkill = new HashMap<>();
        commandToSkill.put("plan", "harness-plan");
        commandToSkill.put("work", "harness-work");
        commandToSkill.put("review", "harness-review");
        commandToSkill.put("release", "harness-release");
        commandToSkill.put("sync", "harness-sync");

        // Additional skills
        commandToSkill.put("breezing", "breezing");
        commandToSkill.put("accept", "harness-accept");
        commandToSkill.put("loop", "harness-loop");
        commandToSkill.put("progress", "harness-progress");

        return commandToSkill.get(command);
    }

    /**
     * Check if a command should be routed to a skill.
     *
     * @param command Command name
     * @return true if command should route to skill, false if it should use Handler
     */
    public static boolean shouldRouteToSkill(String command) {
        // These commands use Handler instead of skills
        Set<String> handlerCommands = Set.of(
                "init", "doctor", "validate", "status", "gen",
                "sprint-contract", "evidence", "completion",
                "version", "help", "hook"
        );

        return !handlerCommands.contains(command);
    }

    /**
     * Get all available skills.
     *
     * @return List of skill names
     */
    public static List<String> getAvailableSkills() {
        try {
            Path skillsDir = Paths.get(SKILLS_DIR);
            if (!Files.exists(skillsDir)) {
                return Collections.emptyList();
            }

            return Files.list(skillsDir)
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();

        } catch (IOException e) {
            logger.error("Failed to list skills", e);
            return Collections.emptyList();
        }
    }
}
