package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentStatus 测试")
public class AgentStatusTest {

    @Test
    @DisplayName("应该有所有必需的状态")
    public void testAllStatuses() {
        assertEquals(7, AgentStatus.values().length);
        assertEquals("PENDING", AgentStatus.PENDING.name());
        assertEquals("RUNNING", AgentStatus.RUNNING.name());
        assertEquals("SUCCESS", AgentStatus.SUCCESS.name());
        assertEquals("FAILED", AgentStatus.FAILED.name());
        assertEquals("SUCCESS_WITH_WARNINGS", AgentStatus.SUCCESS_WITH_WARNINGS.name());
        assertEquals("PARTIAL_SUCCESS", AgentStatus.PARTIAL_SUCCESS.name());
        assertEquals("CANCELLED", AgentStatus.CANCELLED.name());
    }

    @Test
    @DisplayName("应该判断成功状态")
    public void testIsSuccess() {
        assertTrue(AgentStatus.SUCCESS.isSuccess());
        assertFalse(AgentStatus.FAILED.isSuccess());
        assertFalse(AgentStatus.PARTIAL_SUCCESS.isSuccess());
    }

    @Test
    @DisplayName("应该判断失败状态")
    public void testIsFailed() {
        assertTrue(AgentStatus.FAILED.isFailed());
        assertFalse(AgentStatus.SUCCESS.isFailed());
        assertFalse(AgentStatus.PARTIAL_SUCCESS.isFailed());
    }

    @Test
    @DisplayName("应该判断部分成功状态")
    public void testIsPartialSuccess() {
        assertTrue(AgentStatus.PARTIAL_SUCCESS.isPartialSuccess());
        assertTrue(AgentStatus.SUCCESS_WITH_WARNINGS.isPartialSuccess());
        assertFalse(AgentStatus.SUCCESS.isPartialSuccess());
    }
}
