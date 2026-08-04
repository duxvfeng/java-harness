package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentConfig 测试")
public class AgentConfigTest {

    @Test
    @DisplayName("应该创建默认配置")
    public void testDefaultConfig() {
        AgentConfig config = AgentConfig.defaultConfig();
        assertNotNull(config);
        assertFalse(config.isParallelExecutionEnabled());
        assertEquals(300, config.getTimeoutSeconds());
    }

    @Test
    @DisplayName("应该创建自定义配置")
    public void testCustomConfig() {
        AgentConfig config = AgentConfig.builder()
                .parallelExecutionEnabled(true)
                .timeoutSeconds(600)
                .build();

        assertTrue(config.isParallelExecutionEnabled());
        assertEquals(600, config.getTimeoutSeconds());
    }

    @Test
    @DisplayName("应该支持 Builder 模式")
    public void testBuilderPattern() {
        AgentConfig config = AgentConfig.builder()
                .parallelExecutionEnabled(false)
                .timeoutSeconds(120)
                .maxRetries(3)
                .build();

        assertEquals(120, config.getTimeoutSeconds());
        assertEquals(3, config.getMaxRetries());
    }
}
