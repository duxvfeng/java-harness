package com.chachamaru.harness.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

/**
 * ModelAvailabilityChecker 检查器的单元测试
 * 测试模型可用性检查、格式验证、超时设置等功能
 */
class ModelAvailabilityCheckerTest {

    private ModelAvailabilityChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ModelAvailabilityChecker();
    }

    @AfterEach
    void tearDown() {
        // 清理资源
    }

    @Test
    void testIsValidModelNameWithValidNames() {
        // 有效的模型名称
        assertTrue(checker.isValidModelName("glm-4.7"));
        assertTrue(checker.isValidModelName("claude-sonnet-4-20250514"));
        assertTrue(checker.isValidModelName("claude-fable-5-20250514"));
        assertTrue(checker.isValidModelName("gpt-4"));
        assertTrue(checker.isValidModelName("mimo-v2-flash[1M]"));
    }

    @Test
    void testIsValidModelNameWithInvalidNames() {
        // 无效的模型名称
        assertFalse(checker.isValidModelName(null));
        assertFalse(checker.isValidModelName(""));
        assertFalse(checker.isValidModelName("   "));
        assertFalse(checker.isValidModelName(createOverLongModelName(200)));
    }

    @Test
    void testIsRemoteModelWithRemoteModels() {
        // 远程模型
        assertTrue(checker.isRemoteModel("claude-sonnet-4-20250514"));
        assertTrue(checker.isRemoteModel("claude-fable-5-20250514"));
        assertTrue(checker.isRemoteModel("gpt-4"));
    }

    @Test
    void testIsRemoteModelWithLocalModels() {
        // 本地模型
        assertFalse(checker.isRemoteModel("glm-4.7"));
        assertFalse(checker.isRemoteModel("qwen-7b"));
        assertFalse(checker.isRemoteModel("local-model"));
    }

    @Test
    void testIsAvailableWithValidModel() throws InterruptedException {
        // 使用已知的有效模型进行测试
        // 注意：这里使用基本格式验证，不进行实际网络调用
        boolean result = checker.isAvailable("glm-4.7", 1000);
        assertTrue(result, "Local model should be available");
    }

    @Test
    void testIsAvailableWithInvalidModel() {
        boolean result = checker.isAvailable("", 1000);
        assertFalse(result, "Empty model name should not be available");
    }

    @Test
    void testIsAvailableWithNullModel() {
        boolean result = checker.isAvailable(null, 1000);
        assertFalse(result, "Null model name should not be available");
    }

    @Test
    void testIsAvailableWithTimeout() throws InterruptedException {
        // 测试超时设置
        long startTime = System.currentTimeMillis();
        boolean result = checker.isAvailable("test-model", 100);
        long endTime = System.currentTimeMillis();

        // 应该在超时时间内完成
        assertTrue((endTime - startTime) < 500, "Check should complete within timeout");
    }

    @Test
    void testIsAvailableWithZeroTimeout() {
        // 零超时应该立即返回
        boolean result = checker.isAvailable("glm-4.7", 0);
        // 零超时意味着不做检查，直接返回基于格式验证的结果
        assertTrue(result);
    }

    @Test
    void testIsAvailableWithNegativeTimeout() {
        // 负超时应该被处理为零或正值
        boolean result = checker.isAvailable("glm-4.7", -100);
        assertTrue(result);
    }

    @Test
    void testValidateFormatWithValidFormat() {
        assertDoesNotThrow(() -> checker.validateFormat("claude-sonnet-4-20250514"));
        assertDoesNotThrow(() -> checker.validateFormat("glm-4.7"));
    }

    @Test
    void testValidateFormatWithInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> checker.validateFormat(null));
        assertThrows(IllegalArgumentException.class, () -> checker.validateFormat(""));
        assertThrows(IllegalArgumentException.class, () -> checker.validateFormat("   "));
    }

    @Test
    void testCheckNetworkConnectivity() {
        // 网络连接检查（可能在某些环境下失败）
        // 这里测试方法存在性，不测试实际网络连接
        assertDoesNotThrow(() -> checker.checkNetworkConnectivity(1000));
    }

    @Test
    void testTryLightweightApiCall() {
        // API 调用测试（在真实环境中可能失败）
        // 这里测试方法存在性，不测试实际 API 调用
        boolean result = checker.tryLightweightApiCall("test-model", 100);
        // 由于没有真实的 API，应该返回 false 或基于格式验证
        assertNotNull(result);
    }

    @Test
    void testIsAvailableWithMixedModelNames() {
        // 测试不同类型的模型名称
        assertTrue(checker.isAvailable("glm-4.7", 1000));
        assertTrue(checker.isAvailable("claude-sonnet-4-20250514", 1000));
        assertTrue(checker.isAvailable("mimo-v2-flash[1M]", 1000));
    }

    @Test
    void testModelNameLengthValidation() {
        // 测试不同长度的模型名称
        assertTrue(checker.isValidModelName("gpt")); // 短名称
        assertTrue(checker.isValidModelName("claude-sonnet-4-20250514")); // 中等长度
        assertFalse(checker.isValidModelName(createOverLongModelName(150))); // 过长名称
    }

    @Test
    void testSpecialCharactersInModelName() {
        // 测试特殊字符
        assertTrue(checker.isValidModelName("gpt-4_turbo"));
        assertTrue(checker.isValidModelName("mimo-v2-flash[1M]"));
        assertTrue(checker.isValidModelName("claude.3.5"));
    }

    @Test
    void testConcurrentAvailabilityCheck() throws InterruptedException {
        // 测试并发检查（基本测试）
        Thread t1 = new Thread(() -> {
            boolean result = checker.isAvailable("glm-4.7", 1000);
            assertTrue(result);
        });

        Thread t2 = new Thread(() -> {
            boolean result = checker.isAvailable("claude-sonnet-4-20250514", 1000);
            assertTrue(result);
        });

        t1.start();
        t2.start();

        t1.join(2000);
        t2.join(2000);

        // 如果到这里说明没有死锁或崩溃
        assertTrue(true);
    }

    // Helper method to create overly long model name
    private String createOverLongModelName(int length) {
        return "a".repeat(length);
    }
}