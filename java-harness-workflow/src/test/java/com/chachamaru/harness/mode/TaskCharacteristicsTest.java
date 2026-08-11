package com.chachamaru.harness.mode;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * TaskCharacteristics 数据类的单元测试
 * 验证任务特征数据类的正确性、不可变性和 JSON 序列化
 */
@DisplayName("TaskCharacteristics 数据类测试")
class TaskCharacteristicsTest {

    private final ObjectMapper objectMapper = createObjectMapper();

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 配置忽略未知属性，避免 record 方法被识别为属性
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    @Test
    @DisplayName("应该能够创建有效的任务特征对象")
    void shouldCreateValidTaskCharacteristics() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );

        assertNotNull(characteristics);
        assertEquals(1, characteristics.taskCount());
        assertEquals(ComplexityLevel.SIMPLE, characteristics.complexity());
        assertEquals(DependencyType.INDEPENDENT, characteristics.dependencies());
        assertEquals(ReviewRequirement.NONE, characteristics.reviewNeed());
    }

    @Test
    @DisplayName("应该支持所有复杂度等级")
    void shouldSupportAllComplexityLevels() {
        TaskCharacteristics simple = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );
        TaskCharacteristics moderate = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );
        TaskCharacteristics complex = new TaskCharacteristics(
            3, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );
        TaskCharacteristics veryComplex = new TaskCharacteristics(
            4, ComplexityLevel.VERY_COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        assertEquals(ComplexityLevel.SIMPLE, simple.complexity());
        assertEquals(ComplexityLevel.MODERATE, moderate.complexity());
        assertEquals(ComplexityLevel.COMPLEX, complex.complexity());
        assertEquals(ComplexityLevel.VERY_COMPLEX, veryComplex.complexity());
    }

    @Test
    @DisplayName("应该支持所有依赖类型")
    void shouldSupportAllDependencyTypes() {
        TaskCharacteristics independent = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );
        TaskCharacteristics sequential = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );
        TaskCharacteristics mixed = new TaskCharacteristics(
            3, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        assertEquals(DependencyType.INDEPENDENT, independent.dependencies());
        assertEquals(DependencyType.SEQUENTIAL, sequential.dependencies());
        assertEquals(DependencyType.MIXED, mixed.dependencies());
    }

    @Test
    @DisplayName("应该支持所有审查需求")
    void shouldSupportAllReviewRequirements() {
        TaskCharacteristics none = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );
        TaskCharacteristics optional = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );
        TaskCharacteristics required = new TaskCharacteristics(
            3, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        assertEquals(ReviewRequirement.NONE, none.reviewNeed());
        assertEquals(ReviewRequirement.OPTIONAL, optional.reviewNeed());
        assertEquals(ReviewRequirement.REQUIRED, required.reviewNeed());
    }

    @Test
    @DisplayName("应该能够序列化为 JSON")
    void shouldBeSerializableToJson() throws JsonProcessingException {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );

        String json = objectMapper.writeValueAsString(characteristics);

        assertNotNull(json);
        assertTrue(json.contains("\"taskCount\":2"));
        assertTrue(json.contains("\"complexity\":\"MODERATE\""));
        assertTrue(json.contains("\"dependencies\":\"SEQUENTIAL\""));
        assertTrue(json.contains("\"reviewNeed\":\"OPTIONAL\""));
    }

    @Test
    @DisplayName("应该能够从 JSON 反序列化")
    void shouldBeDeserializableFromJson() throws JsonProcessingException {
        String json = "{\"taskCount\":3,\"complexity\":\"COMPLEX\",\"dependencies\":\"MIXED\",\"reviewNeed\":\"REQUIRED\"}";

        TaskCharacteristics characteristics = objectMapper.readValue(json, TaskCharacteristics.class);

        assertNotNull(characteristics);
        assertEquals(3, characteristics.taskCount());
        assertEquals(ComplexityLevel.COMPLEX, characteristics.complexity());
        assertEquals(DependencyType.MIXED, characteristics.dependencies());
        assertEquals(ReviewRequirement.REQUIRED, characteristics.reviewNeed());
    }

    @Test
    @DisplayName("JSON 序列化和反序列化应该保持数据一致性")
    void jsonSerializationShouldMaintainDataConsistency() throws JsonProcessingException {
        TaskCharacteristics original = new TaskCharacteristics(
            4, ComplexityLevel.VERY_COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );

        String json = objectMapper.writeValueAsString(original);
        TaskCharacteristics deserialized = objectMapper.readValue(json, TaskCharacteristics.class);

        assertEquals(original.taskCount(), deserialized.taskCount());
        assertEquals(original.complexity(), deserialized.complexity());
        assertEquals(original.dependencies(), deserialized.dependencies());
        assertEquals(original.reviewNeed(), deserialized.reviewNeed());
    }

    @Test
    @DisplayName("应该处理合理的任务数量范围")
    void shouldHandleReasonableTaskCountRange() {
        // 单个任务
        TaskCharacteristics single = new TaskCharacteristics(
            1, ComplexityLevel.SIMPLE, DependencyType.INDEPENDENT, ReviewRequirement.NONE
        );
        assertEquals(1, single.taskCount());

        // 小任务组
        TaskCharacteristics small = new TaskCharacteristics(
            3, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );
        assertEquals(3, small.taskCount());

        // 大任务组
        TaskCharacteristics large = new TaskCharacteristics(
            10, ComplexityLevel.COMPLEX, DependencyType.MIXED, ReviewRequirement.REQUIRED
        );
        assertEquals(10, large.taskCount());
    }

    @Test
    @DisplayName("相等的对象应该有相同的哈希码")
    void equalObjectsShouldHaveSameHashCode() {
        TaskCharacteristics characteristics1 = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );
        TaskCharacteristics characteristics2 = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );

        assertEquals(characteristics1, characteristics2);
        assertEquals(characteristics1.hashCode(), characteristics2.hashCode());
    }

    @Test
    @DisplayName("toString 方法应该返回有意义的字符串")
    void toStringShouldReturnMeaningfulString() {
        TaskCharacteristics characteristics = new TaskCharacteristics(
            2, ComplexityLevel.MODERATE, DependencyType.SEQUENTIAL, ReviewRequirement.OPTIONAL
        );

        String toString = characteristics.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("TaskCharacteristics"));
        assertTrue(toString.contains("taskCount=2"));
    }
}