package com.chachamaru.harness.foundation.state.impl;

import com.chachamaru.harness.foundation.state.PersistenceException;
import com.chachamaru.harness.foundation.state.StatePersistenceEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * YAML 格式的状态持久化引擎实现
 * 使用 Jackson YAML 模块进行安全的 YAML 序列化和反序列化
 *
 * @param <T> 状态类型
 */
public class YamlStatePersistence<T> implements StatePersistenceEngine<T> {

    private static final Logger logger = LoggerFactory.getLogger(YamlStatePersistence.class);
    private final ObjectMapper objectMapper;

    public YamlStatePersistence() {
        // 配置 YAML ObjectMapper（安全方式）
        YAMLFactory yamlFactory = YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER) // 不写入 --- 文档开始标记
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)       // 最小化引号使用
                .enable(YAMLGenerator.Feature.INDENT_ARRAYS)          // 数组缩进
                .build();

        this.objectMapper = new ObjectMapper(yamlFactory);
    }

    public YamlStatePersistence(ObjectMapper objectMapper) {
        if (objectMapper == null) {
            throw new IllegalArgumentException("ObjectMapper cannot be null");
        }
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(T state, Path path) throws PersistenceException {
        if (state == null) {
            throw new PersistenceException("Cannot save null state",
                    PersistenceException.ErrorType.VALIDATION_ERROR);
        }

        try {
            // 创建目录（如果需要）
            createDirectoryIfNeeded(path);

            // 序列化并保存
            synchronized (this) { // 线程安全
                objectMapper.writeValue(path.toFile(), state);
            }

            logger.debug("Successfully saved state to YAML: {}", path);

        } catch (IOException e) {
            throw new PersistenceException(
                    "Failed to save state to YAML file: " + path,
                    PersistenceException.ErrorType.FILE_WRITE_ERROR,
                    e
            );
        }
    }

    @Override
    public Optional<T> load(Path path, Class<T> type) throws PersistenceException {
        if (!Files.exists(path)) {
            logger.debug("State file does not exist: {}", path);
            return Optional.empty();
        }

        try {
            T state;
            synchronized (this) { // 线程安全
                state = objectMapper.readValue(path.toFile(), type);
            }

            if (state == null) {
                logger.debug("Loaded null state from YAML: {}", path);
                return Optional.empty();
            }

            logger.debug("Successfully loaded state from YAML: {}", path);
            return Optional.of(state);

        } catch (IOException e) {
            throw new PersistenceException(
                    "Failed to load state from YAML file: " + path,
                    PersistenceException.ErrorType.FILE_READ_ERROR,
                    e
            );
        } catch (Exception e) {
            throw new PersistenceException(
                    "Failed to parse YAML from file: " + path,
                    PersistenceException.ErrorType.DESERIALIZATION_ERROR,
                    e
            );
        }
    }

    @Override
    public boolean exists(Path path) {
        return Files.exists(path);
    }

    @Override
    public void delete(Path path) throws PersistenceException {
        try {
            if (exists(path)) {
                synchronized (this) { // 线程安全
                    Files.delete(path);
                }
                logger.debug("Successfully deleted state file: {}", path);
            }
        } catch (IOException e) {
            throw new PersistenceException(
                    "Failed to delete state file: " + path,
                    PersistenceException.ErrorType.FILE_WRITE_ERROR,
                    e
            );
        }
    }

    @Override
    public String getFormat() {
        return "yaml";
    }

    /**
     * 获取 ObjectMapper 实例（用于高级配置）
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
