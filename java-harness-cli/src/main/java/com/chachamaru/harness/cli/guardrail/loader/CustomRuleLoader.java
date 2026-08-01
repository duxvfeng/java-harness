package com.chachamaru.harness.cli.guardrail.loader;

import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.guardrail.config.CustomRuleConfig;
import com.chachamaru.harness.cli.guardrail.rules.DynamicRule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loader for custom guardrail rules from JSON configuration files
 */
public class CustomRuleLoader {
    private static final Logger log = LoggerFactory.getLogger(CustomRuleLoader.class);

    private static final String CUSTOM_RULES_DIR = ".claude/guardrules";
    private static final String CUSTOM_RULES_FILE = "custom-rules.json";

    private final ObjectMapper objectMapper;

    public CustomRuleLoader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Load custom rules from default locations
     */
    public List<Rule> loadCustomRules() {
        List<Rule> customRules = new ArrayList<>();

        // Load from .claude/guardrules/*.json
        customRules.addAll(loadFromDirectory());

        // Load from .claude/custom-rules.json
        customRules.addAll(loadFromMainConfig());

        log.info("Loaded {} custom rules", customRules.size());
        return customRules;
    }

    /**
     * Load rules from .claude/guardrules directory
     */
    private List<Rule> loadFromDirectory() {
        List<Rule> rules = new ArrayList<>();
        Path guardrulesPath = Paths.get(CUSTOM_RULES_DIR);

        if (!Files.exists(guardrulesPath)) {
            log.debug("Custom rules directory does not exist: {}", CUSTOM_RULES_DIR);
            return rules;
        }

        try {
            List<Path> configFiles = Files.walk(guardrulesPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".json"))
                .toList();

            for (Path configFile : configFiles) {
                try {
                    List<CustomRuleConfig> configs = objectMapper.readValue(
                        configFile.toFile(),
                        new TypeReference<List<CustomRuleConfig>>() {}
                    );
                    for (CustomRuleConfig config : configs) {
                        rules.add(new DynamicRule(config));
                    }
                    log.info("Loaded {} rules from {}", configs.size(), configFile);
                } catch (IOException e) {
                    log.error("Failed to load custom rules from {}: {}", configFile, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan custom rules directory: {}", e.getMessage());
        }

        return rules;
    }

    /**
     * Load rules from .claude/custom-rules.json
     */
    private List<Rule> loadFromMainConfig() {
        List<Rule> rules = new ArrayList<>();
        Path configPath = Paths.get(CUSTOM_RULES_FILE);

        if (!Files.exists(configPath)) {
            log.debug("Main custom rules config does not exist: {}", CUSTOM_RULES_FILE);
            return rules;
        }

        try {
            List<CustomRuleConfig> configs = objectMapper.readValue(
                configPath.toFile(),
                new TypeReference<List<CustomRuleConfig>>() {}
            );
            for (CustomRuleConfig config : configs) {
                rules.add(new DynamicRule(config));
            }
            log.info("Loaded {} rules from main config", configs.size());
        } catch (IOException e) {
            log.error("Failed to load custom rules from main config: {}", e.getMessage());
        }

        return rules;
    }

    /**
     * Validate custom rule configuration
     */
    public boolean validateRuleConfig(CustomRuleConfig config) {
        if (config.getId() == null || config.getId().trim().isEmpty()) {
            log.error("Rule ID is required");
            return false;
        }
        if (config.getName() == null || config.getName().trim().isEmpty()) {
            log.error("Rule name is required");
            return false;
        }
        if (config.getToolType() == null || config.getToolType().trim().isEmpty()) {
            log.error("Tool type is required");
            return false;
        }
        if (config.getConditions() == null || config.getConditions().isEmpty()) {
            log.error("At least one condition is required");
            return false;
        }
        if (config.getAction() == null) {
            log.error("Action is required");
            return false;
        }
        return true;
    }
}