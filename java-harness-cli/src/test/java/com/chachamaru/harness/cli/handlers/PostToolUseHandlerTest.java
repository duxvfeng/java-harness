package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PostToolUse Handler Tests")
class PostToolUseHandlerTest {

    private final PostToolUseHandler handler = new PostToolUseHandler();

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() {
        // Clear snapshots between tests
        PostToolUseHandler.clearSnapshots();
    }

    @Test
    @DisplayName("应该允许读取操作")
    void shouldAllowReadOperation() throws IOException {
        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            tempDir.toString(),
            "default",
            "PostToolUse",
            "Read",
            Map.of("path", tempDir.resolve("test.txt").toString()),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("allow", output.permissionDecision());
    }

    @Test
    @DisplayName("应该检测文件篡改")
    void shouldDetectFileTampering() throws IOException {
        Path testFile = tempDir.resolve("test.txt");

        // Simulate first tool operation: Write creates file
        Files.writeString(testFile, "initial content");

        // First PostToolUse call - record initial state
        HookInput firstCall = new HookInput(
            "session-1",
            "/transcript",
            tempDir.toString(),
            "default",
            "PostToolUse",
            "Write",
            Map.of("file_path", testFile.toString()),
            "/plugin"
        );

        HookOutput firstOutput = handler.handle(firstCall);
        assertEquals("allow", firstOutput.permissionDecision());
        assertNull(firstOutput.additionalContext()); // No warning on first operation

        // Simulate external modification (external editor)
        try {
            Thread.sleep(10); // Ensure different modification time
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Files.writeString(testFile, "externally modified content");

        // Simulate second tool operation: should detect external change
        // Note: In real scenario, the tool would write different content,
        // but we're testing the detection mechanism
        HookInput secondCall = new HookInput(
            "session-1",
            "/transcript",
            tempDir.toString(),
            "default",
            "PostToolUse",
            "Write",
            Map.of("file_path", testFile.toString()),
            "/plugin"
        );

        HookOutput secondOutput = handler.handle(secondCall);

        assertEquals("allow", secondOutput.permissionDecision());
        assertNotNull(secondOutput.additionalContext());
        assertTrue(secondOutput.additionalContext().contains("Warning"));
        assertTrue(secondOutput.additionalContext().contains("modified externally"));
    }

    @Test
    @DisplayName("应该处理新文件而不报篡改")
    void shouldHandleNewFileWithoutTampering() throws IOException {
        Path testFile = tempDir.resolve("newfile.txt");

        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            tempDir.toString(),
            "default",
            "PostToolUse",
            "Write",
            Map.of("file_path", testFile.toString()),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("allow", output.permissionDecision());
        assertNull(output.additionalContext()); // No warning for new files
    }

    @Test
    @DisplayName("应该正确识别写入操作")
    void shouldIdentifyWriteOperations() {
        String[] writeTools = {"Write", "Edit", "NotebookEdit"};

        for (String tool : writeTools) {
            // This is tested indirectly through handle method
            assertDoesNotThrow(() -> {
                HookInput input = new HookInput(
                    "session-1",
                    "/transcript",
                    tempDir.toString(),
                    "default",
                    "PostToolUse",
                    tool,
                    Map.of("file_path", tempDir.resolve("test.txt").toString()),
                    "/plugin"
                );
                handler.handle(input);
            });
        }
    }

    @Test
    @DisplayName("应该获取正确的事件名称")
    void shouldGetCorrectEventName() {
        assertEquals("PostToolUse", handler.getEventName());
    }

    @Test
    @DisplayName("应该处理NotebookEdit操作")
    void shouldHandleNotebookEditOperation() throws IOException {
        Path notebookFile = tempDir.resolve("test.ipynb");

        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            tempDir.toString(),
            "default",
            "PostToolUse",
            "NotebookEdit",
            Map.of("notebook_path", notebookFile.toString()),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("allow", output.permissionDecision());
    }
}
