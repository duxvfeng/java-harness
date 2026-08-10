package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ModelTier 枚举的单元测试
 * 测试模型等级映射、分数范围验证、环境变量映射等功能
 */
class ModelTierTest {

    @Test
    void testFastTierProperties() {
        assertEquals("FAST", ModelTier.FAST.name());
        assertEquals(0, ModelTier.FAST.getMinScore());
        assertEquals(2, ModelTier.FAST.getMaxScore());
        assertEquals("ANTHROPIC_DEFAULT_FABLE_MODEL", ModelTier.FAST.getEnvVariable());
    }

    @Test
    void testBalancedTierProperties() {
        assertEquals("BALANCED", ModelTier.BALANCED.name());
        assertEquals(3, ModelTier.BALANCED.getMinScore());
        assertEquals(4, ModelTier.BALANCED.getMaxScore());
        assertEquals("ANTHROPIC_DEFAULT_HAIKU_MODEL", ModelTier.BALANCED.getEnvVariable());
    }

    @Test
    void testQualityTierProperties() {
        assertEquals("QUALITY", ModelTier.QUALITY.name());
        assertEquals(5, ModelTier.QUALITY.getMinScore());
        assertEquals(6, ModelTier.QUALITY.getMaxScore());
        assertEquals("ANTHROPIC_DEFAULT_SONNET_MODEL", ModelTier.QUALITY.getEnvVariable());
    }

    @Test
    void testPowerfulTierProperties() {
        assertEquals("POWERFUL", ModelTier.POWERFUL.name());
        assertEquals(7, ModelTier.POWERFUL.getMinScore());
        assertEquals(999, ModelTier.POWERFUL.getMaxScore());
        assertEquals("ANTHROPIC_DEFAULT_OPUS_MODEL", ModelTier.POWERFUL.getEnvVariable());
    }

    @Test
    void testFromScoreLowComplexity() {
        // Test 0-2 分数范围应该返回 FAST
        assertEquals(ModelTier.FAST, ModelTier.fromScore(0));
        assertEquals(ModelTier.FAST, ModelTier.fromScore(1));
        assertEquals(ModelTier.FAST, ModelTier.fromScore(2));
    }

    @Test
    void testFromScoreMediumComplexity() {
        // Test 3-4 分数范围应该返回 BALANCED
        assertEquals(ModelTier.BALANCED, ModelTier.fromScore(3));
        assertEquals(ModelTier.BALANCED, ModelTier.fromScore(4));
    }

    @Test
    void testFromScoreHighComplexity() {
        // Test 5-6 分数范围应该返回 QUALITY
        assertEquals(ModelTier.QUALITY, ModelTier.fromScore(5));
        assertEquals(ModelTier.QUALITY, ModelTier.fromScore(6));
    }

    @Test
    void testFromScoreUltraHighComplexity() {
        // Test ≥7 分数范围应该返回 POWERFUL
        assertEquals(ModelTier.POWERFUL, ModelTier.fromScore(7));
        assertEquals(ModelTier.POWERFUL, ModelTier.fromScore(10));
        assertEquals(ModelTier.POWERFUL, ModelTier.fromScore(100));
        assertEquals(ModelTier.POWERFUL, ModelTier.fromScore(999));
    }

    @Test
    void testFromScoreBoundaryValues() {
        // 测试边界值
        assertEquals(ModelTier.FAST, ModelTier.fromScore(2));     // FAST 上界
        assertEquals(ModelTier.BALANCED, ModelTier.fromScore(3));  // BALANCED 下界
        assertEquals(ModelTier.BALANCED, ModelTier.fromScore(4));  // BALANCED 上界
        assertEquals(ModelTier.QUALITY, ModelTier.fromScore(5));   // QUALITY 下界
        assertEquals(ModelTier.QUALITY, ModelTier.fromScore(6));   // QUALITY 上界
        assertEquals(ModelTier.POWERFUL, ModelTier.fromScore(7));  // POWERFUL 下界
    }

    @Test
    void testNegativeScore() {
        // 负数分数应该默认使用 FAST
        assertEquals(ModelTier.FAST, ModelTier.fromScore(-1));
        assertEquals(ModelTier.FAST, ModelTier.fromScore(-100));
    }

    @Test
    void testContainsScore() {
        // 测试 containsScore 方法
        assertTrue(ModelTier.FAST.containsScore(0));
        assertTrue(ModelTier.FAST.containsScore(1));
        assertTrue(ModelTier.FAST.containsScore(2));
        assertFalse(ModelTier.FAST.containsScore(3));

        assertTrue(ModelTier.BALANCED.containsScore(3));
        assertTrue(ModelTier.BALANCED.containsScore(4));
        assertFalse(ModelTier.BALANCED.containsScore(5));

        assertTrue(ModelTier.QUALITY.containsScore(5));
        assertTrue(ModelTier.QUALITY.containsScore(6));
        assertFalse(ModelTier.QUALITY.containsScore(7));

        assertTrue(ModelTier.POWERFUL.containsScore(7));
        assertTrue(ModelTier.POWERFUL.containsScore(100));
        assertFalse(ModelTier.POWERFUL.containsScore(6));
    }

    @Test
    void testEnvVariableFormat() {
        // 验证环境变量名称格式正确
        assertTrue(ModelTier.FAST.getEnvVariable().startsWith("ANTHROPIC_DEFAULT_"));
        assertTrue(ModelTier.BALANCED.getEnvVariable().startsWith("ANTHROPIC_DEFAULT_"));
        assertTrue(ModelTier.QUALITY.getEnvVariable().startsWith("ANTHROPIC_DEFAULT_"));
        assertTrue(ModelTier.POWERFUL.getEnvVariable().startsWith("ANTHROPIC_DEFAULT_"));

        assertTrue(ModelTier.FAST.getEnvVariable().endsWith("_MODEL"));
        assertTrue(ModelTier.BALANCED.getEnvVariable().endsWith("_MODEL"));
        assertTrue(ModelTier.QUALITY.getEnvVariable().endsWith("_MODEL"));
        assertTrue(ModelTier.POWERFUL.getEnvVariable().endsWith("_MODEL"));
    }

    @Test
    void testAllTiersHaveNonOverlappingRanges() {
        // 验证所有等级的分数范围不重叠
        // FAST: 0-2
        // BALANCED: 3-4
        // QUALITY: 5-6
        // POWERFUL: 7-999

        for (int score = 0; score <= 1000; score++) {
            ModelTier tier = ModelTier.fromScore(score);

            if (score <= 2) {
                assertEquals(ModelTier.FAST, tier, "Score " + score + " should be FAST");
            } else if (score <= 4) {
                assertEquals(ModelTier.BALANCED, tier, "Score " + score + " should be BALANCED");
            } else if (score <= 6) {
                assertEquals(ModelTier.QUALITY, tier, "Score " + score + " should be QUALITY");
            } else {
                assertEquals(ModelTier.POWERFUL, tier, "Score " + score + " should be POWERFUL");
            }
        }
    }
}