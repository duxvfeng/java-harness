package com.chachamaru.harness.session.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 会话配置加载器
 *
 * <p>简化的配置管理，支持环境变量、默认值和基本配置文件。</p>
 *
 * @author Java Harness Team
 * @since 2026-08-09
 */
public class SessionConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(SessionConfigLoader.class);

    private final Properties properties;
    private final Path configPath;

    public SessionConfigLoader(Path configPath) {
        this.configPath = configPath;
        this.properties = new Properties();
        loadConfig();
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        // 1. 加载默认配置
        loadDefaultConfig();

        // 2. 尝试加载配置文件
        if (configPath != null && Files.exists(configPath)) {
            loadConfigFromFile();
        }

        // 3. 环境变量覆盖
        loadEnvironmentVariables();

        logger.info("Session config loaded: {}", properties);
    }

    /**
     * 获取自动保存配置
     */
    public AutoSaveConfig getAutoSaveConfig() {
        return new AutoSaveConfig(
                getBooleanProperty("session.auto_save.enable", true),
                getIntArrayProperty("session.auto_save.thresholds", new int[]{80, 90}),
                getIntProperty("session.auto_save.max_saves", 10),
                getBooleanProperty("session.auto_save.compression", true),
                getIntProperty("session.auto_save.save_interval_minutes", 5)
        );
    }

    /**
     * 获取恢复配置
     */
    public RestoreConfig getRestoreConfig() {
        return new RestoreConfig(
                getBooleanProperty("session.restore.auto_prompt", true),
                getIntProperty("session.restore.max_history_age_days", 7)
        );
    }

    /**
     * 获取存储配置
     */
    public StorageConfig getStorageConfig() {
        return new StorageConfig(
                parseSizeProperty("session.storage.max_total_size", "500MB"),
                parseSizeProperty("session.storage.max_single_save", "50MB"),
                getBooleanProperty("session.storage.async_save", true),
                getBooleanProperty("session.storage.incremental_save", true)
        );
    }

    // Private helper methods

    private void loadDefaultConfig() {
        properties.setProperty("session.auto_save.enable", "true");
        properties.setProperty("session.auto_save.thresholds", "80,90");
        properties.setProperty("session.auto_save.max_saves", "10");
        properties.setProperty("session.auto_save.compression", "true");
        properties.setProperty("session.auto_save.save_interval_minutes", "5");

        properties.setProperty("session.restore.auto_prompt", "true");
        properties.setProperty("session.restore.max_history_age_days", "7");

        properties.setProperty("session.storage.max_total_size", "500MB");
        properties.setProperty("session.storage.max_single_save", "50MB");
        properties.setProperty("session.storage.async_save", "true");
        properties.setProperty("session.storage.incremental_save", "true");
    }

    private void loadConfigFromFile() {
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
            logger.info("Config loaded from file: {}", configPath);
        } catch (IOException e) {
            logger.warn("Failed to load config from file: {}", configPath, e);
        }
    }

    private void loadEnvironmentVariables() {
        // 环境变量覆盖
        String envAutoSave = System.getenv("HARNESS_SESSION_AUTO_SAVE");
        if (envAutoSave != null) {
            properties.setProperty("session.auto_save.enable", envAutoSave);
        }

        String envThresholds = System.getenv("HARNESS_SESSION_THRESHOLDS");
        if (envThresholds != null) {
            properties.setProperty("session.auto_save.thresholds", envThresholds);
        }

        String envMaxSaves = System.getenv("HARNESS_SESSION_MAX_SAVES");
        if (envMaxSaves != null) {
            properties.setProperty("session.auto_save.max_saves", envMaxSaves);
        }

        logger.debug("Environment variables applied");
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    private int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for {}: {}", key, value);
            return defaultValue;
        }
    }

    private int[] getIntArrayProperty(String key, int[] defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;

        try {
            String[] parts = value.split(",");
            int[] result = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Integer.parseInt(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            logger.warn("Invalid int array value for {}: {}", key, value);
            return defaultValue;
        }
    }

    private long parseSizeProperty(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        try {
            if (value.endsWith("MB")) {
                return Long.parseLong(value.substring(0, value.length() - 2)) * 1024 * 1024;
            } else if (value.endsWith("GB")) {
                return Long.parseLong(value.substring(0, value.length() - 2)) * 1024 * 1024 * 1024;
            } else {
                return Long.parseLong(value);
            }
        } catch (Exception e) {
            logger.warn("Invalid size value for {}: {}", key, value);
            return 50 * 1024 * 1024; // 默认 50MB
        }
    }

    // Configuration classes

    public static class AutoSaveConfig {
        private final boolean enable;
        private final int[] thresholds;
        private final int maxSaves;
        private final boolean compression;
        private final int saveIntervalMinutes;

        public AutoSaveConfig(boolean enable, int[] thresholds, int maxSaves,
                             boolean compression, int saveIntervalMinutes) {
            this.enable = enable;
            this.thresholds = thresholds;
            this.maxSaves = maxSaves;
            this.compression = compression;
            this.saveIntervalMinutes = saveIntervalMinutes;
        }

        public boolean isEnable() { return enable; }
        public int[] getThresholds() { return thresholds; }
        public int getMaxSaves() { return maxSaves; }
        public boolean isCompression() { return compression; }
        public int getSaveIntervalMinutes() { return saveIntervalMinutes; }
    }

    public static class RestoreConfig {
        private final boolean autoPrompt;
        private final int maxHistoryAgeDays;

        public RestoreConfig(boolean autoPrompt, int maxHistoryAgeDays) {
            this.autoPrompt = autoPrompt;
            this.maxHistoryAgeDays = maxHistoryAgeDays;
        }

        public boolean isAutoPrompt() { return autoPrompt; }
        public int getMaxHistoryAgeDays() { return maxHistoryAgeDays; }
    }

    public static class StorageConfig {
        private final long maxTotalSize;
        private final long maxSingleSave;
        private final boolean asyncSave;
        private final boolean incrementalSave;

        public StorageConfig(long maxTotalSize, long maxSingleSave,
                           boolean asyncSave, boolean incrementalSave) {
            this.maxTotalSize = maxTotalSize;
            this.maxSingleSave = maxSingleSave;
            this.asyncSave = asyncSave;
            this.incrementalSave = incrementalSave;
        }

        public long getMaxTotalSize() { return maxTotalSize; }
        public long getMaxSingleSave() { return maxSingleSave; }
        public boolean isAsyncSave() { return asyncSave; }
        public boolean isIncrementalSave() { return incrementalSave; }
    }
}