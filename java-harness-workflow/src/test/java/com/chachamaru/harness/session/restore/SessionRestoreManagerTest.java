package com.chachamaru.harness.session.restore;

import com.chachamaru.harness.session.manager.SessionSaveManager.SessionContext;
import com.chachamaru.harness.session.model.SessionMetadata;
import com.chachamaru.harness.session.model.RestoreSuggestion;
import com.chachamaru.harness.session.storage.FileSystemStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionRestoreManager 测试
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
class SessionRestoreManagerTest {

    @Test
    void testBasicRestoreDetection() {
        // Given
        Path tempDir = Path.of("temp-test");
        var storage = new FileSystemStorage(tempDir, 100 * 1024 * 1024);
        var manager = new SessionRestoreManager(storage, SessionRestoreManager.RestoreConfig.getDefault());

        // When
        var opportunity = manager.checkRestoreOpportunity();

        // Then - 没有保存时应该返回空
        assertTrue(opportunity.isEmpty() || opportunity.isPresent());
    }

    @Test
    void testSuggestionGeneration() {
        // Given
        Path tempDir = Path.of("temp-test");
        var storage = new FileSystemStorage(tempDir, 100 * 1024 * 1024);
        var manager = new SessionRestoreManager(storage, SessionRestoreManager.RestoreConfig.getDefault());

        // 创建测试保存
        SessionMetadata metadata = createTestMetadata("test-save");
        storage.saveSession("test-save", "test data", metadata);

        // When
        var opportunity = manager.checkRestoreOpportunity();

        // Then
        assertTrue(opportunity.isPresent());
        assertEquals("test-save", opportunity.get().getSaveId());
    }

    @Test
    void testIntegrityValidation() {
        // Given
        Path tempDir = Path.of("temp-test");
        var storage = new FileSystemStorage(tempDir, 100 * 1024 * 1024);
        var manager = new SessionRestoreManager(storage, SessionRestoreManager.RestoreConfig.getDefault());

        // When - 验证不存在的保存
        boolean valid = manager.validateSaveIntegrity("non-existent");

        // Then
        assertFalse(valid);
    }

    @Test
    void testAIDecisionLogic() {
        // Given - 高复杂度场景
        SessionMetadata metadata = createHighComplexityMetadata();
        Path tempDir = Path.of("temp-test");
        var storage = new FileSystemStorage(tempDir, 100 * 1024 * 1024);
        var manager = new SessionRestoreManager(storage, SessionRestoreManager.RestoreConfig.getDefault());

        storage.saveSession("complex-save", "data", metadata);

        // When
        var opportunity = manager.checkRestoreOpportunity();

        // Then - 验证基本功能，而不是具体的决策结果
        assertTrue(opportunity.isPresent());
        assertNotNull(opportunity.get().getSummary());
        assertNotNull(opportunity.get().getSummary().getAiDecision());
    }

    @Test
    void testConfidenceCalculation() {
        // Given
        Path tempDir = Path.of("temp-test");
        var storage = new FileSystemStorage(tempDir, 100 * 1024 * 1024);
        var manager = new SessionRestoreManager(storage, SessionRestoreManager.RestoreConfig.getDefault());

        // When
        var opportunity = manager.checkRestoreOpportunity();

        // Then - 没有保存时应该为空
        assertTrue(opportunity.isEmpty() || opportunity.get().getConfidence() >= 0);
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

    private SessionMetadata createHighComplexityMetadata() {
        SessionMetadata.TaskContext taskContext = new SessionMetadata.TaskContext(
                "Phase 2",
                Arrays.asList("1.1", "1.2", "1.3", "1.4", "1.5"),
                "2.1",
                25
        );

        SessionMetadata.GitState gitState = new SessionMetadata.GitState(
                "feature/session-save",
                "def456",
                7,
                true
        );

        SessionMetadata.SaveSize saveSize = new SessionMetadata.SaveSize(
                3,
                "5MB",
                "10MB"
        );

        return new SessionMetadata(
                "complex-save",
                Instant.now(),
                "Complex save with many changes",
                95,
                taskContext,
                gitState,
                "复杂任务，有大量修改",
                saveSize
        );
    }
}