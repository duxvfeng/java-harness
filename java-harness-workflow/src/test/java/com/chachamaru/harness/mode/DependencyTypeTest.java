package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DependencyType 枚举的单元测试
 * 验证依赖类型枚举的正确性和完整性
 */
@DisplayName("DependencyType 枚举测试")
class DependencyTypeTest {

    @Test
    @DisplayName("应该包含所有必需的依赖类型")
    void shouldContainAllRequiredTypes() {
        assertNotNull(DependencyType.INDEPENDENT, "INDEPENDENT 类型必须存在");
        assertNotNull(DependencyType.SEQUENTIAL, "SEQUENTIAL 类型必须存在");
        assertNotNull(DependencyType.MIXED, "MIXED 类型必须存在");
    }

    @Test
    @DisplayName("应该能够通过名称获取依赖类型")
    void shouldGetTypeByName() {
        assertEquals(DependencyType.INDEPENDENT, DependencyType.valueOf("INDEPENDENT"));
        assertEquals(DependencyType.SEQUENTIAL, DependencyType.valueOf("SEQUENTIAL"));
        assertEquals(DependencyType.MIXED, DependencyType.valueOf("MIXED"));
    }

    @Test
    @DisplayName("依赖类型数量应该正确")
    void shouldHaveCorrectTypeCount() {
        DependencyType[] types = DependencyType.values();
        assertEquals(3, types.length, "应该有3种依赖类型");
    }
}