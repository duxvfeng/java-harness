package com.chachamaru.harness.cli.handlers;

import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.cli.hook.HookOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PermissionRequest Handler Tests")
class PermissionRequestHandlerTest {

    private final PermissionRequestHandler handler = new PermissionRequestHandler();

    @Test
    @DisplayName("应该自动批准安全工具")
    void shouldAutoApproveSafeTools() throws IOException {
        String[] safeTools = {"Read", "Glob", "Grep", "WebFetch", "WebSearch"};

        for (String tool : safeTools) {
            HookInput input = new HookInput(
                "session-1",
                "/transcript",
                "/project",
                "default",
                "PermissionRequest",
                tool,
                Map.of(),
                "/plugin"
            );

            HookOutput output = handler.handle(input);
            assertEquals("allow", output.permissionDecision(),
                tool + " should be auto-approved");
        }
    }

    @Test
    @DisplayName("应该拒绝危险操作")
    void shouldDenyDangerousOperations() throws IOException {
        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            "/project",
            "default",
            "PermissionRequest",
            "Bash",
            Map.of("command", "rm -rf /important/file"),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("deny", output.permissionDecision());
        assertNotNull(output.permissionDecisionReason());
        assertTrue(output.permissionDecisionReason().contains("Dangerous operation"));
    }

    @Test
    @DisplayName("应该批准写入安全文件模式")
    void shouldApproveWriteToSafePatterns() throws IOException {
        String[] safeFiles = {
            "/project/src/main/java/Example.java",
            "/project/README.md",
            "/project/docs/guide.txt",
            "/project/config.json",
            "/project/pom.xml"
        };

        for (String file : safeFiles) {
            HookInput input = new HookInput(
                "session-1",
                "/transcript",
                "/project",
                "default",
                "PermissionRequest",
                "Write",
                Map.of("file_path", file),
                "/plugin"
            );

            HookOutput output = handler.handle(input);
            assertEquals("allow", output.permissionDecision(),
                file + " should be auto-approved");
        }
    }

    @Test
    @DisplayName("应该延迟非安全文件写入")
    void shouldDeferUnsafeFileWrites() throws IOException {
        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            "/project",
            "default",
            "PermissionRequest",
            "Write",
            Map.of("file_path", "/etc/passwd"),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        assertEquals("defer", output.permissionDecision());
        assertNotNull(output.permissionDecisionReason());
        assertTrue(output.permissionDecisionReason().contains("Requires user approval"));
    }

    @Test
    @DisplayName("应该获取正确的事件名称")
    void shouldGetCorrectEventName() {
        assertEquals("PermissionRequest", handler.getEventName());
    }

    @Test
    @DisplayName("应该处理NotebookEdit操作")
    void shouldHandleNotebookEditOperation() throws IOException {
        HookInput input = new HookInput(
            "session-1",
            "/transcript",
            "/project",
            "default",
            "PermissionRequest",
            "NotebookEdit",
            Map.of("notebook_path", "/project/notebooks/test.ipynb"),
            "/plugin"
        );

        HookOutput output = handler.handle(input);

        // Notebook files not in safe patterns, so should defer
        assertEquals("defer", output.permissionDecision());
    }

    @Test
    @DisplayName("应该检测多种危险操作")
    void shouldDetectMultipleDangerousOperations() throws IOException {
        String[] dangerousCommands = {
            "rm -rf /tmp/*",
            "delete from users",
            "format c:",
            "destroy database",
            "purge logs"
        };

        for (String command : dangerousCommands) {
            HookInput input = new HookInput(
                "session-1",
                "/transcript",
                "/project",
                "default",
                "PermissionRequest",
                "Bash",
                Map.of("command", command),
                "/plugin"
            );

            HookOutput output = handler.handle(input);
            assertEquals("deny", output.permissionDecision(),
                command + " should be denied");
        }
    }
}
