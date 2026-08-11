package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ComplexityLevel 枚举的单元测试
 * 验证复杂度等级枚举的正确性和完整性
 */
@DisplayName("ComplexityLevel 枚举测试")
class ComplexityLevelTest {

    @Test
    @DisplayName("应该包含所有必需的复杂度等级")
    void shouldContainAllRequiredLevels() {
        assertNotNull(ComplexityLevel.SIMPLE, "SIMPLE 等级必须存在");
        assertNotNull(ComplexityLevel.MODERATE, "MODERATE 等级必须存在");
        assertNotNull(ComplexityLevel.COMPLEX, "COMPLEX 等级必须存在");
        assertNotNull(ComplexityLevel.VERY_COMPLEX, "VERY_COMPLEX 等级必须存在");
    }

    @Test
    @DisplayName("应该能够通过名称获取复杂度等级")
    void shouldGetLevelByName() {
        assertEquals(ComplexityLevel.SIMPLE, ComplexityLevel.valueOf("SIMPLE"));
        assertEquals(ComplexityLevel.MODERATE, ComplexityLevel.valueOf("MODERATE"));
        assertEquals(ComplexityLevel.COMPLEX, ComplexityLevel.valueOf("COMPLEX"));
        assertEquals(ComplexityLevel.VERY_COMPLEX, ComplexityLevel.valueOf("VERY_COMPLEX"));
    }

    @Test
    @DisplayName("复杂度等级数量应该正确")
    void shouldHaveCorrectLevelCount() {
        ComplexityLevel[] levels = ComplexityLevel.values();
        assertEquals(4, levels.length, "应该有4种复杂度等级");
    }

    @Test
    @DisplayName("复杂度等级应该有合理的顺序")
    void levelsShouldHaveLogicalOrder() {
        // 验证复杂度等级的逻辑顺序
        ComplexityLevel[] levels = ComplexityLevel.values();
        // 验证枚举声明的顺序符合预期
        assertEquals(ComplexityLevel.SIMPLE, levels[0]);
        assertEquals(ComplexityLevel.MODERATE, levels[1]);
        assertEquals(ComplexityLevel.COMPLEX, levels[2]);
        assertEquals(ComplexityLevel.VERY_COMPLEX, levels[3]);
    }
}