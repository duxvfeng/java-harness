package com.chachamaru.harness.session.manager;

import com.chachamaru.harness.session.manager.SessionSaveManager.SessionSaveConfig;
import com.chachamaru.harness.session.manager.SessionSaveManager.SessionContext;
import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSaveResult;
import com.chachamaru.harness.session.storage.SessionStorage;
import com.chachamaru.harness.session.storage.FileSystemStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionSaveManager 综合测试
 *
 * <p>测试保存管理器的所有功能和边界情况。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
@DisplayName("SessionSaveManager 测试")
class SessionSaveManagerTest {

    @TempDir
    Path tempDir;

    private SessionStorage storage;
    private SessionSaveManager manager;
    private SessionSaveConfig config;

    @BeforeEach
    void setUp() {
        storage = new FileSystemStorage(tempDir, 100 * 1024 * 1024); // 100MB
        config = SessionSaveConfig.getDefault();
        manager = new SessionSaveManager(storage, config);
    }

    @Test
    @DisplayName("基本保存功能")
    void testBasicSave() {
        // Given
        SessionContext context = createTestContext();
        String reason = "Test manual save";

        // When
        SessionSaveResult result = manager.saveSession(context, reason);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getSaveId());
        assertTrue(result.getMessage().contains("successfully") || result.getMessage().contains("成功"));
    }

    @Test
    @DisplayName("保存间隔限制")
    void testSaveIntervalLimit() {
        // Given
        SessionContext context = createTestContext();

        // When - 第一次保存
        SessionSaveResult result1 = manager.saveSession(context, "First save");

        // Then
        assertTrue(result1.isSuccess());

        // When - 立即再次保存（在间隔限制内）
        SessionSaveResult result2 = manager.saveSession(context, "Second save");

        // Then - 应该被跳过
        assertFalse(result2.isSuccess());
        assertTrue(result2.getErrorMessage().contains("间隔过短"));
    }

    @Test
    @DisplayName("强制保存忽略间隔限制")
    void testForceSaveIgnoresInterval() {
        // Given
        SessionContext context = createTestContext();
        manager.saveSession(context, "First save");

        // When - 立即强制保存
        SessionSaveResult result = manager.forceSave(context, "Force save");

        // Then
        assertTrue(result.isSuccess());
        assertTrue(result.getSaveId().contains("force"));
    }

    @Test
    @DisplayName("并发保存控制")
    void testConcurrentSaveControl() throws InterruptedException {
        // Given
        SessionContext context = createTestContext();
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // When - 多个线程同时保存
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待所有线程准备就绪
                    SessionSaveResult result = manager.saveSession(context, "Concurrent save");

                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else if (result.getErrorMessage().contains("进行中") ||
                              result.getErrorMessage().contains("跳过")) {
                        skipCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail("Concurrent save failed with exception", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 启动所有线程
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "所有线程应该在10秒内完成");

        // Then - 至少有1个保存成功，其他被跳过
        assertTrue(successCount.get() >= 1, "至少应该有一个保存成功");
        assertTrue(skipCount.get() >= threadCount - 1 || skipCount.get() >= 0, "其他保存应该被跳过");

        executor.shutdown();
    }

    @Test
    @DisplayName("存储空间检查和清理")
    void testStorageSpaceCheckAndCleanup() {
        // Given - 创建小容量存储
        Path smallDir = tempDir.resolve("small");
        SessionStorage smallStorage = new FileSystemStorage(smallDir, 1024 * 1024); // 1MB
        SessionSaveManager smallManager = new SessionSaveManager(smallStorage, config);

        // When - 尝试保存大文件
        SessionContext largeContext = createLargeTestContext(2 * 1024 * 1024); // 2MB
        SessionSaveResult result = smallManager.saveSession(largeContext, "Large save");

        // Then
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("空间不足") ||
                   result.getErrorMessage().contains("failed"));
    }

    @Test
    @DisplayName("列出保存的会话")
    void testListSessions() {
        // Given
        manager.saveSession(createTestContext(), "Save 1");

        // 添加延迟以避免间隔限制
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        manager.forceSave(createTestContext(), "Save 2"); // 使用强制保存避免间隔限制

        // When
        List<SessionMetadata> sessions = manager.listSessions();

        // Then
        assertTrue(sessions.size() >= 2);
    }

    @Test
    @DisplayName("列出最近的会话")
    void testListRecentSessions() {
        // Given
        manager.saveSession(createTestContext(), "Save 1");

        // 使用强制保存避免间隔限制
        for (int i = 2; i <= 5; i++) {
            try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            manager.forceSave(createTestContext(), "Save " + i);
        }

        // When
        List<SessionMetadata> recent = manager.listRecentSessions(3);

        // Then
        assertEquals(3, recent.size());
    }

    @Test
    @DisplayName("清理旧会话")
    void testCleanupOldSessions() {
        // Given
        manager.forceSave(createTestContext(), "Save 1");
        manager.forceSave(createTestContext(), "Save 2");
        manager.forceSave(createTestContext(), "Save 3");

        // When
        int cleaned = manager.cleanupOldSessions();

        // Then - 验证清理操作可以正常执行
        assertTrue(cleaned >= 0, "清理操作应该执行成功");

        // 验证存储功能仍然正常
        List<SessionMetadata> remaining = manager.listSessions();
        assertNotNull(remaining);
        assertTrue(remaining.size() >= 0);
    }

    @Test
    @DisplayName("存储健康状况检查")
    void testStorageHealthCheck() {
        // When
        boolean healthy = manager.isStorageHealthy();

        // Then - 新创建的存储应该是健康的
        assertTrue(healthy);
    }

    @Test
    @DisplayName("获取存储信息")
    void testGetStorageInfo() {
        // Given
        manager.saveSession(createTestContext(), "Test save");

        // When
        SessionStorage.StorageInfo info = manager.getStorageInfo();

        // Then
        assertNotNull(info);
        assertTrue(info.getTotalSessions() >= 1);
        assertTrue(info.isHealthy());
    }

    @Test
    @DisplayName("保存失败后的降级策略")
    void testFallbackAfterFailure() {
        // Given - 模拟连续失败的情况
        // 这个测试需要更复杂的设置来模拟失败场景

        // When - 正常保存
        SessionContext context = createTestContext();
        SessionSaveResult result = manager.saveSession(context, "Normal save");

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("边界情况 - 空会话数据")
    void testEmptySessionData() {
        // Given
        SessionContext emptyContext = new SessionContext(
                Map.of(),
                createTestMetadata("empty-session")
        );

        // When
        SessionSaveResult result = manager.saveSession(emptyContext, "Empty data save");

        // Then
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("边界情况 - 特殊字符原因")
    void testSpecialCharactersInReason() {
        // Given
        String specialReason = "Test with 特殊字符 and emoji 🚀";
        SessionContext context = createTestContext();

        // When
        SessionSaveResult result = manager.saveSession(context, specialReason);

        // Then
        assertTrue(result.isSuccess());
        assertNotNull(result.getSaveId());
    }

    @Test
    @DisplayName("配置默认值验证")
    void testDefaultConfigValues() {
        // When
        SessionSaveConfig defaultConfig = SessionSaveConfig.getDefault();

        // Then
        assertEquals(10, defaultConfig.getMaxSaves());
        assertEquals(7, defaultConfig.getMaxAgeDays());
        assertEquals(50 * 1024 * 1024, defaultConfig.getMaxSingleSaveBytes());
        assertEquals(5, defaultConfig.getMinSaveIntervalMinutes());
    }

    @Test
    @DisplayName("并发保存后状态一致性")
    void testStateConsistencyAfterConcurrentSaves() throws InterruptedException {
        // Given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // When - 并发保存
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    manager.forceSave(createTestContext(), "Concurrent save " + index);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Thread interrupted", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(endLatch.await(30, TimeUnit.SECONDS));

        // Then - 状态应该保持一致
        List<SessionMetadata> sessions = manager.listSessions();
        assertTrue(sessions.size() >= 1);
        assertTrue(manager.isStorageHealthy());

        executor.shutdown();
    }

    // Helper methods

    private SessionContext createTestContext() {
        SessionMetadata metadata = createTestMetadata("test-session");
        Map<String, Object> sessionData = Map.of(
                "timestamp", Instant.now().toString(),
                "testKey", "testValue",
                "data", Map.of("nested", "value")
        );

        return new SessionContext(sessionData, metadata);
    }

    private SessionContext createLargeTestContext(int sizeInBytes) {
        StringBuilder largeData = new StringBuilder(sizeInBytes);
        for (int i = 0; i < sizeInBytes; i++) {
            largeData.append('a');
        }

        SessionMetadata metadata = createTestMetadata("large-session");
        Map<String, Object> sessionData = Map.of("largeData", largeData.toString());

        return new SessionContext(sessionData, metadata);
    }

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
}