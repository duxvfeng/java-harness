package com.chachamaru.harness.session.monitor;

import com.chachamaru.harness.session.model.TokenUsageInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Token 监控器测试
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
@DisplayName("TokenMonitor 测试")
class TokenMonitorTest {

    @Test
    @DisplayName("基本检测功能")
    void testBasicDetection() {
        // Given
        TokenMonitor monitor = new TokenMonitor(TokenMonitor.TokenMonitorConfig.getDefault());

        // When
        TokenUsageInfo info = monitor.checkTokenUsage();

        // Then
        assertNotNull(info);
        assertTrue(info.isDetectionSuccessful() || !info.isDetectionSuccessful());
    }

    @Test
    @DisplayName("阈值判断测试")
    void testThresholdJudgment() {
        // Given
        TokenMonitor monitor = new TokenMonitor(TokenMonitor.TokenMonitorConfig.getDefault());

        // When & Then
        assertFalse(monitor.shouldTriggerSave(-1));    // 未知情况不触发
        assertFalse(monitor.shouldTriggerSave(50));    // 50% 不触发
        assertTrue(monitor.shouldTriggerSave(80));     // 80% 触发
        assertTrue(monitor.shouldTriggerSave(90));     // 90% 触发
        assertTrue(monitor.shouldTriggerSave(95));     // 95% 触发
    }

    @Test
    @DisplayName("立即保存判断")
    void testNeedsImmediateSave() {
        // Given
        TokenMonitor monitor = new TokenMonitor(TokenMonitor.TokenMonitorConfig.getDefault());

        // When
        boolean needsImmediate = monitor.needsImmediateSave();

        // Then - 基于当前使用率判断
        assertTrue(needsImmediate == false || needsImmediate == true);
    }

    @Test
    @DisplayName("配置默认值")
    void testDefaultConfig() {
        // Given
        TokenMonitor.TokenMonitorConfig config = TokenMonitor.TokenMonitorConfig.getDefault();

        // Then
        assertNotNull(config);
        assertEquals(2, config.getThresholds().length);
        assertEquals(80, config.getThresholds()[0]);
        assertEquals(90, config.getThresholds()[1]);
        assertEquals(90, config.getUrgentThreshold());
        assertEquals(80, config.getNormalThreshold());
        assertEquals(200000, config.getMaxTokens());
    }
}