package com.chachamaru.harness.foundation.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frontmatter 解析器 - 解析和管理文件头部的元数据
 *
 * <p>支持的格式：</p>
 * <ul>
 *   <li>YAML Frontmatter: --- yaml ---</li>
 *   <li>JSON Frontmatter: { "key": "value" }</li>
 *   <li>混合模式: Frontmatter + 内容</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class FrontmatterParser {

    private static final Logger logger = LoggerFactory.getLogger(FrontmatterParser.class);

    // YAML Frontmatter 模式: --- content ---
    private static final Pattern YAML_FRONTMATTER_PATTERN =
        Pattern.compile("^---\\s*$([\\s\\S]*?)^---\\s*$", Pattern.MULTILINE);

    // JSON Frontmatter 模式: { content }
    private static final Pattern JSON_FRONTMATTER_PATTERN =
        Pattern.compile("^\\{[\\s\\S]*\\}\\s*$");

    // 元数据字段模式
    private static final Pattern METADATA_FIELD_PATTERN =
        Pattern.compile("^_([a-zA-Z_]+):\\s*(.+)$", Pattern.MULTILINE);

    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     */
    public FrontmatterParser() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 解析文件内容，提取 Frontmatter 元数据
     *
     * @param content 文件内容
     * @return 解析结果（元数据 + 内容主体）
     */
    public FrontmatterResult parse(String content) {
        if (content == null || content.trim().isEmpty()) {
            return new FrontmatterResult(new FrontmatterMetadata(), "");
        }

        // 尝试解析 YAML Frontmatter
        Matcher yamlMatcher = YAML_FRONTMATTER_PATTERN.matcher(content.trim());
        if (yamlMatcher.find()) {
            String yamlContent = yamlMatcher.group(1);
            String bodyContent = content.substring(yamlMatcher.end());

            try {
                FrontmatterMetadata metadata = parseYamlMetadata(yamlContent);
                return new FrontmatterResult(metadata, bodyContent.trim());
            } catch (Exception e) {
                logger.warn("解析 YAML Frontmatter 失败: {}", e.getMessage());
                return new FrontmatterResult(new FrontmatterMetadata(), content);
            }
        }

        // 尝试解析 JSON Frontmatter
        if (JSON_FRONTMATTER_PATTERN.matcher(content.trim()).matches()) {
            try {
                FrontmatterMetadata metadata = parseJsonMetadata(content.trim());
                return new FrontmatterResult(metadata, "");
            } catch (Exception e) {
                logger.warn("解析 JSON Frontmatter 失败: {}", e.getMessage());
                return new FrontmatterResult(new FrontmatterMetadata(), content);
            }
        }

        // 没有 Frontmatter，返回原始内容
        return new FrontmatterResult(new FrontmatterMetadata(), content);
    }

    /**
     * 解析 YAML 元数据
     */
    private FrontmatterMetadata parseYamlMetadata(String yamlContent) {
        try {
            // 将 YAML 转换为 JSON（简化实现）
            Map<String, Object> yamlMap = parseSimpleYaml(yamlContent);

            // 手动处理 _harness_version，确保 Jackson 正确映射
            if (yamlMap.containsKey("_harness_version")) {
                // 确保键名正确
                Object version = yamlMap.get("_harness_version");
                if (version != null) {
                    yamlMap.put("_harness_version", version.toString());
                }
            }

            JsonNode jsonNode = objectMapper.valueToTree(yamlMap);
            return objectMapper.treeToValue(jsonNode, FrontmatterMetadata.class);
        } catch (Exception e) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_PARSE_ERROR,
                "YAML 解析失败: " + e.getMessage()
            );
        }
    }

    /**
     * 解析 JSON 元数据
     */
    private FrontmatterMetadata parseJsonMetadata(String jsonContent) {
        try {
            return objectMapper.readValue(jsonContent, FrontmatterMetadata.class);
        } catch (IOException e) {
            throw new TemplateRegistryException(
                TemplateRegistryException.ErrorCode.TEMPLATE_PARSE_ERROR,
                "JSON 解析失败: " + e.getMessage()
            );
        }
    }

    /**
     * 简单 YAML 解析器（仅支持基本格式）
     */
    private Map<String, Object> parseSimpleYaml(String yamlContent) {
        Map<String, Object> result = new HashMap<>();
        String[] lines = yamlContent.split("\\n");
        String currentKey = null;
        Map<String, Object> nestedMap = null;
        int nestedLevel = 0;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            // 计算缩进级别
            int indent = line.indexOf(trimmedLine);

            // 处理嵌套结构
            if (indent > 0 && currentKey != null) {
                if (nestedMap == null) {
                    nestedMap = new HashMap<>();
                    result.put(currentKey, nestedMap);
                }

                int colonIndex = trimmedLine.indexOf(':');
                if (colonIndex > 0) {
                    String nestedKey = trimmedLine.substring(0, colonIndex).trim();
                    String nestedValue = trimmedLine.substring(colonIndex + 1).trim();
                    nestedValue = parseYamlValue(nestedValue, nestedMap, nestedKey);
                }
                continue;
            } else {
                nestedMap = null;
                currentKey = null;
            }

            int colonIndex = trimmedLine.indexOf(':');
            if (colonIndex > 0) {
                currentKey = trimmedLine.substring(0, colonIndex).trim();
                String value = trimmedLine.substring(colonIndex + 1).trim();

                // 检查是否是嵌套结构的开始
                if (value.isEmpty()) {
                    continue; // 等待下一行的嵌套内容
                }

                value = parseYamlValue(value, result, currentKey);
            }
        }

        return result;
    }

    /**
     * 解析 YAML 值
     */
    private String parseYamlValue(String value, Map<String, Object> result, String key) {
        // 移除引号
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("'") && value.endsWith("'")) {
            value = value.substring(1, value.length() - 1);
        }

        // 处理布尔值
        if ("true".equalsIgnoreCase(value)) {
            result.put(key, true);
        } else if ("false".equalsIgnoreCase(value)) {
            result.put(key, false);
        }
        // 处理数字
        else if (value.matches("-?\\d+")) {
            result.put(key, Long.parseLong(value));
        } else if (value.matches("-?\\d+\\.\\d+")) {
            result.put(key, Double.parseDouble(value));
        }
        // 处理 null
        else if ("null".equalsIgnoreCase(value) || "~".equals(value)) {
            result.put(key, null);
        }
        // 字符串值
        else {
            result.put(key, value);
        }

        return value;
    }

    /**
     * 生成 Frontmatter 元数据
     *
     * @param metadata 元数据对象
     * @param body 内容主体
     * @return 完整的文件内容（Frontmatter + 内容）
     */
    public String generate(FrontmatterMetadata metadata, String body) {
        StringBuilder result = new StringBuilder();

        result.append("---\n");

        // 添加标准字段
        if (metadata.getHarnessVersion() != null) {
            result.append("_harness_version: ").append(metadata.getHarnessVersion()).append("\n");
        }

        if (metadata.getTemplateReference() != null) {
            FrontmatterMetadata.TemplateReference ref = metadata.getTemplateReference();
            result.append("_harness_template:\n");
            result.append("  name: ").append(ref.getName()).append("\n");
            result.append("  version: ").append(ref.getVersion()).append("\n");
            if (ref.getCategory() != null) {
                result.append("  category: ").append(ref.getCategory()).append("\n");
            }
        }

        if (metadata.getCreated() != null) {
            result.append("_created: ").append(metadata.getCreated()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        }

        if (metadata.getModified() != null) {
            result.append("_modified: ").append(metadata.getModified()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        }

        if (metadata.getAuthor() != null) {
            result.append("_author: ").append(metadata.getAuthor()).append("\n");
        }

        // 添加自定义元数据
        if (metadata.getCustomMetadata() != null) {
            for (Map.Entry<String, Object> entry : metadata.getCustomMetadata().entrySet()) {
                result.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        result.append("---\n");

        // 添加内容主体
        if (body != null && !body.trim().isEmpty()) {
            result.append("\n").append(body);
        }

        return result.toString();
    }

    /**
     * 验证版本兼容性
     *
     * @param requiredVersion 要求的版本
     * @param currentVersion 当前版本
     * @return 是否兼容
     */
    public boolean isVersionCompatible(String requiredVersion, String currentVersion) {
        if (requiredVersion == null || requiredVersion.isEmpty()) {
            return true;
        }

        if (currentVersion == null || currentVersion.isEmpty()) {
            return false;
        }

        try {
            // 简单版本比较（支持 semantic versioning）
            Version required = parseVersion(requiredVersion);
            Version current = parseVersion(currentVersion);

            return current.major >= required.major &&
                   (current.major > required.major || current.minor >= required.minor);
        } catch (Exception e) {
            logger.warn("版本比较失败: {} vs {}", requiredVersion, currentVersion);
            return false;
        }
    }

    /**
     * 解析版本号
     */
    private Version parseVersion(String versionString) {
        // 移除 'v' 前缀
        versionString = versionString.replaceAll("^v", "");

        String[] parts = versionString.split("\\.");
        Version version = new Version();

        if (parts.length > 0) {
            version.major = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
        }
        if (parts.length > 1) {
            version.minor = Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
        }
        if (parts.length > 2) {
            version.patch = Integer.parseInt(parts[2].replaceAll("[^0-9]", ""));
        }

        return version;
    }

    /**
     * 版本号内部表示
     */
    private static class Version {
        int major = 0;
        int minor = 0;
        int patch = 0;
    }

    /**
     * 更新元数据的时间戳
     *
     * @param metadata 元数据对象
     */
    public void updateTimestamps(FrontmatterMetadata metadata) {
        if (metadata.getCreated() == null) {
            metadata.setCreated(LocalDateTime.now());
        }
        metadata.updateModified();
    }

    /**
     * 合并元数据
     *
     * @param base 基础元数据
     * @param override 覆盖元数据
     * @return 合并后的元数据
     */
    public FrontmatterMetadata merge(FrontmatterMetadata base, FrontmatterMetadata override) {
        if (base == null && override == null) {
            return new FrontmatterMetadata();
        }
        if (base == null) {
            return override;
        }
        if (override == null) {
            return base;
        }

        FrontmatterMetadata merged = new FrontmatterMetadata();

        // 合并基本字段
        merged.setHarnessVersion(override.getHarnessVersion() != null ?
            override.getHarnessVersion() : base.getHarnessVersion());

        merged.setTemplateReference(override.getTemplateReference() != null ?
            override.getTemplateReference() : base.getTemplateReference());

        merged.setCreated(base.getCreated() != null ?
            base.getCreated() : override.getCreated());

        merged.setModified(override.getModified() != null ?
            override.getModified() : base.getModified());

        merged.setAuthor(override.getAuthor() != null ?
            override.getAuthor() : base.getAuthor());

        // 合并自定义元数据
        Map<String, Object> customMetadata = new HashMap<>();
        if (base.getCustomMetadata() != null) {
            customMetadata.putAll(base.getCustomMetadata());
        }
        if (override.getCustomMetadata() != null) {
            customMetadata.putAll(override.getCustomMetadata());
        }
        merged.setCustomMetadata(customMetadata);

        return merged;
    }

    /**
     * Frontmatter 解析结果
     */
    public static class FrontmatterResult {
        private final FrontmatterMetadata metadata;
        private final String content;

        public FrontmatterResult(FrontmatterMetadata metadata, String content) {
            this.metadata = metadata;
            this.content = content;
        }

        public FrontmatterMetadata getMetadata() {
            return metadata;
        }

        public String getContent() {
            return content;
        }

        public boolean hasFrontmatter() {
            return metadata.getHarnessVersion() != null ||
                   metadata.getTemplateReference() != null ||
                   !metadata.getCustomMetadata().isEmpty();
        }
    }
}