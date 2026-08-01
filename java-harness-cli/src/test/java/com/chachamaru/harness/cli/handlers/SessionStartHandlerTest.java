package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SessionStart Handler Tests")
class SessionStartHandlerTest {

    private final SessionStartHandler handler = new SessionStartHandler();

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("应该初始化会话")
    void shouldInitializeSession() throws IOException {
        String sessionId = "test-session-123";

        HookInput input = new HookInput(
            sessionId,
            "/transcript",
            tempDir.toString(),
            "default",
            "SessionStart",
            null,
            java.util.Map.of(),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("allow", output.permissionDecision());

        // Verify session directory was created
        Path sessionDir = tempDir.resolve(".claude").resolve("state").resolve(sessionId);
        assertTrue(Files.exists(sessionDir), "Session directory should be created");
        assertTrue(Files.isDirectory(sessionDir), "Session directory should be a directory");

        // Verify metadata file was created
        Path metadataFile = sessionDir.resolve("metadata.json");
        assertTrue(Files.exists(metadataFile), "Metadata file should be created");

        String metadata = Files.readString(metadataFile);
        assertTrue(metadata.contains("\"sessionId\":\"test-session-123\""), "Metadata should contain session ID");
        assertTrue(metadata.contains("\"cwd\""), "Metadata should contain cwd");
        assertTrue(metadata.contains("\"startTime\""), "Metadata should contain startTime");
    }

    @Test
    @DisplayName("应该生成Plans.md摘要")
    void shouldGeneratePlansSummary() throws IOException {
        // Create a test Plans.md file
        Path plansFile = tempDir.resolve("Plans.md");
        String plansContent = """
            # Phase 8: Hook System Implementation

            ## Phase 8.1: Hook System (4 weeks)

            | Task | Status |
            |------|--------|
            | 8.1.1 | cc:completed |
            | 8.1.2 | cc:TODO |
            | 8.1.3 | cc:TODO |

            ## Phase 8.2: CLI Commands

            | Task | Status |
            |------|--------|
            | 8.2.1 | cc:TODO |
            | 8.2.2 | cc:TODO |
            """;

        Files.writeString(plansFile, plansContent);

        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            tempDir.toString(),
            "default",
            "SessionStart",
            null,
            java.util.Map.of(),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("allow", output.permissionDecision());

        // Verify session was initialized (Plans.md was read)
        Path sessionDir = tempDir.resolve(".claude").resolve("state").resolve("session-1");
        assertTrue(Files.exists(sessionDir), "Session should be initialized");
    }

    @Test
    @DisplayName("应该在没有Plans.md时正常工作")
    void shouldWorkWithoutPlans() throws IOException {
        // Don't create Plans.md

        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            tempDir.toString(),
            "default",
            "SessionStart",
            null,
            java.util.Map.of(),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("allow", output.permissionDecision());

        // Session should still be initialized
        Path sessionDir = tempDir.resolve(".claude").resolve("state").resolve("session-1");
        assertTrue(Files.exists(sessionDir), "Session should be initialized even without Plans.md");
    }

    @Test
    @DisplayName("应该获取正确的事件名称")
    void shouldGetCorrectEventName() {
        assertEquals("SessionStart", handler.getEventName());
    }

    @Test
    @DisplayName("应该多次初始化同一会话")
    void shouldHandleMultipleInitializations() throws IOException {
        String sessionId = "multi-init-session";

        HookInput input = new HookInput(
            sessionId,
            "/transcript",
            tempDir.toString(),
            "default",
            "SessionStart",
            null,
            java.util.Map.of(),
            "/plugin"
        );

        // First initialization
        HookOutput output1 = handler.handle(input);
        assertEquals("allow", output1.permissionDecision());

        // Second initialization (should not fail)
        HookOutput output2 = handler.handle(input);
        assertEquals("allow", output2.permissionDecision());

        // Session directory should exist
        Path sessionDir = tempDir.resolve(".claude").resolve("state").resolve(sessionId);
        assertTrue(Files.exists(sessionDir));
    }

    @Test
    @DisplayName("应该处理不同的权限模式")
    void shouldHandleDifferentPermissionModes() throws IOException {
        String[] modes = {"default", "bypass", "auto"};

        for (String mode : modes) {
            String sessionId = "session-" + mode;

            HookInput input = new HookInput(
                sessionId,
                "/transcript",
                tempDir.toString(),
                mode,
                "SessionStart",
                null,
                java.util.Map.of(),
                "/plugin"
            );

            HookOutput output = handler.handle(input);
            assertEquals("allow", output.permissionDecision(),
                "Should work with " + mode + " permission mode");
        }
    }
}
