package com.chachamaru.harness.config;

import com.chachamaru.harness.foundation.template.TemplateResourceLoader;

/**
 * Configuration template generator for harness.toml.
 * Provides methods to generate default and custom configuration templates.
 */
public class ConfigTemplate {

    private static final String DEFAULT_TEMPLATE = TemplateResourceLoader.load(
        "templates/config/harness.toml.template");

    private static final String MINIMAL_TEMPLATE = TemplateResourceLoader.load(
        "templates/config/harness-minimal.toml.template");

    /**
     * Generate default configuration template.
     *
     * @return Default TOML configuration string
     */
    public static String generateDefault() {
        return DEFAULT_TEMPLATE;
    }

    /**
     * Generate custom configuration template with specified values.
     *
     * @param version Harness version
     * @param backend Backend type (codex, cursor, auto)
     * @param projectRoot Project root path
     * @return Custom TOML configuration string
     */
    public static String generateCustom(String version, String backend, String projectRoot) {
        return DEFAULT_TEMPLATE
                .replace("5.0.0-java", version)
                .replace("backend = \"codex\"", "backend = \"" + backend + "\"")
                .replace("project_root = \".\"", "project_root = \"" + projectRoot + "\"");
    }

    /**
     * Generate minimal configuration template.
     *
     * @return Minimal TOML configuration string
     */
    public static String generateMinimal() {
        return MINIMAL_TEMPLATE;
    }
}
