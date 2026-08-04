package com.chachamaru.harness.workflow.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * plugin.json 生成器
 */
public class PluginGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 生成 .claude-plugin/plugin.json
     * 返回生成的文件路径
     */
    public static String generate(File projectRoot, SyncConfig config) throws IOException {
        SyncConfig.ProjectConfig project = config.getProject();

        Map<String, Object> pluginJson = new HashMap<>();

        if (project.getName() != null) {
            pluginJson.put("name", project.getName());
        }
        if (project.getVersion() != null) {
            pluginJson.put("version", project.getVersion());
        }
        if (project.getDescription() != null) {
            pluginJson.put("description", project.getDescription());
        }

        // Author: 保持 URL 或字符串格式
        if (project.getAuthorName() != null) {
            if (project.getAuthorUrl() != null) {
                Map<String, String> author = new HashMap<>();
                author.put("name", project.getAuthorName());
                author.put("url", project.getAuthorUrl());
                pluginJson.put("author", author);
            } else {
                pluginJson.put("author", project.getAuthorName());
            }
        }

        if (project.getHomepage() != null) {
            pluginJson.put("homepage", project.getHomepage());
        }
        if (project.getRepository() != null) {
            pluginJson.put("repository", project.getRepository());
        }
        if (project.getLicense() != null) {
            pluginJson.put("license", project.getLicense());
        }
        if (project.getKeywords() != null && !project.getKeywords().isEmpty()) {
            pluginJson.put("keywords", project.getKeywords());
        }

        // 关键字段：skills 目录（CC 2.1.94+ 发现 SKILL.md）
        List<String> skills = new ArrayList<>();
        skills.add("./skills/");
        pluginJson.put("skills", skills);

        if (project.getOutputStyles() != null && !project.getOutputStyles().isEmpty()) {
            pluginJson.put("outputStyles", project.getOutputStyles());
        }

        // 写入文件
        Path pluginDir = projectRoot.toPath().resolve(".claude-plugin");
        Files.createDirectories(pluginDir);

        Path outputPath = pluginDir.resolve("plugin.json");
        objectMapper.writeValue(outputPath.toFile(), pluginJson);

        return outputPath.toString();
    }
}
