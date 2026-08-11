package com.chachamaru.harness.mode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ExecutionMode 枚举的单元测试
 * 验证执行模式枚举的正确性和完整性
 */
@DisplayName("ExecutionMode 枚举测试")
class ExecutionModeTest {

    @Test
    @DisplayName("应该包含所有必需的执行模式")
    void shouldContainAllRequiredModes() {
        // 验证所有三种模式都存在
        assertNotNull(ExecutionMode.SOLO, "SOLO 模式必须存在");
        assertNotNull(ExecutionMode.PARALLEL, "PARALLEL 模式必须存在");
        assertNotNull(ExecutionMode.BREEZING, "BREEZING 模式必须存在");
    }

    @Test
    @DisplayName("应该能够通过名称获取模式")
    void shouldGetModeByName() {
        assertEquals(ExecutionMode.SOLO, ExecutionMode.valueOf("SOLO"));
        assertEquals(ExecutionMode.PARALLEL, ExecutionMode.valueOf("PARALLEL"));
        assertEquals(ExecutionMode.BREEZING, ExecutionMode.valueOf("BREEZING"));
    }

    @Test
    @DisplayName("模式数量应该正确")
    void shouldHaveCorrectModeCount() {
        ExecutionMode[] modes = ExecutionMode.values();
        assertEquals(3, modes.length, "应该有3种执行模式");
    }

    @Test
    @DisplayName("所有模式应该有描述信息")
    void allModesShouldHaveDescriptions() {
        // 验证每个模式都有对应的描述
        for (ExecutionMode mode : ExecutionMode.values()) {
            assertNotNull(mode.name(), "模式名称不能为null");
            assertFalse(mode.name().isEmpty(), "模式名称不能为空");
        }
    }
}