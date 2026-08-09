package com.chachamaru.harness.session.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionConfigLoader 测试
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
class SessionConfigLoaderTest {

    @Test
    void testDefaultConfigLoading() {
        // Given
        SessionConfigLoader loader = new SessionConfigLoader(null);

        // When
        var autoSaveConfig = loader.getAutoSaveConfig();
        var restoreConfig = loader.getRestoreConfig();
        var storageConfig = loader.getStorageConfig();

        // Then
        assertNotNull(autoSaveConfig);
        assertTrue(autoSaveConfig.isEnable());
        assertEquals(2, autoSaveConfig.getThresholds().length);
        assertEquals(10, autoSaveConfig.getMaxSaves());

        assertNotNull(restoreConfig);
        assertTrue(restoreConfig.isAutoPrompt());

        assertNotNull(storageConfig);
        assertTrue(storageConfig.getMaxTotalSize() > 0);
    }

    @Test
    void testEnvironmentVariableOverride() {
        // Given
        System.setProperty("HARNESS_SESSION_MAX_SAVES", "20");

        // When - 重新创建加载器以应用环境变量
        SessionConfigLoader loader = new SessionConfigLoader(null);
        var config = loader.getAutoSaveConfig();

        // Then - 验证环境变量覆盖生效
        assertEquals(20, config.getMaxSaves());

        // Cleanup
        System.clearProperty("HARNESS_SESSION_MAX_SAVES");
    }

    @Test
    void testBooleanProperties() {
        // Given
        SessionConfigLoader loader = new SessionConfigLoader(null);

        // When
        var autoSaveConfig = loader.getAutoSaveConfig();
        var storageConfig = loader.getStorageConfig();

        // Then
        assertTrue(autoSaveConfig.isCompression());
        assertTrue(storageConfig.isAsyncSave());
    }

    @Test
    void testSizeParsing() {
        // Given
        SessionConfigLoader loader = new SessionConfigLoader(null);

        // When
        var storageConfig = loader.getStorageConfig();

        // Then
        assertTrue(storageConfig.getMaxTotalSize() > 0);
        assertTrue(storageConfig.getMaxSingleSave() > 0);
    }
}