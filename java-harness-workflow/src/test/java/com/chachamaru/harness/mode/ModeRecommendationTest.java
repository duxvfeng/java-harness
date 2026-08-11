package com.chachamaru.harness.mode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * ModeRecommendation 数据类的单元测试
 * 验证模式推荐数据类的正确性、JSON 序列化和业务逻辑
 */
@DisplayName("ModeRecommendation 数据类测试")
class ModeRecommendationTest {

    private final ObjectMapper objectMapper = createObjectMapper();

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 配置忽略未知属性，避免 record 方法被识别为属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Test
    @DisplayName("应该能够创建有效的模式推荐对象")
    void shouldCreateValidModeRecommendation() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.85,
            "2-3个独立任务，中等复杂度",
            List.of(ExecutionMode.SOLO, ExecutionMode.BREEZING)
        );

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.PARALLEL, recommendation.recommendedMode());
        assertEquals(0.85, recommendation.confidence());
        assertEquals("2-3个独立任务，中等复杂度", recommendation.reason());
        assertEquals(2, recommendation.alternativeModes().size());
    }

    @Test
    @DisplayName("置信度应该在合理范围内")
    void confidenceShouldBeInValidRange() {
        // 高置信度
        ModeRecommendation highConfidence = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.95,
            "单个简单任务",
            List.of()
        );
        assertEquals(0.95, highConfidence.confidence());
        assertTrue(highConfidence.confidence() >= 0.8);

        // 中等置信度
        ModeRecommendation mediumConfidence = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.65,
            "中等复杂度任务",
            List.of(ExecutionMode.SOLO)
        );
        assertEquals(0.65, mediumConfidence.confidence());
        assertTrue(mediumConfidence.confidence() >= 0.6 && mediumConfidence.confidence() < 0.8);

        // 低置信度
        ModeRecommendation lowConfidence = new ModeRecommendation(
            ExecutionMode.BREEZING,
            0.55,
            "复杂任务，需人工确认",
            List.of(ExecutionMode.PARALLEL, ExecutionMode.SOLO)
        );
        assertEquals(0.55, lowConfidence.confidence());
        assertTrue(lowConfidence.confidence() < 0.6);
    }

    @Test
    @DisplayName("应该能够序列化为 JSON")
    void shouldBeSerializableToJson() throws JsonProcessingException {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.75,
            "推荐并行模式",
            List.of(ExecutionMode.SOLO, ExecutionMode.BREEZING)
        );

        String json = objectMapper.writeValueAsString(recommendation);

        assertNotNull(json);
        assertTrue(json.contains("\"recommendedMode\":\"PARALLEL\""));
        assertTrue(json.contains("\"confidence\":0.75"));
        assertTrue(json.contains("推荐并行模式"));
        assertTrue(json.contains("\"alternativeModes\""));
    }

    @Test
    @DisplayName("应该能够从 JSON 反序列化")
    void shouldBeDeserializableFromJson() throws JsonProcessingException {
        String json = "{\"recommendedMode\":\"BREEZING\",\"confidence\":0.85,\"reason\":\"复杂任务组\",\"alternativeModes\":[\"PARALLEL\",\"SOLO\"]}";

        ModeRecommendation recommendation = objectMapper.readValue(json, ModeRecommendation.class);

        assertNotNull(recommendation);
        assertEquals(ExecutionMode.BREEZING, recommendation.recommendedMode());
        assertEquals(0.85, recommendation.confidence());
        assertEquals("复杂任务组", recommendation.reason());
        assertEquals(2, recommendation.alternativeModes().size());
    }

    @Test
    @DisplayName("JSON 序列化和反序列化应该保持数据一致性")
    void jsonSerializationShouldMaintainDataConsistency() throws JsonProcessingException {
        ModeRecommendation original = new ModeRecommendation(
            ExecutionMode.BREEZING,
            0.90,
            "大规模复杂任务，推荐团队协作",
            List.of(ExecutionMode.PARALLEL, ExecutionMode.SOLO)
        );

        String json = objectMapper.writeValueAsString(original);
        ModeRecommendation deserialized = objectMapper.readValue(json, ModeRecommendation.class);

        assertEquals(original.recommendedMode(), deserialized.recommendedMode());
        assertEquals(original.confidence(), deserialized.confidence(), 0.001);
        assertEquals(original.reason(), deserialized.reason());
        assertEquals(original.alternativeModes(), deserialized.alternativeModes());
    }

    @Test
    @DisplayName("应该支持空备选模式列表")
    void shouldSupportEmptyAlternativeModes() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.SOLO,
            1.0,
            "明确需要单独执行的任务",
            List.of()
        );

        assertNotNull(recommendation.alternativeModes());
        assertTrue(recommendation.alternativeModes().isEmpty());
        assertEquals(1.0, recommendation.confidence());
    }

    @Test
    @DisplayName("应该支持多个备选模式")
    void shouldSupportMultipleAlternativeModes() {
        List<ExecutionMode> alternatives = List.of(
            ExecutionMode.SOLO,
            ExecutionMode.PARALLEL,
            ExecutionMode.BREEZING
        );

        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.70,
            "多种模式都可能适用",
            alternatives
        );

        assertEquals(3, recommendation.alternativeModes().size());
        assertTrue(recommendation.alternativeModes().contains(ExecutionMode.SOLO));
        assertTrue(recommendation.alternativeModes().contains(ExecutionMode.PARALLEL));
        assertTrue(recommendation.alternativeModes().contains(ExecutionMode.BREEZING));
    }

    @Test
    @DisplayName("推荐理由不应该为空")
    void reasonShouldNotBeEmpty() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.9,
            "单个任务，直接执行最快",
            List.of()
        );

        assertNotNull(recommendation.reason());
        assertFalse(recommendation.reason().isEmpty());
        assertTrue(recommendation.reason().length() > 5);
    }

    @Test
    @DisplayName("相等的对象应该有相同的哈希码")
    void equalObjectsShouldHaveSameHashCode() {
        List<ExecutionMode> alternatives = List.of(ExecutionMode.SOLO);
        ModeRecommendation recommendation1 = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.75,
            "推荐理由",
            alternatives
        );
        ModeRecommendation recommendation2 = new ModeRecommendation(
            ExecutionMode.PARALLEL,
            0.75,
            "推荐理由",
            alternatives
        );

        assertEquals(recommendation1, recommendation2);
        assertEquals(recommendation1.hashCode(), recommendation2.hashCode());
    }

    @Test
    @DisplayName("toString 方法应该返回有意义的字符串")
    void toStringShouldReturnMeaningfulString() {
        ModeRecommendation recommendation = new ModeRecommendation(
            ExecutionMode.BREEZING,
            0.85,
            "团队协作模式",
            List.of(ExecutionMode.PARALLEL)
        );

        String toString = recommendation.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("ModeRecommendation"));
        assertTrue(toString.contains("BREEZING"));
        assertTrue(toString.contains("0.85"));
    }

    @Test
    @DisplayName("应该判断高置信度推荐")
    void shouldIdentifyHighConfidenceRecommendation() {
        ModeRecommendation highConfidence = new ModeRecommendation(
            ExecutionMode.SOLO,
            0.95,
            "高置信度推荐",
            List.of()
        );

        assertTrue(highConfidence.confidence() > 0.8);
        assertTrue(highConfidence.confidence() <= 1.0);
    }

    @Test
    @DisplayName("应该判断低置信度推荐")
    void shouldIdentifyLowConfidenceRecommendation() {
        ModeRecommendation lowConfidence = new ModeRecommendation(
            ExecutionMode.BREEZING,
            0.55,
            "低置信度推荐",
            List.of(ExecutionMode.PARALLEL, ExecutionMode.SOLO)
        );

        assertTrue(lowConfidence.confidence() < 0.6);
        assertTrue(lowConfidence.confidence() >= 0.0);
    }
}