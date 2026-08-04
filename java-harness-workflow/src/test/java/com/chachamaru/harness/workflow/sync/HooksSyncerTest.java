package com.chachamaru.harness.workflow.sync;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HooksSyncer 测试")
class HooksSyncerTest {

    @Test
    @DisplayName("应该复制 hooks.json 到 .claude-plugin/hooks.json")
    void testSyncHooksJSON(@TempDir Path tempDir) throws Exception {
        // 创建源 hooks.json
        Path hooksDir = tempDir.resolve("hooks");
        Files.createDirectories(hooksDir);

        String hooksContent = "{\"description\":\"test hooks\",\"hooks\":{\"PreToolUse\":[]}}";
        Files.writeString(hooksDir.resolve("hooks.json"), hooksContent);

        // 执行同步
        String syncedPath = HooksSyncer.sync(tempDir.toFile());

        // 验证目标文件存在
        assertNotNull(syncedPath);
        assertTrue(Files.exists(Path.of(syncedPath)));

        // 验证内容一致
        String syncedContent = Files.readString(Path.of(syncedPath));
        assertEquals(hooksContent, syncedContent);
    }

    @Test
    @DisplayName("当源文件不存在时应该抛出异常")
    void testSyncHooksJSON_SourceNotExist(@TempDir Path tempDir) {
        // 源文件不存在，应该抛出异常
        assertThrows(Exception.class, () -> {
            HooksSyncer.sync(tempDir.toFile());
        });
    }

    @Test
    @DisplayName("当源文件不是有效 JSON 时应该抛出异常")
    void testSyncHooksJSON_InvalidJSON(@TempDir Path tempDir) throws Exception {
        // 创建源 hooks.json，但内容不是有效 JSON
        Path hooksDir = tempDir.resolve("hooks");
        Files.createDirectories(hooksDir);

        String invalidContent = "not a json";
        Files.writeString(hooksDir.resolve("hooks.json"), invalidContent);

        // 应该抛出异常
        assertThrows(Exception.class, () -> {
            HooksSyncer.sync(tempDir.toFile());
        });
    }

    @Test
    @DisplayName("应该创建 .claude-plugin 目录")
    void testSyncHooksJSON_CreatesTargetDirectory(@TempDir Path tempDir) throws Exception {
        // 创建源 hooks.json
        Path hooksDir = tempDir.resolve("hooks");
        Files.createDirectories(hooksDir);

        String hooksContent = "{\"description\":\"test hooks\"}";
        Files.writeString(hooksDir.resolve("hooks.json"), hooksContent);

        // 执行同步
        HooksSyncer.sync(tempDir.toFile());

        // 验证目标目录已创建
        Path targetDir = tempDir.resolve(".claude-plugin");
        assertTrue(Files.exists(targetDir));
        assertTrue(Files.isDirectory(targetDir));
    }
}
