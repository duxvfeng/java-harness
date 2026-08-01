package com.chachamaru.harness.foundation.state;

import java.nio.file.Path;
import java.util.Optional;

/**
 * 状态持久化引擎接口
 * 提供状态序列化、保存和恢复功能
 *
 * @param <T> 状态类型
 */
public interface StatePersistenceEngine<T> {

    /**
     * 保存状态到指定路径
     *
     * @param state 要保存的状态对象
     * @param path 目标文件路径
     * @throws PersistenceException 保存失败时抛出
     */
    void save(T state, Path path) throws PersistenceException;

    /**
     * 从指定路径加载状态
     *
     * @param path 源文件路径
     * @param type 状态类型
     * @return 加载的状态对象，如果文件不存在返回空
     * @throws PersistenceException 加载失败时抛出
     */
    Optional<T> load(Path path, Class<T> type) throws PersistenceException;

    /**
     * 检查指定路径的状态文件是否存在
     *
     * @param path 文件路径
     * @return 如果文件存在返回 true
     */
    boolean exists(Path path);

    /**
     * 删除指定路径的状态文件
     *
     * @param path 文件路径
     * @throws PersistenceException 删除失败时抛出
     */
    void delete(Path path) throws PersistenceException;

    /**
     * 获取支持的格式名称
     *
     * @return 格式名称（如 "json", "yaml"）
     */
    String getFormat();

    /**
     * 创建状态文件目录（如果不存在）
     *
     * @param path 文件路径
     * @throws PersistenceException 创建失败时抛出
     */
    default void createDirectoryIfNeeded(Path path) throws PersistenceException {
        Path parent = path.getParent();
        if (parent != null && !parent.toFile().exists()) {
            try {
                java.nio.file.Files.createDirectories(parent);
            } catch (java.io.IOException e) {
                throw new PersistenceException("Failed to create directory: " + parent, e);
            }
        }
    }
}
