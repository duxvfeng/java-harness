package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import com.chachamaru.harness.session.restore.SessionRestoreManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simplified unit tests for SessionInitHandler
 */
@DisplayName("SessionInitHandler Tests")
class SessionInitHandlerTest {

    private SessionRestoreManager restoreManager;
    private SessionInitHandler handler;

    @BeforeEach
    void setUp() {
        // Create a real restore manager for testing (can be replaced with mock later)
        restoreManager = SessionRestoreManager.createDefault();
        handler = new SessionInitHandler(restoreManager);
    }

    @Test
    @DisplayName("应该返回正确的事件名称")
    void shouldReturnCorrectEventName() {
        assertEquals("SessionInit", handler.getEventName());
    }

    @Test
    @DisplayName("当没有恢复机会时应该允许session启动")
    void shouldAllowSessionStartWhenNoRestoreOpportunity() throws Exception {
        // Arrange
        HookInput input = createTestHookInput();

        // Act
        HookOutput output = handler.handle(input);

        // Assert
        assertEquals("allow", output.permissionDecision());
    }

    @Test
    @DisplayName("当功能禁用时应该跳过恢复检查")
    void shouldSkipRestoreCheckWhenDisabled() throws Exception {
        // Arrange
        SessionInitHandler.RestorePromptConfig disabledConfig =
                new SessionInitHandler.RestorePromptConfig(false, false, false, 2000);
        SessionInitHandler disabledHandler = new SessionInitHandler(restoreManager, disabledConfig);

        HookInput input = createTestHookInput();

        // Act
        HookOutput output = disabledHandler.handle(input);

        // Assert
        assertEquals("allow", output.permissionDecision());
    }

    @Test
    @DisplayName("生成的HookOutput应该包含必需字段")
    void shouldIncludeRequiredFieldsInHookOutput() throws Exception {
        // Arrange
        HookInput input = createTestHookInput();

        // Act
        HookOutput output = handler.handle(input);

        // Assert
        assertNotNull(output);
        assertEquals("allow", output.permissionDecision());
    }

    // Helper methods

    private HookInput createTestHookInput() {
        return new HookInput(
                "test-session-id",
                "/transcripts/test-session",
                "/Users/apple/IdeaProjects/java-harness",
                "bypassPermissions",
                "SessionInit",
                "test-tool",
                Map.of(),
                "/plugin/root"
        );
    }
}