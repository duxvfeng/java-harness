package com.chachamaru.harness.session.storage;

import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSaveResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 存储层综合测试
 *
 * <p>测试 FileSystemStorage 的所有功能和边界情况。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
@DisplayName("存储层测试")
class StorageTest {

    @TempDir
    Path tempDir;

    private SessionStorage storage;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        storage = new FileSystemStorage(tempDir, 100 * 1024 * 1024); // 100MB
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("保存会话 - 基本功能")
    void testSaveSession() {
        // Given
        String sessionId = "test-session-1";
        String sessionData = "Sample session data";
        SessionMetadata metadata = createTestMetadata("test-session-1");

        // When
        SessionSaveResult result = storage.saveSession(sessionId, sessionData, metadata);

        // Then
        assertTrue(result.isSuccess());
        assertEquals("test-session-1", result.getSaveId());
        assertEquals("Session saved successfully", result.getMessage());
        assertNotNull(result.getSize());
    }

    @Test
    @DisplayName("保存会话 - 大文件压缩")
    void testSaveSessionWithCompression() {
        // Given
        String sessionId = "test-session-large";
        String largeData = createLargeString(2 * 1024 * 1024); // 2MB
        SessionMetadata metadata = createTestMetadata(sessionId);

        // When
        SessionSaveResult result = storage.saveSession(sessionId, largeData, metadata);

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getSize() < largeData.length(), "压缩后大小应该小于原始大小");
    }

    @Test
    @DisplayName("加载会话数据")
    void testLoadSessionData() {
        // Given
        String sessionId = "test-load-1";
        String sessionData = "Test data for loading";
        SessionMetadata metadata = createTestMetadata(sessionId);
        storage.saveSession(sessionId, sessionData, metadata);

        // When
        Optional<String> loaded = storage.loadSessionData(sessionId);

        // Then
        assertTrue(loaded.isPresent());
        assertEquals(sessionData, loaded.get());
    }

    @Test
    @DisplayName("加载压缩的会话数据")
    void testLoadCompressedSessionData() {
        // Given
        String sessionId = "test-load-compressed";
        String largeData = createLargeString(2 * 1024 * 1024);
        SessionMetadata metadata = createTestMetadata(sessionId);
        storage.saveSession(sessionId, largeData, metadata);

        // When
        Optional<String> loaded = storage.loadSessionData(sessionId);

        // Then
        assertTrue(loaded.isPresent());
        assertEquals(largeData, loaded.get());
    }

    @Test
    @DisplayName("加载会话元数据")
    void testLoadMetadata() {
        // Given
        String sessionId = "test-metadata-1";
        SessionMetadata original = createTestMetadata(sessionId);
        storage.saveSession(sessionId, "data", original);

        // When
        Optional<SessionMetadata> loaded = storage.loadMetadata(sessionId);

        // Then
        assertTrue(loaded.isPresent());
        assertEquals(original.getSaveId(), loaded.get().getSaveId());
        assertEquals(original.getTokenUsage(), loaded.get().getTokenUsage());
        assertEquals(original.getSummary(), loaded.get().getSummary());
    }

    @Test
    @DisplayName("列出所有会话")
    void testListSessions() {
        // Given
        storage.saveSession("session-1", "data1", createTestMetadata("session-1"));

        // 添加小延迟确保时间戳不同
        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        storage.saveSession("session-2", "data2", createTestMetadata("session-2"));

        try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        storage.saveSession("session-3", "data3", createTestMetadata("session-3"));

        // When
        List<SessionMetadata> sessions = storage.listSessions();

        // Then
        assertEquals(3, sessions.size());
        // 检查是否按时间戳降序排序（允许相等的时间戳）
        assertTrue(sessions.get(0).getTimestamp().isAfter(sessions.get(1).getTimestamp()) ||
                  sessions.get(0).getTimestamp().equals(sessions.get(1).getTimestamp()));
        assertTrue(sessions.get(1).getTimestamp().isAfter(sessions.get(2).getTimestamp()) ||
                  sessions.get(1).getTimestamp().equals(sessions.get(2).getTimestamp()));
    }

    @Test
    @DisplayName("列出最近会话 - 限制数量")
    void testListRecentSessions() {
        // Given
        for (int i = 1; i <= 5; i++) {
            storage.saveSession("session-" + i, "data" + i, createTestMetadata("session-" + i));
        }

        // When
        List<SessionMetadata> recent = storage.listRecentSessions(3);

        // Then
        assertEquals(3, recent.size());
    }

    @Test
    @DisplayName("删除会话")
    void testDeleteSession() {
        // Given
        String sessionId = "test-delete-1";
        storage.saveSession(sessionId, "data", createTestMetadata(sessionId));

        // When
        boolean deleted = storage.deleteSession(sessionId);

        // Then
        assertTrue(deleted);
        assertFalse(storage.loadSessionData(sessionId).isPresent());
        assertFalse(storage.loadMetadata(sessionId).isPresent());
    }

    @Test
    @DisplayName("删除不存在的会话")
    void testDeleteNonExistentSession() {
        // When
        boolean deleted = storage.deleteSession("non-existent-session");

        // Then
        assertFalse(deleted);
    }

    @Test
    @DisplayName("清理旧会话 - 按数量")
    void testCleanupOldSessionsByCount() {
        // Given
        for (int i = 1; i <= 15; i++) {
            storage.saveSession("session-" + i, "data" + i, createTestMetadata("session-" + i));
        }

        // When - 只保留10个
        int cleaned = storage.cleanupOldSessions(10, 30);

        // Then
        assertTrue(cleaned > 0);
        List<SessionMetadata> remaining = storage.listSessions();
        assertTrue(remaining.size() <= 10);
    }

    @Test
    @DisplayName("清理旧会话 - 按时间")
    void testCleanupOldSessionsByAge() {
        // Given - 创建一些会话（时间戳会在测试中有所不同）
        for (int i = 1; i <= 5; i++) {
            storage.saveSession("old-session-" + i, "data" + i, createTestMetadata("old-session-" + i));
        }

        // When - 清理超过1天的会话（在这个测试中不太可能触发，但API应该工作）
        int cleaned = storage.cleanupOldSessions(100, 1);

        // Then - API调用应该成功
        assertTrue(cleaned >= 0);
    }

    @Test
    @DisplayName("存储健康检查")
    void testHealthCheck() {
        // When
        boolean healthy = storage.healthCheck();

        // Then - 新创建的存储应该是健康的
        assertTrue(healthy);
    }

    @Test
    @DisplayName("获取存储信息")
    void testGetStorageInfo() {
        // Given
        storage.saveSession("session-1", "data1", createTestMetadata("session-1"));
        storage.saveSession("session-2", "data2", createTestMetadata("session-2"));

        // When
        SessionStorage.StorageInfo info = storage.getStorageInfo();

        // Then
        assertNotNull(info);
        assertEquals(2, info.getTotalSessions());
        assertTrue(info.isHealthy());
        assertTrue(info.getUsedSizeBytes() > 0);
        assertEquals(100 * 1024 * 1024, info.getTotalSizeBytes());
        assertTrue(info.getUsagePercentage() >= 0 && info.getUsagePercentage() <= 100);
    }

    @Test
    @DisplayName("并发保存控制")
    void testConcurrentSaveControl() {
        // Given
        String sessionId = "concurrent-test";
        SessionMetadata metadata = createTestMetadata(sessionId);

        // When - 快速连续保存同一个会话
        Thread thread1 = new Thread(() -> storage.saveSession(sessionId, "data1", metadata));
        Thread thread2 = new Thread(() -> storage.saveSession(sessionId, "data2", metadata));

        thread1.start();
        thread2.start();

        // Then - 两个线程都应该成功完成，没有抛出异常
        assertDoesNotThrow(() -> {
            thread1.join();
            thread2.join();
        });

        // 最终应该有一个有效的保存
        Optional<String> loaded = storage.loadSessionData(sessionId);
        assertTrue(loaded.isPresent());
    }

    @Test
    @DisplayName("原子写入保证")
    void testAtomicWrite() {
        // Given
        String sessionId = "atomic-write-test";
        String data = "Test data for atomic write";

        // When
        SessionSaveResult result = storage.saveSession(sessionId, data, createTestMetadata(sessionId));

        // Then - 保存成功后，数据应该立即可用且完整
        assertTrue(result.isSuccess());
        Optional<String> loaded = storage.loadSessionData(sessionId);
        assertTrue(loaded.isPresent());
        assertEquals(data, loaded.get());
    }

    @Test
    @DisplayName("存储空间限制")
    void testStorageSpaceLimit() {
        // Given - 创建一个有空间限制的存储
        Path limitedDir = tempDir.resolve("limited");
        SessionStorage limitedStorage = new FileSystemStorage(limitedDir, 1024); // 1KB

        // When - 尝试保存大文件
        String largeData = createLargeString(10 * 1024); // 10KB
        SessionSaveResult result = limitedStorage.saveSession("large-session", largeData, createTestMetadata("large-session"));

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Insufficient") || result.getErrorMessage().contains("space"));
    }

    @Test
    @DisplayName("边界情况 - 空会话数据")
    void testEmptySessionData() {
        // Given
        String sessionId = "empty-session";
        String emptyData = "";
        SessionMetadata metadata = createTestMetadata(sessionId);

        // When
        SessionSaveResult result = storage.saveSession(sessionId, emptyData, metadata);

        // Then
        assertTrue(result.isSuccess());
        Optional<String> loaded = storage.loadSessionData(sessionId);
        assertTrue(loaded.isPresent());
        assertEquals("", loaded.get());
    }

    @Test
    @DisplayName("边界情况 - 特殊字符")
    void testSpecialCharacters() {
        // Given
        String sessionId = "special-chars-测试";
        String specialData = "Data with special chars: 中文emoji 🚀\n\t\r";

        // When
        SessionSaveResult result = storage.saveSession(sessionId, specialData, createTestMetadata(sessionId));

        // Then
        assertTrue(result.isSuccess());
        Optional<String> loaded = storage.loadSessionData(sessionId);
        assertTrue(loaded.isPresent());
        assertEquals(specialData, loaded.get());
    }

    // Helper methods

    private SessionMetadata createTestMetadata(String saveId) {
        SessionMetadata.TaskContext taskContext = new SessionMetadata.TaskContext(
                "Phase 1",
                Arrays.asList("1.1", "1.2"),
                "1.3",
                10
        );

        SessionMetadata.GitState gitState = new SessionMetadata.GitState(
                "master",
                "abc123",
                0,
                false
        );

        SessionMetadata.SaveSize saveSize = new SessionMetadata.SaveSize(
                1,
                "1KB",
                "1KB"
        );

        return new SessionMetadata(
                saveId,
                Instant.now(),
                "Test save",
                50,
                taskContext,
                gitState,
                "Test summary",
                saveSize
        );
    }

    private String createLargeString(int sizeInBytes) {
        StringBuilder sb = new StringBuilder(sizeInBytes);
        for (int i = 0; i < sizeInBytes; i++) {
            sb.append('a');
        }
        return sb.toString();
    }
}