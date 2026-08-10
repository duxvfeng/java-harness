package com.chachamaru.harness.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 模型选择配置加载器
 * 支持从多个来源加载配置，按优先级处理，提供默认配置兜底
 *
 * <p>配置优先级：</p>
 * <ol>
 *   <li>.claude/settings.json (项目级别，最高优先级)</li>
 *   <li>harness.toml (项目配置)</li>
 *   <li>默认配置 (内置兜底)</li>
 * </ol>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
 *
 * // 按优先级加载配置
 * ModelSelectionConfig config = loader.loadOrDefault();
 *
 * // 获取默认配置
 * ModelSelectionConfig defaultConfig = loader.getDefaultConfig();
 *
 * // 从特定文件加载
 * Optional<ModelSelectionConfig> settingsConfig = loader.loadFromSettingsJson(".claude/settings.json");
 * Optional<ModelSelectionConfig> tomlConfig = loader.loadFromHarnessToml("harness.toml");
 * }</pre>
 */
public class ModelSelectionConfigLoader {

    private static final String DEFAULT_SETTINGS_PATH = ".claude/settings.json";
    private static final String DEFAULT_TOML_PATH = "harness.toml";

    private ModelSelectionConfig cachedDefaultConfig;

    /**
     * 按优先级加载配置或返回默认配置
     * 优先级：settings.json > harness.toml > 默认配置
     *
     * @return 配置对象
     */
    public ModelSelectionConfig loadOrDefault() {
        // 尝试按优先级加载
        Optional<ModelSelectionConfig> config = loadWithPriority();

        // 如果都失败了，返回默认配置
        return config.orElseGet(this::getDefaultConfig);
    }

    /**
     * 按优先级加载配置
     *
     * @return 配置的 Optional，如果都加载失败返回空
     */
    private Optional<ModelSelectionConfig> loadWithPriority() {
        // 1. 尝试加载 settings.json
        Optional<ModelSelectionConfig> settingsConfig = loadFromSettingsJson(DEFAULT_SETTINGS_PATH);
        if (settingsConfig.isPresent()) {
            return settingsConfig;
        }

        // 2. 尝试加载 harness.toml
        Optional<ModelSelectionConfig> tomlConfig = loadFromHarnessToml(DEFAULT_TOML_PATH);
        if (tomlConfig.isPresent()) {
            return tomlConfig;
        }

        // 3. 都失败，返回空
        return Optional.empty();
    }

    /**
     * 从 settings.json 加载配置
     *
     * @param path settings.json 文件路径
     * @return 配置的 Optional，如果加载失败返回空
     */
    public Optional<ModelSelectionConfig> loadFromSettingsJson(String path) {
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }

        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return Optional.empty();
            }

            // TODO: 实际实现需要解析 JSON
            // 当前返回空，表示未实现 JSON 解析
            return Optional.empty();

        } catch (Exception e) {
            // 任何异常都返回空，让调用者使用默认配置
            return Optional.empty();
        }
    }

    /**
     * 从 harness.toml 加载配置
     *
     * @param path harness.toml 文件路径
     * @return 配置的 Optional，如果加载失败返回空
     */
    public Optional<ModelSelectionConfig> loadFromHarnessToml(String path) {
        if (path == null || path.isEmpty()) {
            return Optional.empty();
        }

        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return Optional.empty();
            }

            // TODO: 实际实现需要解析 TOML
            // 当前返回空，表示未实现 TOML 解析
            return Optional.empty();

        } catch (Exception e) {
            // 任何异常都返回空，让调用者使用默认配置
            return Optional.empty();
        }
    }

    /**
     * 获取默认配置
     * 提供完整的默认配置，包含所有等级和合理的降级链
     *
     * @return 默认配置对象
     */
    public ModelSelectionConfig getDefaultConfig() {
        if (cachedDefaultConfig == null) {
            cachedDefaultConfig = createDefaultConfig();
        }
        return cachedDefaultConfig;
    }

    /**
     * 创建默认配置
     *
     * @return 新创建的默认配置对象
     */
    private ModelSelectionConfig createDefaultConfig() {
        Map<ModelTier, TierConfig> tierConfigs = new HashMap<>();

        // FAST 等级配置
        tierConfigs.put(ModelTier.FAST, new TierConfig(
            ModelTier.FAST,
            "ANTHROPIC_DEFAULT_FABLE_MODEL",
            new String[]{
                "env:ANTHROPIC_DEFAULT_FABLE_MODEL",
                "env:ANTHROPIC_MODEL",
                "glm-4.7"
            }
        ));

        // BALANCED 等级配置
        tierConfigs.put(ModelTier.BALANCED, new TierConfig(
            ModelTier.BALANCED,
            "ANTHROPIC_DEFAULT_HAIKU_MODEL",
            new String[]{
                "env:ANTHROPIC_DEFAULT_HAIKU_MODEL",
                "env:ANTHROPIC_MODEL",
                "glm-4.7"
            }
        ));

        // QUALITY 等级配置
        tierConfigs.put(ModelTier.QUALITY, new TierConfig(
            ModelTier.QUALITY,
            "ANTHROPIC_DEFAULT_SONNET_MODEL",
            new String[]{
                "env:ANTHROPIC_DEFAULT_SONNET_MODEL",
                "env:ANTHROPIC_MODEL",
                "glm-4.7"
            }
        ));

        // POWERFUL 等级配置
        tierConfigs.put(ModelTier.POWERFUL, new TierConfig(
            ModelTier.POWERFUL,
            "ANTHROPIC_DEFAULT_OPUS_MODEL",
            new String[]{
                "env:ANTHROPIC_DEFAULT_OPUS_MODEL",
                "env:ANTHROPIC_MODEL",
                "glm-4.7"
            }
        ));

        return new ModelSelectionConfig(
            true,                           // enabled
            "effortBased",                  // strategy
            tierConfigs,                    // tier configurations
            5000,                           // timeout: 5 seconds
            3,                              // max attempts
            false                           // validate API call (disabled by default)
        );
    }

    /**
     * 强制重新加载配置
     * 清除缓存，重新尝试加载
     *
     * @return 配置对象
     */
    public ModelSelectionConfig reload() {
        cachedDefaultConfig = null;
        return loadOrDefault();
    }

    /**
     * 检查配置文件是否存在
     *
     * @param path 配置文件路径
     * @return 如果文件存在返回 true
     */
    public boolean configExists(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        try {
            return Files.exists(Paths.get(path));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取配置文件路径列表（按优先级）
     *
     * @return 配置文件路径列表
     */
    public List<String> getConfigPaths() {
        return Arrays.asList(DEFAULT_SETTINGS_PATH, DEFAULT_TOML_PATH);
    }

    /**
     * 获取默认 settings.json 路径
     * @return 默认路径
     */
    public static String getDefaultSettingsPath() {
        return DEFAULT_SETTINGS_PATH;
    }

    /**
     * 获取默认 harness.toml 路径
     * @return 默认路径
     */
    public static String getDefaultTomlPath() {
        return DEFAULT_TOML_PATH;
    }
}