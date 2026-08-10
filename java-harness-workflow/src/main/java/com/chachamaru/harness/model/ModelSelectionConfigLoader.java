package com.chachamaru.harness.model;

/**
 * 模型选择配置加载器
 * 支持从不同来源加载配置
 */
public class ModelSelectionConfigLoader {

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
        }
        return createDefaultConfig();
    }

    /**
     * 加载配置
     */
    public ModelSelectionConfig load() {
        // 目前返回 null，触发使用默认配置
        // 未来可以扩展为从文件加载
        return null;
    }

    /**
     * 创建默认配置
     */
    private ModelSelectionConfig createDefaultConfig() {
        return new ModelSelectionConfig();
    }
}