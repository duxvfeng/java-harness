package com.chachamaru.harness.foundation.state;

import com.chachamaru.harness.foundation.state.impl.JsonStatePersistence;
import com.chachamaru.harness.foundation.state.impl.YamlStatePersistence;

import java.nio.file.Path;

/**
 * 状态持久化引擎工厂
 * 提供便捷的方法创建不同格式的持久化引擎
 */
public final class StatePersistenceFactory {

    private StatePersistenceFactory() {
        // 工具类，禁止实例化
    }

    /**
     * 创建 JSON 格式的持久化引擎
     *
     * @param <T> 状态类型
     * @return JSON 持久化引擎实例
     */
    public static <T> StatePersistenceEngine<T> createJsonPersistence() {
        return new JsonStatePersistence<>();
    }

    /**
     * 创建 YAML 格式的持久化引擎
     *
     * @param <T> 状态类型
     * @return YAML 持久化引擎实例
     */
    public static <T> StatePersistenceEngine<T> createYamlPersistence() {
        return new YamlStatePersistence<>();
    }

    /**
     * 根据文件扩展名自动创建合适的持久化引擎
     *
     * @param path 文件路径
     * @param <T> 状态类型
     * @return 对应的持久化引擎实例
     * @throws IllegalArgumentException 如果扩展名不支持
     */
    @SuppressWarnings("unchecked")
    public static <T> StatePersistenceEngine<T> createFromExtension(Path path) {
        String fileName = path.getFileName().toString();
        String extension = getFileExtension(fileName);

        return switch (extension.toLowerCase()) {
            case "json" -> (StatePersistenceEngine<T>) createJsonPersistence();
            case "yaml", "yml" -> (StatePersistenceEngine<T>) createYamlPersistence();
            default -> throw new IllegalArgumentException(
                    "Unsupported file extension: " + extension +
                            ". Supported formats: json, yaml, yml"
            );
        };
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * 检查文件扩展名是否被支持
     *
     * @param path 文件路径
     * @return 如果扩展名被支持返回 true
     */
    public static boolean isSupportedFormat(Path path) {
        String fileName = path.getFileName().toString();
        String extension = getFileExtension(fileName);
        return extension.matches("(?i)(json|yaml|yml)");
    }
}
