package com.chachamaru.harness.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tomlj.Toml;
import org.tomlj.TomlTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 模型选择配置加载器
 * 支持从不同来源加载配置，按优先级顺序：环境变量 > settings.json > harness.toml > 默认配置
 */
public class ModelSelectionConfigLoader {

    private static final String SETTINGS_JSON_PATH = ".claude/settings.json";
    private static final String HARNESS_TOML_PATH = "harness.toml";
    private static final String MODEL_SELECTION_KEY = "modelSelection";

    /**
     * 加载配置或返回默认配置
     */
    public ModelSelectionConfig loadOrDefault() {
        try {
            ModelSelectionConfig config = load();
            if (config != null) {
                return config;
            }
        } catch (Exception e) {
            // 加载失败，使用默认配置
            System.err.println("Config loading failed, using default: " + e.getMessage());
        }
        return createDefaultConfig();
    }

    /**
     * 加载配置（按优先级顺序）
     * 优先级：环境变量 > settings.json > harness.toml > 默认配置
     */
    public ModelSelectionConfig load() {
        // 1. 尝试从 settings.json 加载
        ModelSelectionConfig config = loadFromSettingsJson();
        if (config != null) {
            return config;
        }

        // 2. 尝试从 harness.toml 加载
        config = loadFromHarnessToml();
        if (config != null) {
            return config;
        }

        // 3. 返回 null，触发使用默认配置
        return null;
    }

    /**
     * 从 .claude/settings.json 加载配置
     */
    private ModelSelectionConfig loadFromSettingsJson() {
        Path settingsPath = Paths.get(SETTINGS_JSON_PATH);
        if (!Files.exists(settingsPath)) {
            return null;
        }

        try {
            String content = Files.readString(settingsPath);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);

            if (!root.has(MODEL_SELECTION_KEY)) {
                return null;
            }

            JsonNode modelSelectionNode = root.get(MODEL_SELECTION_KEY);
            return parseJsonConfig(modelSelectionNode);

        } catch (IOException e) {
            System.err.println("Failed to load settings.json: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从 harness.toml 加载配置
     */
    private ModelSelectionConfig loadFromHarnessToml() {
        Path tomlPath = Paths.get(HARNESS_TOML_PATH);
        if (!Files.exists(tomlPath)) {
            return null;
        }

        try {
            String content = Files.readString(tomlPath);
            TomlTable toml = Toml.parse(content);

            if (!toml.contains("model_selection")) {
                return null;
            }

            return parseTomlConfig(toml);

        } catch (Exception e) {
            System.err.println("Failed to load harness.toml: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析 JSON 配置
     */
    private ModelSelectionConfig parseJsonConfig(JsonNode node) {
        boolean enabled = node.has("enabled") ? node.get("enabled").asBoolean() : true;
        String strategy = node.has("strategy") ? node.get("strategy").asText() : "effortBased";

        Map<ModelTier, TierConfig> tierConfigs = new HashMap<>();

        if (node.has("tierMapping")) {
            JsonNode tierMappingNode = node.get("tierMapping");
            tierConfigs.put(ModelTier.FAST, parseJsonTierConfig(tierMappingNode, "fast", ModelTier.FAST));
            tierConfigs.put(ModelTier.BALANCED, parseJsonTierConfig(tierMappingNode, "balanced", ModelTier.BALANCED));
            tierConfigs.put(ModelTier.QUALITY, parseJsonTierConfig(tierMappingNode, "quality", ModelTier.QUALITY));
            tierConfigs.put(ModelTier.POWERFUL, parseJsonTierConfig(tierMappingNode, "powerful", ModelTier.POWERFUL));
        } else {
            // 使用默认配置
            tierConfigs = createDefaultTierConfigs();
        }

        return new ModelSelectionConfig(enabled, strategy, tierConfigs);
    }

    /**
     * 解析 JSON 单个等级配置
     */
    private TierConfig parseJsonTierConfig(JsonNode parentNode, String key, ModelTier tier) {
        if (!parentNode.has(key)) {
            return createDefaultTierConfig(tier);
        }

        JsonNode tierNode = parentNode.get(key);
        String modelEnv = tierNode.has("modelEnv") ? tierNode.get("modelEnv").asText() : tier.getModelEnv();

        String[] fallbackModels;
        if (tierNode.has("fallbackModels") && tierNode.get("fallbackModels").isArray()) {
            JsonNode fallbackArray = tierNode.get("fallbackModels");
            fallbackModels = new String[fallbackArray.size()];
            for (int i = 0; i < fallbackArray.size(); i++) {
                fallbackModels[i] = fallbackArray.get(i).asText();
            }
        } else {
            fallbackModels = createDefaultFallbackModels(tier);
        }

        return new TierConfig(tier, modelEnv, fallbackModels);
    }

    /**
     * 解析 TOML 配置
     */
    private ModelSelectionConfig parseTomlConfig(TomlTable toml) {
        TomlTable modelSelection = toml.getTable("model_selection");
        if (modelSelection == null) {
            return createDefaultConfig();
        }

        boolean enabled = modelSelection.getBoolean("enable_smart_selection", () -> true);
        String strategy = modelSelection.getString("strategy", () -> "effort_based");

        Map<ModelTier, TierConfig> tierConfigs = new HashMap<>();

        if (modelSelection.contains("tiers")) {
            TomlTable tiers = modelSelection.getTable("tiers");
            if (tiers != null) {
                tierConfigs.put(ModelTier.FAST, parseTomlTierConfig(tiers, "fast", ModelTier.FAST));
                tierConfigs.put(ModelTier.BALANCED, parseTomlTierConfig(tiers, "balanced", ModelTier.BALANCED));
                tierConfigs.put(ModelTier.QUALITY, parseTomlTierConfig(tiers, "quality", ModelTier.QUALITY));
                tierConfigs.put(ModelTier.POWERFUL, parseTomlTierConfig(tiers, "powerful", ModelTier.POWERFUL));
            }
        } else {
            tierConfigs = createDefaultTierConfigs();
        }

        return new ModelSelectionConfig(enabled, strategy, tierConfigs);
    }

    /**
     * 解析 TOML 单个等级配置
     */
    private TierConfig parseTomlTierConfig(TomlTable tiersMap, String key, ModelTier tier) {
        if (!tiersMap.contains(key)) {
            return createDefaultTierConfig(tier);
        }

        TomlTable tierConfig = tiersMap.getTable(key);
        if (tierConfig == null) {
            return createDefaultTierConfig(tier);
        }

        String modelEnv = tierConfig.getString("model_env", () -> tier.getModelEnv());

        String[] fallbackModels;
        if (tierConfig.contains("fallback_models")) {
            Object fallbackObj = tierConfig.get("fallback_models");
            if (fallbackObj instanceof java.util.List) {
                java.util.List<String> fallbackList = (java.util.List<String>) fallbackObj;
                fallbackModels = fallbackList.toArray(new String[0]);
            } else {
                fallbackModels = createDefaultFallbackModels(tier);
            }
        } else {
            fallbackModels = createDefaultFallbackModels(tier);
        }

        return new TierConfig(tier, modelEnv, fallbackModels);
    }

    /**
     * 创建默认等级配置
     */
    private TierConfig createDefaultTierConfig(ModelTier tier) {
        return new TierConfig(tier, tier.getModelEnv(), createDefaultFallbackModels(tier));
    }

    /**
     * 创建默认降级模型列表
     */
    private String[] createDefaultFallbackModels(ModelTier tier) {
        return new String[]{
            "env:" + tier.getModelEnv(),
            "env:ANTHROPIC_MODEL",
            "glm-4.7"
        };
    }

    /**
     * 创建默认等级配置映射
     */
    private Map<ModelTier, TierConfig> createDefaultTierConfigs() {
        Map<ModelTier, TierConfig> configs = new HashMap<>();
        for (ModelTier tier : ModelTier.values()) {
            configs.put(tier, createDefaultTierConfig(tier));
        }
        return configs;
    }

    /**
     * 创建默认配置
     */
    private ModelSelectionConfig createDefaultConfig() {
        return new ModelSelectionConfig();
    }
}