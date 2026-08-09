package com.chachamaru.harness.session.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * 会话数据模型综合测试
 *
 * <p>测试所有会话数据模型的序列化、反序列化、构造方法和边界情况。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
@DisplayName("会话数据模型测试")
class SessionModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    @DisplayName("SessionMetadata - JSON 序列化测试")
    void testSessionMetadataSerialization() throws Exception {
        // Given
        SessionMetadata.TaskContext taskContext = new SessionMetadata.TaskContext(
                "Phase 2",
                Arrays.asList("1.1", "1.2", "1.3"),
                "2.1",
                25
        );

        SessionMetadata.GitState gitState = new SessionMetadata.GitState(
                "feature/session-save",
                "a850127",
                7,
                true
        );

        SessionMetadata.SaveSize saveSize = new SessionMetadata.SaveSize(
                5,
                "2.3MB",
                "8.7MB"
        );

        SessionMetadata metadata = new SessionMetadata(
                "20260809-153045-token-85",
                Instant.parse("2026-08-09T15:30:45Z"),
                "Token usage reached 85%",
                85,
                taskContext,
                gitState,
                "Phase 1 已完成，正在执行 Phase 2 中文 README 创建，已完成 2.1-2.4 任务",
                saveSize
        );

        // When
        String json = objectMapper.writeValueAsString(metadata);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"saveId\":\"20260809-153045-token-85\""));
        assertTrue(json.contains("\"tokenUsage\":85"));
        assertTrue(json.contains("\"currentPhase\":\"Phase 2\""));
        assertTrue(json.contains("\"branch\":\"feature/session-save\""));

        // When - 反序列化
        SessionMetadata deserialized = objectMapper.readValue(json, SessionMetadata.class);

        // Then - 验证反序列化结果
        assertEquals(metadata.getSaveId(), deserialized.getSaveId());
        assertEquals(metadata.getTokenUsage(), deserialized.getTokenUsage());
        assertEquals(metadata.getTaskContext().getCurrentPhase(),
                    deserialized.getTaskContext().getCurrentPhase());
        assertEquals(metadata.getGitState().getBranch(),
                    deserialized.getGitState().getBranch());
    }

    @Test
    @DisplayName("SessionMetadata - equals 和 hashCode 测试")
    void testSessionMetadataEqualsHashCode() {
        // Given
        SessionMetadata.TaskContext taskContext1 = new SessionMetadata.TaskContext(
                "Phase 2", Arrays.asList("1.1"), "2.1", 25
        );
        SessionMetadata.GitState gitState1 = new SessionMetadata.GitState(
                "master", "abc123", 0, false
        );
        SessionMetadata.SaveSize saveSize1 = new SessionMetadata.SaveSize(
                1, "1MB", "2MB"
        );

        SessionMetadata metadata1 = new SessionMetadata(
                "save-1", Instant.now(), "test", 50,
                taskContext1, gitState1, "summary", saveSize1
        );

        SessionMetadata metadata2 = new SessionMetadata(
                "save-1", metadata1.getTimestamp(), "test", 50,
                taskContext1, gitState1, "summary", saveSize1
        );

        SessionMetadata metadata3 = new SessionMetadata(
                "save-2", Instant.now(), "test", 50,
                taskContext1, gitState1, "summary", saveSize1
        );

        // Then & When
        assertEquals(metadata1, metadata2);
        assertEquals(metadata1.hashCode(), metadata2.hashCode());
        assertNotEquals(metadata1, metadata3);
        assertNotEquals(metadata1.hashCode(), metadata3.hashCode());
    }

    @Test
    @DisplayName("SessionSummary - JSON 序列化测试")
    void testSessionSummarySerialization() throws Exception {
        // Given
        SessionSummary.AIDecision aiDecision = new SessionSummary.AIDecision(
                true,
                "复杂任务，需要恢复 Plans.md 状态和修改文件上下文",
                0.85
        );

        SessionSummary summary = new SessionSummary(
                "20260809-153045-token-85",
                "Phase 1 文档清理完成，Phase 2 README 创建中 (60%)",
                "正在编写中文 README 功能特性列表 (Task 2.2)",
                Arrays.asList(
                        "✅ 完成中文 README 项目概述 (Task 2.1)",
                        "✅ 完成架构设计章节 (Task 2.3)",
                        "🔄 正在执行功能特性列表 (Task 2.2)"
                ),
                "建议继续 Task 2.2，然后完成 Task 2.4-2.6",
                aiDecision
        );

        // When
        String json = objectMapper.writeValueAsString(summary);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"saveId\":\"20260809-153045-token-85\""));
        assertTrue(json.contains("\"needsDetailedContext\":true"));
        assertTrue(json.contains("\"confidence\":0.85"));

        // When - 反序列化
        SessionSummary deserialized = objectMapper.readValue(json, SessionSummary.class);

        // Then
        assertEquals(summary.getSaveId(), deserialized.getSaveId());
        assertEquals(summary.getQuickOverview(), deserialized.getQuickOverview());
        assertEquals(summary.getAiDecision().needsDetailedContext(),
                    deserialized.getAiDecision().needsDetailedContext());
    }

    @Test
    @DisplayName("SessionSaveResult - Builder 模式测试")
    void testSessionSaveResultBuilder() {
        // Given
        Instant now = Instant.now();

        // When - 成功结果
        SessionSaveResult success = SessionSaveResult.success("save-1", "Save completed", 2048000L);

        // Then
        assertTrue(success.isSuccess());
        assertEquals("save-1", success.getSaveId());
        assertEquals("Save completed", success.getMessage());
        assertEquals(2048000L, success.getSize());

        // When - 失败结果
        SessionSaveResult failed = SessionSaveResult.failed("Disk full");

        // Then
        assertFalse(failed.isSuccess());
        assertEquals("Disk full", failed.getErrorMessage());

        // When - 跳过结果
        SessionSaveResult skipped = SessionSaveResult.skipped("Too soon");

        // Then
        assertFalse(skipped.isSuccess());
        assertEquals("Save skipped", skipped.getMessage());
        assertEquals("Too soon", skipped.getErrorMessage());
    }

    @Test
    @DisplayName("TokenUsageInfo - 阈值判断测试")
    void testTokenUsageInfoThresholds() {
        // Given - 正常使用情况
        TokenUsageInfo normal = new TokenUsageInfo(50000, 50, 50000, "environment_variable");

        // When & Then
        assertFalse(normal.isThresholdReached(80));
        assertFalse(normal.needsImmediateSave(90, 80));
        assertTrue(normal.isDetectionSuccessful());
        assertEquals("Moderate (50%)", normal.getStatusDescription());

        // Given - 高使用情况
        TokenUsageInfo high = new TokenUsageInfo(85000, 85, 15000, "environment_variable");

        // When & Then
        assertTrue(high.isThresholdReached(80));
        assertTrue(high.isThresholdReached(85));
        assertFalse(high.needsImmediateSave(90, 80));
        assertEquals("High (85%)", high.getStatusDescription());

        // Given - 紧急情况
        TokenUsageInfo critical = new TokenUsageInfo(95000, 95, 5000, "environment_variable");

        // When & Then
        assertTrue(critical.isThresholdReached(80));
        assertTrue(critical.needsImmediateSave(90, 80));
        assertEquals("Critical (95%)", critical.getStatusDescription());

        // Given - 检测失败
        TokenUsageInfo failed = TokenUsageInfo.detectionFailed();

        // When & Then
        assertFalse(failed.isThresholdReached(80));
        assertFalse(failed.isDetectionSuccessful());
        assertFalse(failed.needsImmediateSave(90, 80));
    }

    @Test
    @DisplayName("RestoreSuggestion - 置信度和建议等级测试")
    void testRestoreSuggestionLevels() {
        // Given - 高置信度 + 最近保存
        SessionSummary.AIDecision aiDecision1 = new SessionSummary.AIDecision(true, "reason", 0.9);
        SessionSummary summary1 = new SessionSummary(
                "save-1", "overview", "current", Arrays.asList("progress"),
                "recommendation", aiDecision1
        );
        RestoreSuggestion highConfidenceRecent = new RestoreSuggestion(
                "save-1", summary1, true, "Complex task", 0.9, 2, Instant.now()
        );

        // When & Then
        assertTrue(highConfidenceRecent.isHighConfidence());
        assertTrue(highConfidenceRecent.isRecentSave());
        assertFalse(highConfidenceRecent.isOldSave());
        assertEquals(RestoreSuggestion.SuggestionLevel.STRONGLY_RECOMMENDED,
                    highConfidenceRecent.getSuggestionLevel());

        // Given - 中置信度 + 最近保存
        SessionSummary.AIDecision aiDecision2 = new SessionSummary.AIDecision(false, "reason", 0.6);
        SessionSummary summary2 = new SessionSummary(
                "save-2", "overview", "current", Arrays.asList("progress"),
                "recommendation", aiDecision2
        );
        RestoreSuggestion mediumConfidenceRecent = new RestoreSuggestion(
                "save-2", summary2, false, "Simple task", 0.6, 12, Instant.now()
        );

        // When & Then
        assertTrue(mediumConfidenceRecent.isMediumConfidence());
        assertTrue(mediumConfidenceRecent.isRecentSave());
        assertEquals(RestoreSuggestion.SuggestionLevel.RECOMMENDED,
                    mediumConfidenceRecent.getSuggestionLevel());

        // Given - 低置信度 + 较久保存
        RestoreSuggestion lowConfidenceOld = new RestoreSuggestion(
                "save-3", summary2, false, "Old task", 0.3, 200, Instant.now()
        );

        // When & Then
        assertFalse(lowConfidenceOld.isRecentSave());
        assertTrue(lowConfidenceOld.isOldSave());
        assertEquals(RestoreSuggestion.SuggestionLevel.LOW_PRIORITY,
                    lowConfidenceOld.getSuggestionLevel());
    }

    @Test
    @DisplayName("边界情况 - null 和空值处理")
    void testEdgeCasesAndNullHandling() {
        // Given - TokenUsageInfo 边界情况
        TokenUsageInfo unknown = TokenUsageInfo.unknown();

        // When & Then
        assertEquals(-1, unknown.getCurrentUsage());
        assertEquals(-1, unknown.getPercentage());
        assertFalse(unknown.isThresholdReached(50));

        // Given - 置信度边界测试
        SessionSummary.AIDecision aiDecision = new SessionSummary.AIDecision(true, "reason", 0.5);
        SessionSummary summary = new SessionSummary(
                "save", "overview", "current", Arrays.asList(), "rec", aiDecision
        );

        // When - 置信度超出范围应被限制在 [0, 1]
        RestoreSuggestion tooHigh = new RestoreSuggestion(
                "save", summary, true, "reason", 1.5, 1, Instant.now()
        );
        RestoreSuggestion tooLow = new RestoreSuggestion(
                "save", summary, true, "reason", -0.5, 1, Instant.now()
        );

        // Then
        assertEquals(1.0, tooHigh.getConfidence());
        assertEquals(0.0, tooLow.getConfidence());
    }

    @Test
    @DisplayName("toString() 方法输出格式验证")
    void testToStringOutput() {
        // Given
        TokenUsageInfo info = new TokenUsageInfo(50000, 50, 50000, "test");

        // When
        String str = info.toString();

        // Then
        assertNotNull(str);
        assertTrue(str.contains("TokenUsageInfo{"));
        assertTrue(str.contains("currentUsage=50000"));
        assertTrue(str.contains("percentage=50"));
    }

    @Test
    @DisplayName("SessionMetadata - 嵌套类 equals 和 hashCode 测试")
    void testNestedClassEqualsHashCode() {
        // Given
        SessionMetadata.TaskContext taskContext1 = new SessionMetadata.TaskContext(
                "Phase 1", Arrays.asList("1.1", "1.2"), "1.3", 10
        );
        SessionMetadata.TaskContext taskContext2 = new SessionMetadata.TaskContext(
                "Phase 1", Arrays.asList("1.1", "1.2"), "1.3", 10
        );
        SessionMetadata.TaskContext taskContext3 = new SessionMetadata.TaskContext(
                "Phase 2", Arrays.asList("2.1"), "2.2", 5
        );

        // When & Then
        assertEquals(taskContext1, taskContext2);
        assertEquals(taskContext1.hashCode(), taskContext2.hashCode());
        assertNotEquals(taskContext1, taskContext3);

        // Given
        SessionMetadata.GitState gitState1 = new SessionMetadata.GitState("master", "abc123", 0, false);
        SessionMetadata.GitState gitState2 = new SessionMetadata.GitState("master", "abc123", 0, false);
        SessionMetadata.GitState gitState3 = new SessionMetadata.GitState("feature", "def456", 5, true);

        // When & Then
        assertEquals(gitState1, gitState2);
        assertEquals(gitState1.hashCode(), gitState2.hashCode());
        assertNotEquals(gitState1, gitState3);
    }
}