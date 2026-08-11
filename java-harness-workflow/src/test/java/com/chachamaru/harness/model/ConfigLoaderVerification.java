package com.chachamaru.harness.model;

/**
 * 简单的配置加载功能验证测试
 */
public class ConfigLoaderVerification {

    public static void main(String[] args) {
        System.out.println("=== ModelSelectionConfigLoader 功能验证 ===");

        // 测试 1: 创建加载器
        ModelSelectionConfigLoader loader = new ModelSelectionConfigLoader();
        System.out.println("✅ 配置加载器创建成功");

        // 测试 2: 加载默认配置
        ModelSelectionConfig config = loader.loadOrDefault();
        assert config != null : "配置不应为 null";
        System.out.println("✅ 默认配置加载成功");

        // 测试 3: 验证基本属性
        assert config.isEnabled() : "应该默认启用";
        assert "effortBased".equals(config.getStrategy()) : "策略应该是 effortBased";
        assert config.getTierConfigs().size() == 4 : "应该有 4 个等级配置";
        System.out.println("✅ 基本属性验证通过");

        // 测试 4: 验证所有等级都存在
        assert config.getTierConfig(ModelTier.FAST) != null : "FAST 等级不应为 null";
        assert config.getTierConfig(ModelTier.BALANCED) != null : "BALANCED 等级不应为 null";
        assert config.getTierConfig(ModelTier.QUALITY) != null : "QUALITY 等级不应为 null";
        assert config.getTierConfig(ModelTier.POWERFUL) != null : "POWERFUL 等级不应为 null";
        System.out.println("✅ 所有等级配置完整");

        // 测试 5: 验证降级链结构
        for (ModelTier tier : ModelTier.values()) {
            TierConfig tierConfig = config.getTierConfig(tier);
            String[] fallbackModels = tierConfig.getFallbackModels();
            assert fallbackModels.length >= 2 : "降级链至少应该有 2 个模型";
            assert fallbackModels[0].startsWith("env:") : "第一个模型应该是环境变量引用";
            System.out.println("✅ " + tier + " 等级降级链验证通过");
        }

        // 测试 6: 验证环境变量映射
        assert "ANTHROPIC_DEFAULT_FABLE_MODEL".equals(config.getTierConfig(ModelTier.FAST).getModelEnv());
        assert "ANTHROPIC_DEFAULT_HAIKU_MODEL".equals(config.getTierConfig(ModelTier.BALANCED).getModelEnv());
        assert "ANTHROPIC_DEFAULT_SONNET_MODEL".equals(config.getTierConfig(ModelTier.QUALITY).getModelEnv());
        assert "ANTHROPIC_DEFAULT_OPUS_MODEL".equals(config.getTierConfig(ModelTier.POWERFUL).getModelEnv());
        System.out.println("✅ 环境变量映射验证通过");

        System.out.println("\n🎉 所有验证通过！Task 12.14 实现成功！");
    }
}