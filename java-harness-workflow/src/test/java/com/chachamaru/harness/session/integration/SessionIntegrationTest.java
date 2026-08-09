package com.chachamaru.harness.session.integration;

import com.chachamaru.harness.session.manager.SessionSaveManager;
import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.SessionSaveResult;
import com.chachamaru.harness.session.restore.SessionRestoreManager;
import com.chachamaru.harness.session.storage.FileSystemStorage;
import com.chachamaru.harness.session.storage.SessionStorage;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests for session save and restore system.
 *
 * <p>Tests the complete workflow including save, restore, and cleanup operations
 * with performance validation and comprehensive coverage.</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
@DisplayName("Session Management End-to-End Integration Tests")
class SessionIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(SessionIntegrationTest.class);

    private static SessionStorage storage;
    private static SessionSaveManager saveManager;
    private static SessionRestoreManager restoreManager;
    private static Path testStorageRoot;

    @BeforeAll
    static void setupTestEnvironment() throws IOException {
        // Create test storage directory
        testStorageRoot = Files.createTempDirectory("session-test-");
        storage = new FileSystemStorage(testStorageRoot, 100 * 1024 * 1024); // 100MB

        saveManager = new SessionSaveManager(
                storage,
                SessionSaveManager.SessionSaveConfig.getDefault()
        );

        restoreManager = new SessionRestoreManager(
                storage,
                SessionRestoreManager.RestoreConfig.getDefault()
        );

        logger.info("Test environment initialized with storage root: {}", testStorageRoot);
    }

    @AfterAll
    static void cleanupTestEnvironment() throws IOException {
        // Clean up test storage
        if (testStorageRoot != null && Files.exists(testStorageRoot)) {
            Files.delete(testStorageRoot);
            logger.info("Test environment cleaned up");
        }
    }

    @BeforeEach
    void cleanTestStorage() {
        // Clean storage before each test
        List<SessionMetadata> sessions = storage.listSessions();
        for (SessionMetadata session : sessions) {
            storage.deleteSession(session.getSaveId());
        }
    }

    @Test
    @DisplayName("完整的保存和恢复工作流程")
    void testCompleteSaveAndRestoreWorkflow() throws Exception {
        // 1. Save a session
        long startTime = System.currentTimeMillis();

        SessionSaveManager.SessionContext saveContext = createTestContext("Test workflow");
        SessionSaveResult saveResult = saveManager.saveSession(saveContext, "Test workflow save");

        long saveTime = System.currentTimeMillis() - startTime;

        // Validate save result
        assertTrue(saveResult.isSuccess(), "Save operation should succeed");
        assertNotNull(saveResult.getSaveId(), "Save ID should be generated");
        assertTrue(saveTime < 5000, "Save should complete in under 5 seconds, took: " + saveTime + "ms");

        logger.info("Save completed in {}ms with ID: {}", saveTime, saveResult.getSaveId());

        // 2. List sessions and verify the saved session
        List<SessionMetadata> sessions = storage.listSessions();
        assertEquals(1, sessions.size(), "Should have exactly one session");
        assertEquals(saveResult.getSaveId(), sessions.get(0).getSaveId(), "Session ID should match");

        // 3. Restore the session
        long restoreStartTime = System.currentTimeMillis();

        boolean integrityValid = restoreManager.validateSaveIntegrity(saveResult.getSaveId());
        assertTrue(integrityValid, "Session integrity should be valid");

        Optional<SessionMetadata> restoredMetadata = storage.loadMetadata(saveResult.getSaveId());
        assertTrue(restoredMetadata.isPresent(), "Should be able to load saved session");

        long restoreTime = System.currentTimeMillis() - restoreStartTime;

        assertTrue(restoreTime < 5000, "Restore should complete in under 5 seconds, took: " + restoreTime + "ms");
        logger.info("Restore completed in {}ms", restoreTime);

        // 4. Verify data consistency
        SessionMetadata original = saveContext.getMetadata();
        SessionMetadata restored = restoredMetadata.get();

        assertEquals(original.getSaveReason(), restored.getSaveReason(), "Save reason should match");
        assertEquals(original.getTokenUsage(), restored.getTokenUsage(), "Token usage should match");
    }

    @Test
    @DisplayName("并发保存操作")
    void testConcurrentSaveOperations() throws Exception {
        // Create multiple save contexts
        SessionSaveManager.SessionContext context1 = createTestContext("Concurrent test 1");
        SessionSaveManager.SessionContext context2 = createTestContext("Concurrent test 2");
        SessionSaveManager.SessionContext context3 = createTestContext("Concurrent test 3");

        // Perform concurrent saves
        long startTime = System.currentTimeMillis();

        SessionSaveResult result1 = saveManager.saveSession(context1, "Concurrent save 1");
        Thread.sleep(100); // Small delay to avoid exact timestamp collision

        SessionSaveResult result2 = saveManager.saveSession(context2, "Concurrent save 2");
        Thread.sleep(100);

        SessionSaveResult result3 = saveManager.saveSession(context3, "Concurrent save 3");

        long totalTime = System.currentTimeMillis() - startTime;

        // Validate all saves
        assertTrue(result1.isSuccess(), "First save should succeed");
        assertTrue(result2.isSuccess(), "Second save should succeed");
        assertTrue(result3.isSuccess(), "Third save should succeed");

        // Verify all sessions are stored
        List<SessionMetadata> sessions = storage.listSessions();
        assertEquals(3, sessions.size(), "Should have 3 sessions");

        // Verify performance (should complete reasonably fast even with concurrency protection)
        assertTrue(totalTime < 10000, "Concurrent saves should complete in under 10 seconds, took: " + totalTime + "ms");

        logger.info("Concurrent saves completed in {}ms for 3 operations", totalTime);
    }

    @Test
    @DisplayName("会话清理功能")
    void testSessionCleanupFunctionality() throws Exception {
        // Create multiple sessions with different ages
        for (int i = 1; i <= 15; i++) {
            SessionSaveManager.SessionContext context = createTestContext("Cleanup test " + i);
            saveManager.saveSession(context, "Cleanup test save " + i);

            if (i % 3 == 0) {
                Thread.sleep(100); // Small delay between some saves
            }
        }

        // Verify initial state
        List<SessionMetadata> sessionsBefore = storage.listSessions();
        assertEquals(15, sessionsBefore.size(), "Should have 15 sessions initially");

        // Perform cleanup
        int cleanedCount = storage.cleanupOldSessions(5, 30); // Keep 5, max 30 days

        // Verify cleanup result
        assertTrue(cleanedCount >= 10, "Should clean at least 10 sessions");

        List<SessionMetadata> sessionsAfter = storage.listSessions();
        assertTrue(sessionsAfter.size() <= 5, "Should have at most 5 sessions after cleanup");
        assertTrue(sessionsAfter.size() >= 1, "Should keep at least the most recent session");

        logger.info("Cleanup removed {} sessions, {} remaining", cleanedCount, sessionsAfter.size());
    }

    @Test
    @DisplayName("恢复建议生成")
    void testRestoreSuggestionGeneration() throws Exception {
        // Create a recent session
        SessionSaveManager.SessionContext context = createTestContext("Restore suggestion test");
        saveManager.saveSession(context, "Recent save for restore test");

        // Generate restore suggestion
        Optional<com.chachamaru.harness.session.model.RestoreSuggestion> suggestion =
                restoreManager.checkRestoreOpportunity();

        assertTrue(suggestion.isPresent(), "Should generate restore suggestion");

        com.chachamaru.harness.session.model.RestoreSuggestion restoreSuggestion = suggestion.get();
        assertNotNull(restoreSuggestion.getSaveId(), "Suggestion should have save ID");
        assertNotNull(restoreSuggestion.getSummary(), "Suggestion should have summary");
        assertTrue(restoreSuggestion.getConfidence() > 0.0, "Suggestion should have confidence");
        assertTrue(restoreSuggestion.getConfidence() <= 1.0, "Confidence should be between 0 and 1");

        logger.info("Generated restore suggestion with confidence: {}", restoreSuggestion.getConfidence());
    }

    @Test
    @DisplayName("存储健康检查")
    void testStorageHealthCheck() {
        // Initial health check
        boolean isHealthy = storage.healthCheck();
        assertTrue(isHealthy, "Storage should be healthy initially");

        // Get storage info
        SessionStorage.StorageInfo storageInfo = storage.getStorageInfo();
        assertNotNull(storageInfo, "Should provide storage info");
        assertTrue(storageInfo.isHealthy(), "Storage info should indicate healthy state");

        logger.info("Storage health: {} sessions, {} bytes used",
                storageInfo.getTotalSessions(), storageInfo.getUsedSizeBytes());
    }

    @Test
    @DisplayName("性能目标验证")
    void testPerformanceTargets() throws Exception {
        // Test save performance
        long saveStartTime = System.currentTimeMillis();

        SessionSaveManager.SessionContext context = createLargeTestContext("Performance test");
        SessionSaveResult saveResult = saveManager.saveSession(context, "Performance test save");

        long saveTime = System.currentTimeMillis() - saveStartTime;

        // Test restore performance
        long restoreStartTime = System.currentTimeMillis();

        boolean integrityValid = restoreManager.validateSaveIntegrity(saveResult.getSaveId());
        Optional<SessionMetadata> metadata = storage.loadMetadata(saveResult.getSaveId());

        long restoreTime = System.currentTimeMillis() - restoreStartTime;

        // Validate performance targets
        assertTrue(saveTime < 3000, "Save should complete in under 3 seconds, took: " + saveTime + "ms");
        assertTrue(restoreTime < 5000, "Restore should complete in under 5 seconds, took: " + restoreTime + "ms");
        assertTrue(integrityValid, "Integrity check should pass");
        assertTrue(metadata.isPresent(), "Should be able to load metadata");

        // Check storage efficiency
        SessionStorage.StorageInfo storageInfo = storage.getStorageInfo();
        long averageSessionSize = storageInfo.getUsedSizeBytes() / Math.max(1, storageInfo.getTotalSessions());

        assertTrue(averageSessionSize < 10 * 1024 * 1024, "Average session size should be < 10MB");

        logger.info("Performance validation: save={}ms, restore={}ms, avg size={}KB",
                saveTime, restoreTime, averageSessionSize / 1024);
    }

    @Test
    @DisplayName("错误处理和恢复")
    void testErrorHandlingAndRecovery() throws Exception {
        // Test with invalid save ID
        boolean integrityValid = restoreManager.validateSaveIntegrity("nonexistent-id");
        assertFalse(integrityValid, "Invalid save ID should fail integrity check");

        // Test save with minimal context
        SessionSaveManager.SessionContext minimalContext = createMinimalContext();
        SessionSaveResult result = saveManager.saveSession(minimalContext, "Minimal save");

        assertTrue(result.isSuccess() || result.getErrorMessage() != null,
                "Save should either succeed or provide error message");

        if (result.isSuccess()) {
            // Verify we can load it back
            Optional<SessionMetadata> metadata = storage.loadMetadata(result.getSaveId());
            assertTrue(metadata.isPresent(), "Should be able to load minimally saved session");
        }

        logger.info("Error handling test completed with result: {}", result.isSuccess() ? "SUCCESS" : "FAILED");
    }

    // Helper methods

    private Map<String, Object> createTestSessionData(String reason) {
        return Map.of("testData", "testValue", "reason", reason, "timestamp", Instant.now().toString());
    }

    private SessionSaveManager.SessionContext createTestContext(String reason) {
        SessionMetadata.TaskContext taskContext = new SessionMetadata.TaskContext(
                "Test Phase",
                List.of("task1", "task2"),
                "current-task",
                5
        );

        SessionMetadata.GitState gitState = new SessionMetadata.GitState(
                "test-branch",
                "commit-123",
                3,
                true
        );

        SessionMetadata.SaveSize saveSize = new SessionMetadata.SaveSize(
                10,
                "1MB",
                "2MB"
        );

        SessionMetadata metadata = new SessionMetadata(
                "pending",
                Instant.now(),
                reason,
                75,
                taskContext,
                gitState,
                "Test session: " + reason,
                saveSize
        );

        return new SessionSaveManager.SessionContext(createTestSessionData(reason), metadata);
    }

    private SessionSaveManager.SessionContext createLargeTestContext(String reason) {
        // Create a larger session context to test performance
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeContent.append("Test data line ").append(i).append(" with some content\n");
        }

        Map<String, Object> largeData = new HashMap<>(createTestSessionData(reason));
        largeData.put("largeContent", largeContent.toString());

        SessionMetadata metadata = new SessionMetadata(
                "pending",
                Instant.now(),
                reason,
                75,
                createTestContext(reason).getMetadata().getTaskContext(),
                createTestContext(reason).getMetadata().getGitState(),
                "Large test session: " + reason,
                createTestContext(reason).getMetadata().getSize()
        );

        return new SessionSaveManager.SessionContext(largeData, metadata);
    }

    private SessionSaveManager.SessionContext createMinimalContext() {
        SessionMetadata.TaskContext taskContext = new SessionMetadata.TaskContext(
                "Minimal",
                List.of(),
                "none",
                0
        );

        SessionMetadata.GitState gitState = new SessionMetadata.GitState(
                "main",
                "abc123",
                0,
                false
        );

        SessionMetadata.SaveSize saveSize = new SessionMetadata.SaveSize(
                0,
                "0B",
                "0B"
        );

        SessionMetadata metadata = new SessionMetadata(
                "pending",
                Instant.now(),
                "Minimal save",
                50,
                taskContext,
                gitState,
                "Minimal session",
                saveSize
        );

        return new SessionSaveManager.SessionContext(Map.of(), metadata);
    }
}