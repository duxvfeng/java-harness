package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ReviewRequirement 枚举的单元测试
 * 验证审查需求枚举的正确性和完整性
 */
@DisplayName("ReviewRequirement 枚举测试")
class ReviewRequirementTest {

    @Test
    @DisplayName("应该包含所有必需的审查需求")
    void shouldContainAllRequiredRequirements() {
        assertNotNull(ReviewRequirement.NONE, "NONE 需求必须存在");
        assertNotNull(ReviewRequirement.OPTIONAL, "OPTIONAL 需求必须存在");
        assertNotNull(ReviewRequirement.REQUIRED, "REQUIRED 需求必须存在");
    }

    @Test
    @DisplayName("应该能够通过名称获取审查需求")
    void shouldGetRequirementByName() {
        assertEquals(ReviewRequirement.NONE, ReviewRequirement.valueOf("NONE"));
        assertEquals(ReviewRequirement.OPTIONAL, ReviewRequirement.valueOf("OPTIONAL"));
        assertEquals(ReviewRequirement.REQUIRED, ReviewRequirement.valueOf("REQUIRED"));
    }

    @Test
    @DisplayName("审查需求数量应该正确")
    void shouldHaveCorrectRequirementCount() {
        ReviewRequirement[] requirements = ReviewRequirement.values();
        assertEquals(3, requirements.length, "应该有3种审查需求");
    }
}