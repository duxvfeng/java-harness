package com.chachamaru.harness.mode;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 学习结果持久化
 * 保存和加载用户反馈历史数据
 */
public class LearningPersistence {

    private static final String DEFAULT_DATA_DIR = ".claude/mode-learning/";
    private static final String DATA_FILE = "user-feedback.dat";
    private final String dataDirectory;

    /**
     * 创建持久化管理器
     */
    public LearningPersistence() {
        this(DEFAULT_DATA_DIR);
    }

    /**
     * 创建持久化管理器
     * @param dataDirectory 数据目录
     */
    public LearningPersistence(String dataDirectory) {
        this.dataDirectory = dataDirectory;
        ensureDirectoryExists();
    }

    /**
     * 保存学习数据
     * @param history 用户反馈历史
     */
    public void saveLearningData(UserFeedbackHistory history) {
        saveLearningData(history, "latest");
    }

    /**
     * 保存学习数据（带版本）
     * @param history 用户反馈历史
     * @param version 版本号
     */
    public void saveLearningData(UserFeedbackHistory history, String version) {
        try {
            String fileName = version.equals("latest") ? DATA_FILE : "user-feedback-" + version + ".dat";
            Path filePath = Paths.get(dataDirectory, fileName);

            try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(filePath.toFile())))
            ) {
                oos.writeObject(history);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法保存学习数据", e);
        }
    }

    /**
     * 加载学习数据
     * @return 用户反馈历史
     */
    public UserFeedbackHistory loadLearningData() {
        return loadVersion("latest");
    }

    /**
     * 加载指定版本的学习数据
     * @param version 版本号
     * @return 用户反馈历史
     */
    public UserFeedbackHistory loadVersion(String version) {
        try {
            String fileName = version.equals("latest") ? DATA_FILE : "user-feedback-" + version + ".dat";
            Path filePath = Paths.get(dataDirectory, fileName);

            if (!Files.exists(filePath)) {
                return new UserFeedbackHistory();
            }

            try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(filePath.toFile())))
            ) {
                return (UserFeedbackHistory) ois.readObject();
            }
        } catch (IOException | ClassNotFoundException e) {
            return new UserFeedbackHistory();
        }
    }

    /**
     * 检查版本是否存在
     * @param version 版本号
     * @return 是否存在
     */
    public boolean versionExists(String version) {
        try {
            String fileName = version.equals("latest") ? DATA_FILE : "user-feedback-" + version + ".dat";
            Path filePath = Paths.get(dataDirectory, fileName);
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取所有可用版本
     * @return 版本列表
     */
    public List<String> getAvailableVersions() {
        try {
            List<String> versions = new ArrayList<>();

            if (Files.exists(Paths.get(dataDirectory))) {
                try (var stream = Files.list(Paths.get(dataDirectory))) {
                    stream.forEach(path -> {
                        String fileName = path.getFileName().toString();
                        if (fileName.startsWith("user-feedback-") && fileName.endsWith(".dat")) {
                            String version = fileName
                                .substring("user-feedback-".length())
                                .replace(".dat", "");
                            versions.add(version);
                        } else if (fileName.equals(DATA_FILE)) {
                            versions.add("latest");
                        }
                    });
                }
            }

            return versions;
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * 确保数据目录存在
     */
    private void ensureDirectoryExists() {
        try {
            Path dirPath = Paths.get(dataDirectory);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("无法创建数据目录: " + dataDirectory, e);
        }
    }
}