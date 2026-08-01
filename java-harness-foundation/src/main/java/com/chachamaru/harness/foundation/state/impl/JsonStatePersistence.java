package com.chachamaru.harness.foundation.state.impl;

import com.chachamaru.harness.foundation.state.PersistenceException;
import com.chachamaru.harness.foundation.state.StatePersistenceEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * JSON 格式的状态持久化引擎实现
 * 使用 Jackson 进行 JSON 序列化和反序列化
 *
 * @param <T> 状态类型
 */
public class JsonStatePersistence<T> implements StatePersistenceEngine<T> {

    private static final Logger logger = LoggerFactory.getLogger(JsonStatePersistence.class);
    private final ObjectMapper objectMapper;

    public JsonStatePersistence() {
        this.objectMapper = new ObjectMapper();
        // 配置 ObjectMapper
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    public JsonStatePersistence(ObjectMapper objectMapper) {
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

            logger.debug("Successfully saved state to JSON: {}", path);

        } catch (IOException e) {
            throw new PersistenceException(
                    "Failed to save state to JSON file: " + path,
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

            logger.debug("Successfully loaded state from JSON: {}", path);
            return Optional.of(state);

        } catch (IOException e) {
            throw new PersistenceException(
                    "Failed to load state from JSON file: " + path,
                    PersistenceException.ErrorType.FILE_READ_ERROR,
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
        return "json";
    }

    /**
     * 获取 ObjectMapper 实例（用于高级配置）
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
